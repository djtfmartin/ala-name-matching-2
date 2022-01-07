package au.org.ala.names.builder;

import au.org.ala.bayesian.Observable;
import au.org.ala.bayesian.*;
import au.org.ala.names.lucene.LuceneLoadStore;
import au.org.ala.util.Counter;
import au.org.ala.util.SimpleClassifier;
import au.org.ala.vocab.BayesianTerm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import org.apache.commons.cli.*;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
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
    /** The number of documents to commit in a batch */
    public static final int COMMIT_BATCH = 5000;

    protected IndexBuilderConfiguration config;
    /** The network that this is a builder for */
    @Getter
    protected Network network;
    /** The factory for creating builder objects */
    @Getter
    protected F factory;
    /** The builder to use in processing */
    protected Builder builder;
    /** The class term to use for the concept */
    protected Term conceptTerm;
    /** The weight observable (required) */
    protected Observable weight;
    /** The parent observable */
    protected Optional<Observable> parent;
    /** The accepted name observable */
    protected Optional<Observable> accepted;
    /** The category identifier observable (required) */
    protected Observable identifier;
    /** The concept name observable */
    protected Observable name;
    /** The alternative concept name observable */
    protected Optional<Observable> altName;
    /** The synonym concept name observable */
    protected Optional<Observable> synonymName;
    /** The concept name additional terms */
    protected Optional<Observable> additionalName;
    /** The complete, propertly formatted concept name */
    protected Optional<Observable> fullName;
    /** The set of terms to copy from an accepted taxon to a synonym */
    protected Set<Observable> copy;
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
    /** The wight analyser, used to compute the weight of a classifier */
    @Getter
    protected WeightAnalyser weightAnalyser;
    /** The identifiers used. Non-unique identifiers are disambiguated */
    protected Set<Object> identifiers;

    /**
     * Construct with a configuration.
     *
     * @param config The configuration
     *
     * @throws BayesianException if unable to create the load-store
     * @throws IOException if unable to read the information
     *
     */
    public IndexBuilder(IndexBuilderConfiguration config) throws BayesianException, IOException {
        this.config = config;
        this.network = Network.read(this.config.getNetwork());
        this.factory = config.createFactory(this);
        this.builder = config.createBuilder(this, this.factory);
        this.loadStore = config.createLoadStore(this);
        this.analyser = this.factory.createAnalyser();
        this.weightAnalyser = config.createWeightAnalyser(this.network);
        if (this.network.getConcept() == null)
            throw new BuilderException("Network requires concept term");
        this.conceptTerm = TermFactory.instance().findTerm(this.network.getConcept().toASCIIString());
        this.weight = this.network.findObservable(BayesianTerm.weight, true).orElseThrow(() -> new BuilderException("Require observable " + BayesianTerm.weight + ":true property"));
        this.parent = this.network.findObservable(BayesianTerm.parent, true);
        this.accepted = this.network.findObservable(BayesianTerm.accepted, true);
        this.identifier = this.network.findObservable(BayesianTerm.identifier, true).orElseThrow(() -> new BuilderException("Require observable " + BayesianTerm.identifier + ":true property"));
        this.name = this.network.findObservable(BayesianTerm.name, true).orElseThrow(() -> new BuilderException("Require observable " + BayesianTerm.name + ":true property"));
        this.additionalName = this.network.findObservable(BayesianTerm.additionalName, true);
        this.fullName = this.network.findObservable(BayesianTerm.fullName, true);
        this.altName = this.network.findObservable(BayesianTerm.altName, true);
        this.synonymName = this.network.findObservable(BayesianTerm.synonymName, true);
        this.copy = new HashSet<>(this.network.findObservables(BayesianTerm.copy, true));
        this.stored = new HashSet<>(this.network.getObservables()); // The defined observables in the network
        this.stored.add(this.weight);
        this.parent.ifPresent(o -> this.stored.add(o));
        this.accepted.ifPresent(o -> this.stored.add(o));
        this.stored.add(this.identifier);
        this.stored.add(this.name);
        this.additionalName.ifPresent(o -> this.stored.add(o));
        this.fullName.ifPresent(o -> this.stored.add(o));
        this.altName.ifPresent(o -> this.stored.add(o));
        this.synonymName.ifPresent(o -> this.stored.add(o));
        this.stored.addAll(this.copy);
        this.identifiers = new HashSet<>();
    }

    /**
     * Load a source into the index builder.
     *
     * @param source The source to load
     *
     * @throws BayesianException if something goes wrong during loading
     */
    public void load(Source source) throws BayesianException {
        this.registerJmx(source.getCounter(), "source");
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
     * @throws BayesianException if unable to store information in the index or build inference parameters
     */
    public void build() throws BayesianException {
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
     * @throws BayesianException if unable to traverse the tree, compute derivations or store data property
     */
    public void expandTree() throws BayesianException {
        LOGGER.info("Expanding accepted concept tree");
        this.expandedStore = this.config.createLoadStore(Annotator.NULL);
        int index = 1;
        int oldIndex = index;
        Observation isRoot = this.loadStore.getAnnotationObservation(BayesianTerm.isRoot);
        List<Classifier> top = this.loadStore.getAllClassifiers(this.conceptTerm, isRoot); // Keep across commits
        Counter topCounter = new Counter("Processed {0} top level documents, {1}s, {3,number,0.0}%", LOGGER, 100, top.size());
        Counter counter = new Counter("Processed {0} accepted taxa, {2,number,0.0}/s, last {4}", LOGGER, this.config.getLogInterval(), -1);
        this.registerJmx(topCounter, "top");
        this.registerJmx(counter, "accepted");
        topCounter.start();
        counter.start();
        for (Classifier classifier: top) {
            topCounter.increment(classifier.getIdentifier());
            index = this.expandTree(classifier, new LinkedList<Classifier>(), index, counter);
            if (index - oldIndex > COMMIT_BATCH) {
                this.expandedStore.commit();
                oldIndex = index;
            }
        }
        this.expandedStore.commit();
        counter.stop();
        topCounter.stop();
    }

    /**
     * Expand a single classifier in the tree.
     *
     * @param classifier The classifier
     * @param parents A queue of parents, closest parent first
     * @param index The index value
     * @param counter The statistics counter
     *
     * @return The next index value to use
     *
     * @throws BayesianException if unable to computer derived values or store the result
     */
    public int expandTree(Classifier classifier, Deque<Classifier> parents, int index, Counter counter) throws BayesianException {
        int left = index;
        String id = classifier.get(this.identifier);
        // Perform all derivations
        this.builder.expand(classifier, parents);
        final Set<String> names = new LinkedHashSet<>(); // Require order
        Set<String> altNames = new LinkedHashSet<>();
        Set<String> synonymNames = new LinkedHashSet<>();

        // Add all possible names something could be seen as
        names.addAll(this.analyser.analyseNames(classifier, this.name, this.fullName, this.additionalName, true));
        classifier.setNames(names);
        if (!this.name.getMultiplicity().isMany() && names.size() > 1) {
            LOGGER.warn("Classifier " + id + " with single value name " + this.name.getId() + " has multiple names " + names);
        } else {
            for (String nm : names) { // Add any new and interesting variants of the name
                Boolean match = classifier.match(nm, this.name);
                if (match == null || !match)
                    classifier.add(this.name, nm);
            }
        }

        // Add alternative names
        if (this.altName.isPresent()) {
            altNames.addAll(this.analyser.analyseNames(classifier, this.name, this.fullName, this.additionalName,false).stream().filter(n -> !names.contains(n)).collect(Collectors.toSet()));
            if (!this.altName.get().getMultiplicity().isMany() && altNames.size() > 1) {
                LOGGER.warn("Classifier " + id + " with single value alternate name " + this.altName.get().getId() + " has multiple names " + altNames);
            } else {
                for (String nm : altNames) {
                    classifier.add(this.altName.get(), nm);
                }
            }
        }
        // Add synonyms
        if (this.synonymName.isPresent() && this.accepted.isPresent()) {
            Iterable<Classifier> synonyms = this.loadStore.getAll(this.conceptTerm, new Observation(true, this.accepted.get(), id));
            for (Classifier synonym : synonyms) {
                synonymNames.addAll(this.analyser.analyseNames(synonym, this.name, this.fullName, this.additionalName, true));
            }
            if (!this.synonymName.get().getMultiplicity().isMany() && altNames.size() > 1) {
                LOGGER.warn("Classifier " + id + " with single value synonym name " + this.synonymName.get().getId() + " has multiple names " + altNames);
            } else {
                for (String nm : synonymNames) {
                    classifier.add(this.synonymName.get(), nm);
                }
            }
        }
        List<String> trail = parents.stream().map(p -> p.get(this.identifier).toString()).collect(Collectors.toList());
        Collections.reverse(trail);
        classifier.setTrail(trail);
        this.builder.infer(classifier);
        if (this.parent.isPresent()) {
            parents.push(classifier);
            Iterable<Classifier> children = this.loadStore.getAll(this.conceptTerm, new Observation(true, this.parent.get(), id));
            for (Classifier child : children) {
                index = this.expandTree(child, parents, index, counter);
            }
            parents.pop();
        }
        classifier.setIndex(left, index);
        this.buildWeight(classifier);
        this.expandedStore.store(classifier);
        counter.increment(classifier.getIdentifier() + ": " + id);
        return index + 1;
    }

    /**
     * Expand all synonyms, including the portions of the parent taxonomy that are accurate.
     *
     * @throws BayesianException if unable to manage the updates
     */
    public void expandSynonyms() throws BayesianException {
        LOGGER.info("Expanding synonyms");
        Counter counter = new Counter("Processed {0} synonyms, {2,number,0.0}/s, last {4}", LOGGER, this.config.getLogInterval(), -1);
        Observation isSynonym = this.loadStore.getAnnotationObservation(BayesianTerm.isSynonym);
        Iterable<Classifier> synonyms = this.loadStore.getAll(this.conceptTerm, isSynonym);
        this.registerJmx(counter, "synonyms");
        counter.start();
        for (Classifier classifier: synonyms) {
            String id = classifier.get(this.identifier);
            Optional<String> aid = this.accepted.map(acc -> classifier.get(acc));
            Optional<Classifier> acpt = Optional.empty();
            if (!aid.isPresent()) {
                LOGGER.error("Synonym document " + id + " does not have an accepted taxon id");
            } else {
                acpt = Optional.ofNullable(this.expandedStore.get(this.conceptTerm, this.identifier, aid.get()));
                if (!acpt.isPresent()) {
                    LOGGER.error("Taxon " + id + " with accepted id " + aid + " does not have a matching accepted taxon");
                }
            }
            this.expandSynonym(classifier, acpt);
            this.buildWeight(classifier);
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
     * @throws BayesianException if unable to write the updated document
     */
    public void expandSynonym(Classifier classifier, Optional<Classifier> accepted) throws BayesianException {
        Set<String> allNames = new LinkedHashSet<>();
        allNames.addAll(this.analyser.analyseNames(classifier, this.name, this.fullName, this.additionalName, true));
        classifier.setNames(allNames);
        for (String nm: allNames)
            classifier.add(this.name, nm);
        if (accepted.isPresent()){
            Classifier acc = accepted.get();
            for (Observable obs: this.copy) {
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
     * @throws BayesianException if unable to compute the parameters
     */
    public void buildParameters() throws BayesianException {
        LOGGER.info("Building parameter sets");
        this.parameterisedStore = config.createLoadStore(Annotator.NULL);
        final Counter counter = new Counter("Processed {0} parameter sets, {2,number,0.0}/s, last {4}", LOGGER, this.config.getLogInterval(), -1);
        final ParameterAnalyser analyser = this.expandedStore.getParameterAnalyser(this.network, this.weight, this.config.getDefaultWeight());
        final BlockingQueue<Classifier> workQueue = new LinkedBlockingQueue<Classifier>(this.config.getThreads() * 4);
        final Classifier poisonPill = new SimpleClassifier();
        Iterable<Classifier> taxa = this.expandedStore.getAll(this.conceptTerm);
        List<ParameterConsumer> consumers = IntStream.range(0, this.config.getThreads()).mapToObj(i -> new ParameterConsumer(analyser, workQueue, counter, poisonPill)).collect(Collectors.toList());
        List<Thread> consumerThreads = consumers.stream().map(Thread::new).collect(Collectors.toList());
        consumerThreads.forEach(t -> t.start());
        this.registerJmx(counter, "parameters");
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

    protected void buildParameters(Classifier classifier, ParameterAnalyser analyser) throws BayesianException {
        String id = classifier.get(this.identifier);
        String signature = this.builder.buildSignature(classifier);
        classifier.setSignature(signature);
        Parameters parameters = this.builder.calculate(analyser, classifier);
        classifier.storeParameters(parameters);
        this.parameterisedStore.store(classifier);

    }

    /**
     * Build the weight of the classifier.
     *
     * @param classifier The classifier
     *
     * @throws BayesianException if unable to computer the weight.
     *
     * @see WeightAnalyser
     */
    protected void buildWeight(Classifier classifier) throws BayesianException {
        Double weight = classifier.get(this.weight);
        if (weight == null)
            weight = this.weightAnalyser.weight(classifier);
        weight = this.weightAnalyser.modify(classifier, weight);
        if (weight < 1.0)
            throw new BuilderException("Weight must be greater than or equoal to 1 for " + classifier.get(this.identifier) + " weight " + weight);
        classifier.replace(this.weight, weight);
    }

    /**
     * Build the final matching index for use by classification matchers.
     *
     * @param output The directory to write to
     *
     * @throws BayesianException if unable to build
     */
    public void buildIndex(File output) throws BayesianException {
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
        Iterable<Classifier> taxa = this.parameterisedStore.getAll(this.conceptTerm);
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
     * @throws BayesianException if unable to construct the document
     */
    public Classifier createMetadata() throws BayesianException {
        Classifier metadata = this.loadStore.newClassifier();
        Observable creator = new Observable(DcTerm.creator);
        creator.setProperty(BayesianTerm.index, false);
        Observable created = new Observable(DcTerm.created);
        created.setProperty(BayesianTerm.index, false);
        Observable description = new Observable(DcTerm.description);
        description.setProperty(BayesianTerm.index, false);
        Observable identifier = new Observable(DcTerm.identifier);
        Observable title = new Observable(DcTerm.title);
        title.setProperty(BayesianTerm.index, false);
        Observable source = new Observable(DcTerm.source);
        source.setProperty(BayesianTerm.index, false);
        Observable builderClass = new Observable(BayesianTerm.builderClass);
        builderClass.setProperty(BayesianTerm.index, false);
        Observable version = new Observable(DcTerm.hasVersion);
        version.setProperty(BayesianTerm.index, false);
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
            throw new StoreException("Unable to write network configuration", ex);
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
     * @throws BayesianException if unable to generate required values or create an annotation
     */
    @Override
    public void annotate(Classifier classifier) throws BayesianException {
        this.builder.generate(classifier);
        Term type = classifier.getType();
        String id = classifier.get(this.identifier);
        String p = this.parent.isPresent() ? classifier.get(this.parent.get()) : null;
        String a = this.accepted.isPresent() ? classifier.get(this.accepted.get()) : null;

        if (id == null || this.identifiers.contains(id)) {
            LOGGER.warn("Non-unique or null identifier " + id + " replacing with " + classifier.getIdentifier());
            id = classifier.getIdentifier();
            classifier.replace(this.identifier, id);
            classifier.annotate(BayesianTerm.identifierCreated);
        }
        this.identifiers.add(id);
        if (type != this.conceptTerm)
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
                source = new CSVSource(builder.conceptTerm, new FileReader(in), builder.getFactory(), builder.getNetwork().getObservables());
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

    /**
     * If called, will register the counters etc. for JMX management
     */
    public void registerJmx(Counter counter, String name) {
        if (this.config.isEnableJmx()) {
            try {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                String network = this.getNetwork().getJavaVariable();
                ObjectName on = new ObjectName(this.getClass().getPackage().getName() + ":type=Counter,network=" + network + ",name=" + name);
                mbs.registerMBean(counter, on);
            } catch (Exception ex) {
                LOGGER.error("Unable to register counter " + name, ex);
            }
        }
    }
}
