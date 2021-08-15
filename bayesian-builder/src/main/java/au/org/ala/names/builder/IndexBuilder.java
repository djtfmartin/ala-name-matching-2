package au.org.ala.names.builder;

import au.org.ala.bayesian.Observable;
import au.org.ala.bayesian.*;
import au.org.ala.names.lucene.LuceneLoadStore;
import au.org.ala.util.CleanedScientificName;
import au.org.ala.util.Counter;
import au.org.ala.util.SimpleClassifier;
import au.org.ala.vocab.BayesianTerm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import org.apache.commons.cli.*;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Build a name matching index.
 * <p>
 * The process of building an index is:
 * </p>
 * <ol>
 *     <li>Load all the source data into a loading index, so that we have a complete picture of the data.</li>
 *     <li>Annotate the loading index with inferred information - alternative names, parents, higher taxonomy, etc.</li>
 *     <li>Compute parameters for each calculation required by an inference class. The inference class and parameters are generated by the bayseian network compiler.</li>
 *     <li>Produce an output lucene index optimised for subsequent searching</li>
 * </ol>
 */
public class IndexBuilder<C extends Classification<C>, I extends Inferencer<C>, F extends NetworkFactory<C, I, F>> implements Annotator {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexBuilder.class);

    /** The date format for timestamping backups */
    public static final SimpleDateFormat BACKUP_TAG = new SimpleDateFormat("yyyyMMddHHmmss");
    /** The data format for timestamping metadata */
    public static final SimpleDateFormat TIMESTAMP = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

    protected IndexBuilderConfiguration config;
    /** The network that this is a builder for */
    @Getter
    protected Network network;
    /** The factory for creating builder objects */
    @Getter
    protected F factory;
    /** The builder to use in processing */
    protected Builder builder;
    /** The weight observable (required) */
    protected Observable weight;
    /** The parent observable (required) */
    protected Observable parent;
    /** The accepted name observable */
    protected Optional<Observable> accepted;
    /** The taxon ID observable (required) */
    protected Observable taxonId;
    /** The scientific name observable */
    protected Observable scientificName;
    /** The alternartive name observable */
    protected Optional<Observable> altScientificName;
    /** The scientific name authorship */
    protected Optional<Observable> scientificNameAuthorship;
    /** The complete, propertly formatted name/author pair */
    protected Optional<Observable> nameComplete;
    /** The set of terms to copy from an accepted taxon to a synonym */
    protected Set<Observable> synonymCopy;
    /** The set of terms that need to be stored */
    protected Set<Observable> stored;
    /** The load-store. Used to store semi-structured information before building the complete index. */
    @Getter
    protected LoadStore loadStore;
    /** The expanded store. Used to store expanded information */
    @Getter
    protected LoadStore expandedStore;
    /** The parameterised store. Used to store parameterised information */
    @Getter
    protected LoadStore parameterisedStore;
    /** The analyser. Used to add extra information to loaded classifiers. Accessed via the annotation interface */
    protected Analyser<C> analyser;

    /**
     * Construct with a configuration.
     *
     * @param config The configuration
     *
     * @throws StoreException if unable to create the load-store
     *
     */
    public IndexBuilder(IndexBuilderConfiguration config) throws BuilderException, InferenceException, StoreException, IOException {
        this.config = config;
        this.network = Network.read(this.config.getNetwork());
        this.factory = config.createFactory(this);
        this.builder = config.createBuilder(this, this.factory);
        this.loadStore = config.createLoadStore(this);
        this.analyser = this.factory.createAnalyser();
        this.weight = this.network.findObservable(BayesianTerm.weight, true).orElseThrow(() -> new BuilderException("Require observable " + BayesianTerm.weight + ":true property"));
        this.parent = this.network.findObservable(DwcTerm.parentNameUsageID, true).orElseThrow(() -> new BuilderException("Require observable " + DwcTerm.parentNameUsageID + ":true property"));
        this.accepted = this.network.findObservable(DwcTerm.acceptedNameUsageID, true);
        this.taxonId = this.network.findObservable(DwcTerm.taxonID, true).orElseThrow(() -> new BuilderException("Require observable " + DwcTerm.taxonID + ":true property"));
        this.scientificName = this.network.findObservable(DwcTerm.scientificName, true).orElseThrow(() -> new BuilderException("Require observable " + DwcTerm.scientificName + ":true property"));
        this.scientificNameAuthorship = this.network.findObservable(DwcTerm.scientificNameAuthorship, true);
        this.nameComplete = this.network.findObservable(BayesianTerm.fullName, true);
        this.altScientificName = this.network.findObservable(BayesianTerm.altName, true);
        this.synonymCopy = new HashSet<>(this.network.findObservables(BayesianTerm.copy, true));
        this.stored = new HashSet<>(this.network.getObservables()); // The defined observables in the network
        this.stored.add(this.weight);
        this.stored.add(this.parent);
        this.accepted.ifPresent(o -> this.stored.add(o));
        this.stored.add(this.taxonId);
        this.stored.add(this.scientificName);
        this.scientificNameAuthorship.ifPresent(o -> this.stored.add(o));
        this.nameComplete.ifPresent(o -> this.stored.add(o));
        this.altScientificName.ifPresent(o -> this.stored.add(o));
        this.stored.addAll(this.synonymCopy);
    }

    /**
     * Load a source into the index builder.
     *
     * @param source The source to load
     *
     * @throws BuilderException if something foes wrong
     * @throws InferenceException if there is something wrong with annotating the load
     * @throws StoreException if the store fails during loading
     */
    public void load(Source source) throws BuilderException, InferenceException, StoreException {
        source.load(this.loadStore, this.stored);
        this.loadStore.commit();
    }


    /**
     * Build the index once the data has been loaded.
     * <p>
     * This builds all the information so that the index can be constructed.
     * It does not output the final index; see {@link #buildIndex(File)} for that.
     * </p>
     *
     * @throws BuilderException if unable to store information in the index
     * @throws InferenceException if unable to build the inference parameters
     * @throws StoreException if unable to store intermediate documents
     */
    public void build() throws BuilderException, InferenceException, StoreException {
        this.expandTree();
        this.expandSynonyms();
        this.buildParameters();
    }


    /**
     * Close down the index builder.
     * <p>
     *     This ensures that all underlying stores are gracefully shut down.
     * </p>
     *
     * @throws StoreException if unable to close
     */
    public void close() throws StoreException {
        this.loadStore.close();
        if (this.expandedStore != null)
            this.expandedStore.close();
        if (this.parameterisedStore != null)
            this.parameterisedStore.close();
    }

    /**
     * Traverse the tree, building a model of the taxonomic tree in terms of parent/child relationships
     * and fill out information about the
     *
     * @throws BuilderException if unable to traverse the tree
     * @throws StoreException if unable to store data property
     */
    public void expandTree() throws BuilderException, InferenceException, StoreException {
        LOGGER.info("Expanding accepted concept tree");
        this.expandedStore = this.config.createLoadStore(Annotator.NULL);
        int index = 1;
        Observation isRoot = this.loadStore.getAnnotationObservation(BayesianTerm.isRoot);
        List<Classifier> top = this.loadStore.getAllClassifiers(DwcTerm.Taxon, isRoot); // Keep across commits
        Counter topCounter = new Counter("Processed {0} top level documents, {1}s, {3,number,0.0}%", LOGGER, 100, top.size());
        Counter counter = new Counter("Processed {0} accepted taxa, {2,number,0.0}/s, last {4}", LOGGER, this.config.getLogInterval(), -1);
        topCounter.start();
        counter.start();
        for (Classifier classifier: top) {
            topCounter.increment(classifier.getIdentifier());
            index = this.expandTree(classifier, new LinkedList<Classifier>(), index, counter);
            this.expandedStore.commit();
        }
        counter.stop();
        topCounter.stop();
    }

    public int expandTree(Classifier classifier, Deque<Classifier> parents, int index, Counter counter) throws InferenceException, StoreException {
        int left = index;
        // Perform all derivations
        this.builder.expand(classifier, parents);
        String id = classifier.get(this.taxonId);
        Set<String> allNames = new HashSet<>();
        Set<String> altNames = new HashSet<>();

        String name = classifier.get(this.scientificName);
        CleanedScientificName n = new CleanedScientificName(name);
        allNames.add(n.getName());
        allNames.add(n.getBasic());
        allNames.add(n.getNormalised());
        // Ensure that the normalised version of the name is used.
        if (!name.equals(n.getNormalised())) {
            classifier.replace(this.scientificName, n.getNormalised());
        }

        Optional<String> authorship = this.scientificNameAuthorship.map(sna -> classifier.get(sna));
        String nameComplete = this.nameComplete.map(nc -> (String) classifier.get(nc)).orElseGet(() -> (name + " " + authorship.orElse("")).trim());
        CleanedScientificName nc = new CleanedScientificName(nameComplete);
        allNames.add(nc.getName());
        allNames.add(nc.getBasic());
        allNames.add(nc.getNormalised());

        classifier.setNames(allNames);

        if (this.altScientificName.isPresent()) {
            if (this.accepted.isPresent()) {
                Iterable<Classifier> synonyms = this.loadStore.getAll(DwcTerm.Taxon, new Observation(true, this.accepted.get(), id));
                for (Classifier synonym : synonyms) {
                    String syn = synonym.get(this.scientificName);
                    CleanedScientificName s = new CleanedScientificName(syn);
                    altNames.add(s.getName());
                    altNames.add(s.getBasic());
                    altNames.add(s.getNormalised());
                }
            }
            for (String nm : altNames) {
                classifier.add(this.altScientificName.get(), nm);
            }
        }
        this.builder.infer(classifier);
        if (this.parent != null) {
            parents.push(classifier);
            Iterable<Classifier> children = this.loadStore.getAll(DwcTerm.Taxon, new Observation(true, this.parent, id));
            for (Classifier child : children) {
                index = this.expandTree(child, parents, index, counter);
            }
            parents.pop();
        }
        classifier.setIndex(left, index);
        this.expandedStore.store(classifier);
        counter.increment(classifier.getIdentifier());
        return index + 1;
    }

    /**
     * Expand all synonyms, including the portions of the parent taxonomy that are accurate.
     *
     * @throws StoreException if unable to manage the updates
     */
    public void expandSynonyms() throws InferenceException, StoreException {
        LOGGER.info("Expanding synonyms");
        Counter counter = new Counter("Processed {0} synonyms, {2,number,0.0}/s, last {4}", LOGGER, this.config.getLogInterval(), -1);
        Observation isSynonym = this.loadStore.getAnnotationObservation(BayesianTerm.isSynonym);
        Iterable<Classifier> synonyms = this.loadStore.getAll(DwcTerm.Taxon, isSynonym);
        counter.start();
        for (Classifier classifier: synonyms) {
            String id = classifier.get(this.taxonId);
            Optional<String> aid = this.accepted.map(acc -> classifier.get(acc));
            Optional<Classifier> acpt = Optional.empty();
            if (!aid.isPresent()) {
                LOGGER.error("Synonym document " + id + " does not have an accepted taxon id");
            } else {
                acpt = Optional.ofNullable(this.expandedStore.get(DwcTerm.Taxon, this.taxonId, aid.get()));
                if (!acpt.isPresent()) {
                    LOGGER.error("Taxon " + id + " with accepted id " + aid + " does not have a matching accepted taxon");
                }
            }
            this.expandSynonym(classifier, acpt);
            this.expandedStore.store(classifier);
           counter.increment(classifier.getIdentifier());
        }
        counter.stop();
        this.expandedStore.commit();
    }

    /**
     * Expand information about a synonym.
     * <p>
     * Unless otherwise specified, the synonym inherits information
     * such as the rank and higher taxonomy from the parent.
     * </p>
     *
     * @param classifier The synonym classifier
     * @param accepted The accepted taxon document
     *
     * @throws StoreException if unable to write the updated document
     */
    public void expandSynonym(Classifier classifier, Optional<Classifier> accepted) throws InferenceException, StoreException {
        Set<String> allNames = new HashSet<>();
        String name = classifier.get(this.scientificName);
        CleanedScientificName n = new CleanedScientificName(name);
        allNames.add(n.getName());
        allNames.add(n.getBasic());
        allNames.add(n.getNormalised());
        // Ensure that the normalised version of the name is used.
        if (!name.equals(n.getNormalised())) {
             classifier.replace(this.scientificName, n.getNormalised());
        }
        if (this.altScientificName.isPresent()) {
            for (String nm : allNames) {
                classifier.add(this.altScientificName.get(), nm);
            }
        }
        if (accepted.isPresent()){
            Classifier acc = accepted.get();
            for (Observable obs: this.synonymCopy) {
                if (!classifier.has(obs))
                    classifier.addAll(obs, acc);
            }
        }
        this.builder.infer(classifier);
    }

    /**
     * Build the parameters for each taxon.
     * <p>
     * This is a slow, expensive piece of processing and is done in parallel.
     * </p>
     *
     * @throws StoreException
     * @throws InferenceException
     */
    public void buildParameters() throws StoreException, InferenceException {
        LOGGER.info("Building parameter sets");
        this.parameterisedStore = config.createLoadStore(Annotator.NULL);
        final Counter counter = new Counter("Processed {0} parameter sets, {2,number,0.0}/s, last {4}", LOGGER, this.config.getLogInterval(), -1);
        final ParameterAnalyser analyser = this.expandedStore.getParameterAnalyser(this.network, this.weight, this.config.getDefaultWeight());
        final BlockingQueue<Classifier> workQueue = new LinkedBlockingQueue<Classifier>(this.config.getThreads() * 4);
        final Classifier poisonPill = new SimpleClassifier();
        Iterable<Classifier> taxa = this.expandedStore.getAll(DwcTerm.Taxon);
        List<ParameterConsumer> consumers = IntStream.range(0, this.config.getThreads()).mapToObj(i -> new ParameterConsumer(analyser, workQueue, counter, poisonPill)).collect(Collectors.toList());
        List<Thread> consumerThreads = consumers.stream().map(Thread::new).collect(Collectors.toList());
        consumerThreads.forEach(t -> t.start());
        counter.start();
        try {
            for (Classifier classifier : taxa) {
                workQueue.put(classifier);
            }
            for (ParameterConsumer consumer : consumers)
                workQueue.put(poisonPill);
            LOGGER.info("Waiting for consumers to terminate");
            for (Thread thread: consumerThreads)
                thread.join();
            for (ParameterConsumer consumer: consumers)
                if (consumer.getError() != null) {
                    throw new InferenceException("Consumer error : " + consumer.getError());
                }
        } catch (InterruptedException ex) {
            throw new InferenceException("Interupted during parameter building", ex);
        }
        counter.stop();
        this.loadStore.commit();
    }

    protected void buildParameters(Classifier classifier, ParameterAnalyser analyser) throws StoreException, InferenceException {
        String id = classifier.get(this.taxonId);
        String signature = this.builder.buildSignature(classifier);
        classifier.setSignature(signature);
        Parameters parameters = this.builder.calculate(analyser, classifier);
        classifier.storeParameters(parameters);
        this.parameterisedStore.store(classifier);

    }

    public void buildIndex(File output) throws StoreException, InferenceException {
        LOGGER.info("Building matchng index at " + output);
        if (output.exists()) {
            String backup = output.getName() + "-" + ((SimpleDateFormat) BACKUP_TAG.clone()).format(new Date());
            File dest = new File(output.getParent(), backup);
            output.renameTo(dest);
            LOGGER.info("Renamed existing " + output + " to " + dest);
         }
        if (!output.mkdirs())
            throw new IllegalArgumentException("Unable to create " + output);
        LoadStore index = new LuceneLoadStore(this, output, false, false);
        // Copy taxa across
        Iterable<Classifier> taxa = this.parameterisedStore.getAll(DwcTerm.Taxon);
        for (Classifier classifier : taxa) {
            index.store(classifier);
        }
        // Insert metadata document
        Classifier metadata = this.createMetadata();
        index.store(metadata, BayesianTerm.Metadata);

        index.commit();
        index.close();
    }

    /**
     * Build a document describing the index
     *
     * @return The document metadata
     *
     * @throws StoreException if unable to construct the document
     */
    public Classifier createMetadata() throws StoreException {
        Classifier metadata = this.loadStore.newClassifier();
        Observable creator = new Observable(DcTerm.creator);
        Observable created = new Observable(DcTerm.created);
        Observable description = new Observable(DcTerm.description);
        Observable identifier = new Observable(DcTerm.identifier);
        Observable title = new Observable(DcTerm.title);
        Observable source = new Observable(DcTerm.source);
        Observable builderClass = new Observable(BayesianTerm.builderClass);
        Observable version = new Observable(DcTerm.hasVersion);
        Date timestamp = new Date();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        metadata.add(identifier, UUID.randomUUID().toString());
        metadata.add(title, this.network.getId());
        metadata.add(description, this.network.getDescription());
        metadata.add(creator, System.getProperty("user.name", "unknown"));
        metadata.add(created, TIMESTAMP.format(timestamp));
        metadata.add(builderClass, this.config.getBuilderClass().getName());
        metadata.add(version, this.getClass().getPackage().getSpecificationVersion());
        try {
            metadata.add(source, mapper.writeValueAsString(this.network));
        } catch (JsonProcessingException ex) {
            throw new StoreException("Unable to write network condfiguration", ex);
        }
        return metadata;
    }

    /**
     * Annotate a classifier with additional information.
     * <p>
     * Any {@link Analyser} is applied to the classification.
     * If the document does not have a parent or an accepted taxon
     * then annotate it as {@link BayesianTerm#isRoot}.
     * If the document does not have a parent but does have an accepted taxon,
     * then annotate it as {@link BayesianTerm#isSynonym}
     * </p>
     *
     * @param classifier The classifier
     *
     * @throws InferenceException If any analysis fails
     * @throws StoreException If unable to create an annotation for some reason.
     */
    @Override
    public void annotate(Classifier classifier) throws InferenceException, StoreException {
        Term type = classifier.getType();
        String p = classifier.get(this.parent);
        String a = this.accepted.isPresent() ? classifier.get(this.accepted.get()) : null;

        if (type != DwcTerm.Taxon)
            return;
        if (( p == null || p.isEmpty()) && (a == null || a.isEmpty()))
            classifier.annotate(BayesianTerm.isRoot);
        if ((p == null || p.isEmpty()) && (a != null && !a.isEmpty()))
            classifier.annotate(BayesianTerm.isSynonym);
    }

    /**
     * Run the index builder.
     * <p>
     * This is a generic, one-size-fits all builder.
     * You are better off using the generated builders that are associated with a specific
     * network configuration.
     * </p>
     *
     * @param args Command line arguments
     *
     * @throws Exception if unable to complete
     */
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        Option configOption = Option.builder("c").longOpt("config").desc("Specify a configuration file").hasArg().argName("URL").type(URL.class).build();
        Option workOption = Option.builder("w").longOpt("work").desc("Working directory").hasArg().argName("DIR").type(File.class).build();
        Option networkOption = Option.builder("n").longOpt("network").desc("Network description").hasArg().argName("URL").type(URL.class).build();
        Option builderClassOption = Option.builder("b").longOpt("builder").desc("Network description").hasArg().argName("CLASS").type(Class.class).build();
        Option outputOption = Option.builder("o").longOpt("output").desc("Output index directory").hasArg().argName("DIR").type(File.class).build();
        Option helpOption = Option.builder("h").longOpt("help").desc("Print help").build();
        options.addOption(configOption);
        options.addOption(workOption);
        options.addOption(networkOption);
        options.addOption(builderClassOption);
        options.addOption(outputOption);
        options.addOption(helpOption);
        IndexBuilderConfiguration config;
        File output;

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(helpOption.getOpt())) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("java -jar name-matching-builder [OPTIONS] [SOURCES]", options);
            System.exit(0);
        }
        if (cmd.hasOption(configOption.getOpt())) {
            config = IndexBuilderConfiguration.read(((URL) cmd.getParsedOptionValue(configOption.getOpt())));
        } else {
            config = new IndexBuilderConfiguration();
        }
        if (cmd.hasOption(workOption.getOpt())) {
            config.setWork((File) cmd.getParsedOptionValue(workOption.getOpt()));
        }
        if (cmd.hasOption(networkOption.getOpt())) {
            config.setNetwork((URL) cmd.getParsedOptionValue(networkOption.getOpt()));
        }
        if (cmd.hasOption(builderClassOption.getOpt())) {
            config.setBuilderClass((Class) cmd.getParsedOptionValue(builderClassOption.getOpt()));
        }
        if (cmd.hasOption(outputOption.getOpt())) {
            output = (File) cmd.getParsedOptionValue(outputOption.getOpt());
        } else {
            output = new File(config.getWork(), "output");
        }
        IndexBuilder builder = new IndexBuilder(config);
        for (String input: cmd.getArgs()) {
            File in = new File(input);
            Source source = null;
            if (!in.exists()) {
                System.err.println("Input file " + in + " does not exist");
                System.exit(1);
            }
            if (input.endsWith(".csv")) {
                source = new CSVSource(DwcTerm.Taxon, new FileReader(in), builder.getFactory(), builder.getNetwork().getObservables());
            } else {
                System.err.println("Unable to determine the type of " + in);
            }
            builder.load(source);
        }
        builder.expandTree();
        builder.expandSynonyms();
        builder.buildParameters();
        builder.buildIndex(output);
        builder.close();
    }

    /**
     * Processor for parameter estimates
     */
    private class ParameterConsumer implements Runnable {
        ParameterAnalyser analyser;
        BlockingQueue<Classifier> queue;
        Counter counter;
        Classifier poisonPill;
        @Getter
        String error;

        /**
         * Build a parameter consumer for a work queue
         *
         * @param analyser The parameter analyser
         * @param queue The work queue
         * @param counter The statistics counter
         * @param poisonPill The "poison pill" used to terminate this worked
         */
        public ParameterConsumer(ParameterAnalyser analyser, BlockingQueue<Classifier> queue, Counter counter, Classifier poisonPill) {
            this.analyser = analyser;
            this.queue = queue;
            this.counter = counter;
            this.poisonPill = poisonPill;
            this.error = null;
        }

        /**
         * Pick up a document from the queue and process parameters
         * <p>
         * Terminate when a null document is received
         * </p>
         */
        @Override
        public void run() {
            this.error = null;
            IndexBuilder.this.LOGGER.info("Starting processor " + Thread.currentThread().getName());
            try {
                while (true) {
                    Classifier classifier = this.queue.take();
                    if (classifier == this.poisonPill) {
                        IndexBuilder.this.LOGGER.info("Terminating processor " + Thread.currentThread().getName());
                        return;
                    }
                    IndexBuilder.this.buildParameters(classifier, analyser);
                    this.counter.increment(classifier.getIdentifier());
                }
            } catch (Exception ex) {
                IndexBuilder.LOGGER.error(ex.getMessage(), ex);
                this.error = ex.getMessage();
            }
        }
    }
}
