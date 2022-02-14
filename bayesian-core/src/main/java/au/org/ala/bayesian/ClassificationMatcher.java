package au.org.ala.bayesian;

import au.org.ala.util.BacktrackingIterator;
import au.org.ala.util.Service;
import au.org.ala.util.Statistics;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provide search services for a classification.
 * <p>
 * A classification is provided and the best possible match for the classification
 * is then returned.
 * </p>
 * <p>
 * This class can be used stand-along but, generally, more application-specific matchers
 * can be aligned to the
 * </p>
 *
 * @param <C> The classification class this matcher is for
 * @param <I> The inferencer used by the matcher
 * @param <F> The factory class for classifications, analysers etc.
 * @param <M> The measurement statistics
 */
@Service
public class ClassificationMatcher<C extends Classification<C>, I extends Inferencer<C>, F extends NetworkFactory<C, I, F>, M extends MatchMeasurement> implements AutoCloseable, ClassificationMatcherMXBean {
    private static final Logger logger = LoggerFactory.getLogger(ClassificationMatcher.class);

    /**
     * The default possible theshold for something to be considered. @see #isPossible
     */
    public static double POSSIBLE_THRESHOLD = 0.05;
    /**
     * The default immediately acceptable threshold for something to be regarded as accepted. @see @isImmediateMatch
     */
    public static double IMMEDIATE_THRESHOLD = 0.99;
    /**
     * The default acceptable threshold for something. @see @isAcceptableMatch
     */
    public static double ACCEPTABLE_THRESHOLD = 0.90;
    /**
     * The amount that the evidence is allowed to be below the prior probability
     */
    public static double EVIDENCE_SLOP = 20.0;
    /**
     * The amount of evidence before something is too unlikely
     */
    public static double EVIDENCE_THRESHOLD = Inference.MINIMUM_PROBABILITY * 10.0;

    /**
     * The default match sort
     */
    protected Comparator<Match<C, M>> DEFAULT_SORT = (m1, m2) -> -Double.compare(m1.getProbability().getPosterior(), m2.getProbability().getPosterior());

    /**
     * The configuration associated with this matcher
     */
    @Getter
    private ClassificationMatcherConfiguration config;
    @Getter
    private F factory;
    @Getter
    private ClassifierSearcher<?> searcher;
    @Getter
    private I inferencer;
    @Getter
    private Analyser<C> analyser;
    @Getter
    private Optional<Observable<String>> identifier;
    @Getter
    private Optional<Observable<String>> accepted;
    /**
     * The management bean if this is available for monitoring
     */
    private ObjectInstance mbean;
    /**
     * The timing statistics
     */
    @Getter
    private Statistics timeStatistics = new Statistics("elapsed time");
    /**
     * The search statistics
     */
    @Getter
    private Statistics searchStatistics = new Statistics("searches");
    /**
     * The search modification statistics
     */
    @Getter
    private Statistics searchModificationStatistics = new Statistics("search modifications");
    /**
     * The retrieval statistics
     */
    @Getter
    private Statistics candidateStatistics = new Statistics("candidates retrieved");
    /**
     * The maximum acceptable candidiate statistics
     */
    @Getter
    private Statistics maxCandidateStatistics = new Statistics("maximum acceptable candidate");
    /**
     * The hint modification statistics
     */
    @Getter
    private Statistics hintModificationStatistics = new Statistics("hint modifications");
    /**
     * The hint modification statistics
     */
    @Getter
    private Statistics matchStatistics = new Statistics("matches");
    /**
     * The hint modification statistics
     */
    @Getter
    private Statistics matchModificationStatistics = new Statistics("match modifications");
    /**
     * The hint modification statistics
     */
    @Getter
    private Statistics matchableStatistics = new Statistics("matchable");

    /**
     * Create with a searcher and inferencer.
     *
     * @param factory  The factory for creating objects for the matcher to work on
     * @param searcher The mechanism for getting candidiates
     * @param config   The configuration. If null a default configuration is used.
     */
    public ClassificationMatcher(F factory, ClassifierSearcher<?> searcher, ClassificationMatcherConfiguration config) {
        this.config = config != null ? config : ClassificationMatcherConfiguration.builder().build();
        this.factory = factory;
        this.searcher = searcher;
        this.inferencer = this.factory.createInferencer();
        this.analyser = this.factory.createAnalyser();
        this.identifier = this.factory.getIdentifier();
        this.accepted = this.factory.getAccepted();
        if (this.config.isEnableJmx()) {
            String network = this.factory.getNetworkId().replaceAll("[^A-Za-z0-9.]+", ".");
            try {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                ObjectName on = new ObjectName(this.getClass().getPackage().getName() + ":type=" + this.getClass().getSimpleName() + ",network=" + network);
                this.mbean = mbs.registerMBean(this, on);
            } catch (Exception ex) {
                logger.error("Unable to register searcher " + network, ex);
            }
        }
    }

    /**
     * Close the matcher
     *
     * @throws StoreException if unable to close for some reason.
     */
    @Override
    public void close() throws Exception {
        if (this.mbean != null) {
            try {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                mbs.unregisterMBean(this.mbean.getObjectName());
            } catch (Exception ex) {
                logger.error("Unable to deregister searcher " + mbean.getObjectName(), ex);
            }
        }
    }

    /**
     * Find a match for a classification
     *
     * @param classification The classification to match
     * @return A match, {@link Match#invalidMatch()} is returned if no match is found
     * @throws BayesianException if there is a failure during inference
     */
    @NonNull
    public Match<C, M> findMatch(@NonNull C classification) throws BayesianException {
        M measurement = null;
        if (this.config.isStatistics() || this.config.isInstrument()) {
            measurement = this.createMeasurement();
            measurement.start();
        }
        Match<C, M> match = this.findMatch(classification, measurement);
        if (measurement != null)
            measurement.stop();
        this.recordMeasurement(measurement);
        match = match.getActual() == null ? match : match.with(classification.buildFidelity(match.getActual()));
        return this.config.isInstrument() ? match.with(measurement) : match;
    }

    /**
     * Find a match for a classification
     *
     * @param classification The classification to match
     * @return A match, {@link Match#invalidMatch()} is returned if no match is found
     * @throws BayesianException if there is a failure during inference
     */
    @NonNull
    protected Match<C, M> findMatch(@NonNull C classification, M measurement) throws BayesianException {
        classification.inferForSearch(this.analyser);
        classification = this.prepareForMatching(classification);

        // Immediate search
        Match<C, M> match = this.findSource(classification, true, measurement);
        Match<C, M> bad = Match.invalidMatch();
        if (match != null && match.isValid())
            return match;
        if (match != null && !match.isValid())
            bad = match;

        // Search for modified version
        C base = this.prepareForSourceModification(classification);
        C modified = null;
        C previous = base;
        Iterator<C> sourceClassifications = new BacktrackingIterator<>(base, base.searchModificationOrder());
        while (sourceClassifications.hasNext()) {
            modified = sourceClassifications.next();
            if (modified == previous)
                continue;
            if (measurement != null)
                measurement.searchModification();
            modified.inferForSearch(this.analyser);
            match = this.findSource(modified, true, measurement);
            if (match != null && match.isValid())
                return match;
            previous = modified;
        }
        return bad;
    }

    /**
     * Find a match based on a source document.
     *
     * @param classification The (pre-inferred) classification
     * @param modify         Allow evidence modifications
     * @param measurement    The performance measurement (null for no measurement)
     * @return A match or null for no match
     * @throws BayesianException if there is an error in search or inference
     */
    @NonNull
    protected Match<C, M> findSource(@NonNull C classification, boolean modify, M measurement) throws BayesianException {
        if (measurement != null)
            measurement.search();
        List<? extends Classifier> candidates = this.getSearcher().search(classification);
        if (measurement != null)
            measurement.addCandidates(candidates.size());
        if (candidates.isEmpty())
            return null;

        // First do a basic match and see if we have something easily matchable
        Match<C, M> match = this.findMatch(classification, candidates, modify, measurement);
        Match<C, M> bad = null;
        if (match != null && match.isValid())
            return match;
        if (match != null && !match.isValid())
            bad = match;

        // Try with hints
        if (modify) {
            C base = this.prepareForHintModification(classification);
            C modified = null;
            C previous = base;
            Iterator<C> subClassifications = new BacktrackingIterator<C>(base, base.hintModificationOrder());
            while (subClassifications.hasNext()) {
                modified = subClassifications.next();
                if (modified == classification || modified == previous) // Skip null case
                    continue;
                if (measurement != null)
                    measurement.hintModification();
                match = this.findMatch(modified, candidates, modify, measurement);
                if (match != null && match.isValid())
                    return match;
                previous = modified;
            }
        }
        return bad;
    }

    /**
     * Find a match based on a source document.
     * <p>
     * If it's not findable and there are hints, try using hints.
     * </p>
     *
     * @param classification The (pre-inferred) classification
     * @param candidates     The list of possible candidiates
     * @param modify         Allow evidence modifications
     * @param measurement    The measurement for the searcg, null for no collection
     * @return A match or null for no match
     * @throws BayesianException if there is an error in search or inference
     */
    protected Match<C, M> findMatch(@NonNull C classification, List<? extends Classifier> candidates, boolean modify, MatchMeasurement measurement) throws BayesianException {
        if (measurement != null)
            measurement.match();
        // First do a basic match and see if we have something easily matchable
        List<Match<C, M>> results = this.doMatch(classification, candidates, measurement);
        if (measurement != null)
            measurement.addMatchable(results.size());
        Match<C, M> match = this.findSingle(classification, results, measurement);
        Match<C, M> bad = null;
        if (match != null && match.isValid())
            return match;
        if (match != null && !match.isValid())
            bad = match;

        // Now try modifictions in order
        if (modify && results.stream().allMatch(r -> this.isBadEvidence(r))) {
            C base = this.prepareForMatchModification(classification);
            C modified = null;
            C previous = base;
            Iterator<C> subClassifications = new BacktrackingIterator<C>(base, base.matchModificationOrder());
            while (subClassifications.hasNext()) {
                modified = subClassifications.next();
                if (modified == classification || modified == previous) // Skip null case
                    continue;
                if (measurement != null)
                    measurement.matchModification();
                results = this.doMatch(modified, candidates, measurement);
                match = this.findSingle(modified, results, measurement);
                if (match != null && match.isValid())
                    return match;
                previous = modified;
            }
        }
        return bad;
    }

    /**
     * Prepare a classification for matching.
     * <p>
     * Subclasses can do clever stuff here, like infer higher-order information.
     * By default, the unmodified classification is returned.
     * </p>
     *
     * @param classification The classification
     * @return The prepared classification
     * @throws BayesianException if unable to prepare the classification
     */
    protected C prepareForMatching(C classification) throws BayesianException {
        return classification;
    }

    /**
     * Prepare a classification for modification when looking for a source classifiers.
     * <p>
     * Subclasses can do clever stuff here, like infer higher-order information.
     * By default, the unmodified classification is returned.
     * </p>
     *
     * @param classification The classification
     * @return The prepared classification
     * @throws BayesianException if unable to prepare the classification
     */
    protected C prepareForSourceModification(C classification) throws BayesianException {
        return classification;
    }

    /**
     * Prepare a classification for modification when looking for a match.
     * <p>
     * Subclasses can do clever stuff here, like infer higher-order information.
     * By default, the unmodified classification is returned.
     * </p>
     *
     * @param classification The classification
     * @return The prepared classification
     * @throws BayesianException if unable to prepare the classification
     */
    protected C prepareForMatchModification(C classification) throws BayesianException {
        return classification;
    }


    /**
     * Prepare a classification for modification when looking for a hinted match.
     * <p>
     * Subclasses can do clever stuff here, like infer higher-order information.
     * By default, the unmodified classification is returned.
     * </p>
     *
     * @param classification The classification
     * @return The prepared classification
     * @throws BayesianException if unable to prepare the classification
     */
    protected C prepareForHintModification(C classification) throws BayesianException {
        return classification;
    }

    protected List<Match<C, M>> doMatch(C classification, List<? extends Classifier> candidates, MatchMeasurement measurement) throws BayesianException {
        int index = 0;
        int maxCandidate = 0;
        List<Match<C, M>> results = new ArrayList<>(candidates.size());
        for (Classifier candidate : candidates) {
            index++;
            if (!this.isValidCandidate(classification, candidate))
                continue;
            Inference inference = this.inferencer.probability(classification, candidate);
            if (!this.isPossible(classification, candidate, inference))
                continue;
            C candidateClassification = this.factory.createClassification();
            candidateClassification.read(candidate, true);
            Match<C, M> match = new Match<>(classification, candidate, candidateClassification, inference);
            results.add(match);
            maxCandidate = index;
        }
        results.sort(this.getMatchSorter());
        results = this.resolve(results);
        results = this.annotate(results);
        if (measurement != null)
            measurement.maxCandidate(maxCandidate);
        return results;
    }

    /**
     * Resolve the results to ensure that accepted values are kept.
     *
     * @param results The results
     * @return The resolved results.
     */
    protected List<Match<C, M>> resolve(List<Match<C, M>> results) {
        return results.stream().map(this::resolve).collect(Collectors.toList());
    }

    /**
     * Add the accepted taxon to the match for any synonym.
     *
     * @param match The match
     * @return The original match, if not a synonym or the match with the accepted result
     */
    protected Match<C, M> resolve(Match<C, M> match) {
        try {
            String acceptedId = match.getMatch().getAccepted();
            if (acceptedId == null || !this.accepted.isPresent() || !this.identifier.isPresent())
                return match;
            Classifier acceptedCandidate = this.getSearcher().get(match.getMatch().getType(), this.identifier.get(), acceptedId);
            if (acceptedCandidate == null)
                return match;
            C acceptedClassification = this.factory.createClassification();
            acceptedClassification.read(acceptedCandidate, true);
            return match.withAccepted(acceptedCandidate, acceptedClassification);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to match " + match, ex);
        }
    }

    /**
     * Annotate the result list with additional information and issues.
     *
     * @param results The list of results
     * @return The modified set of matches
     * @throws BayesianException if there is a problem retrieving or inferring information
     */
    protected List<Match<C, M>> annotate(List<Match<C, M>> results) throws BayesianException {
        return results;
    }

    /**
     * Find a single match that is acceptable.
     *
     * @param classification The template classification
     * @param results        The list of results
     * @param measurement    The measuremnent, if null then ignored
     * @return A single acceptable result, or null
     * @throws BayesianException if there is an inference or retrieval error
     */
    protected Match<C, M> findSingle(C classification, List<Match<C, M>> results, MatchMeasurement measurement) throws BayesianException {
        if (!results.isEmpty()) {
            Match<C, M> first = results.get(0);
            // Look for an immediate match
            if (this.isImmediateMatch(first))
                return first;
            // See if we have a single acceptable answer
            List<Match<C, M>> acceptable = results.stream().filter(this::isAcceptableMatch).collect(Collectors.toList());
            if (acceptable.size() == 1)
                return acceptable.get(0);
        }
        return null;
    }


    /**
     * Find the least upper bound for parents of a list of sources.
     * <p>
     * This uses the trail contained in each accepted classifier to find
     * the lowest common identifier and then generate
     * </p>
     *
     * @param sources The sources list
     * @return The lowest parent that each
     * @throws BayesianException if unable to compute the lub
     */
    protected Match<C, M> lub(List<Match<C, M>> sources) throws BayesianException {
        if (sources.isEmpty())
            return null;

        Match<C, M> exemplar = sources.get(0);
        String id = null;
        List<List<String>> trails = sources.stream().map(m -> m.getAcceptedCandidate().getTrail()).collect(Collectors.toList());
        int level = 0;
        int limit = trails.stream().mapToInt(t -> t.size()).min().orElse(0);
        boolean same = true;

        while (level < limit && same) {
            final int l = level;
            final String test = trails.get(0).get(l);
            same = trails.stream().allMatch(s -> s.get(l).equals(test));
            if (same)
                id = test;
            level++;
        }
        if (id != null) {
            Classifier lub = this.getSearcher().get(exemplar.getAccepted().getType(), this.getIdentifier().get(), id);
            C lubClassification = this.getFactory().createClassification();
            lubClassification.read(lub, true);
            return exemplar.withAccepted(lub, lubClassification).boost(sources);
        }
        return null;
    }


    /**
     * Get the sorting method for a list of matches
     *
     * @return The sort to use
     */
    protected Comparator<Match<C, M>> getMatchSorter() {
        return DEFAULT_SORT;
    }

    /**
     * Initial check to see if a candidate is anywhere near acceptable.
     * <p>
     * This can be used to winnow the results quickly before doing further inference.
     * </p>
     *
     * @param classification The template classification
     * @param candidate      The candidate classifier
     * @return True if this can be used as a possible match
     * @throws BayesianException if unable to determine whether this is a valid candidate
     */
    protected boolean isValidCandidate(C classification, Classifier candidate) throws BayesianException {
        return true;
    }

    /**
     * Is this a possible match.
     * <p>
     * Generally, this means something above the minmum possible threshold
     * and with a p(E | H) greater than the p(H), meaning that the evidence
     * isn't completely unlikely.
     * Subclasses can get more creative, if required.
     * </p>
     *
     * @param classification The source classification
     * @param candidate      The candidate classifier
     * @param inference      The match probability
     * @return True if this is a possible match
     */
    protected boolean isPossible(C classification, Classifier candidate, Inference inference) {
        return inference.getPosterior() >= POSSIBLE_THRESHOLD && inference.getBoost() >= 1.0;
    }

    /**
     * Is this an immediate match, meaning that it will be used as a valid match
     * without the need to examine other possibilities.
     * <p>
     * Generally, this means something above the acceptable threshold probability
     * for both the posterior probability and the conditional evidence.
     * Subclasses can get more creative, if required.
     * </p>
     *
     * @param match The potential match
     * @return True if this is a possible match
     */
    protected boolean isImmediateMatch(Match<C, M> match) {
        Inference p = match.getProbability();
        return p.getPosterior() >= IMMEDIATE_THRESHOLD && p.getConditional() >= IMMEDIATE_THRESHOLD;
    }

    /**
     * Is this match acceptable?
     * <p>
     * Generally, this means something that looks like it might have a chance of
     * being the right thing.
     * </p>
     *
     * @param match The candidate match
     * @return True if this is an acceptable match
     */
    protected boolean isAcceptableMatch(Match<C, M> match) {
        Inference p = match.getProbability();
        return p.getPosterior() >= ACCEPTABLE_THRESHOLD && p.getEvidence() >= EVIDENCE_THRESHOLD && p.getConditional() >= POSSIBLE_THRESHOLD;
    }

    /**
     * Does this match indicate an evidence problem.
     * <p>
     * Bad matches can occur when the likelihood of the evidence is so low
     * that it is overwhelming priors.
     * </p>
     *
     * @param match The candidate match
     * @return True if this match is an example of bad evidence.
     */
    protected boolean isBadEvidence(Match<C, M> match) {
        Inference p = match.getProbability();
        return p.getPrior() > p.getEvidence() * EVIDENCE_SLOP || p.getPrior() > p.getConditional();
    }


    /**
     * Record the results of a measurement.
     *
     * @param measurement The measurement object. May be null for no measurements
     */
    protected void recordMeasurement(M measurement) {
        if (measurement == null)
            return;
        if (this.config.isStatistics()) {
            this.timeStatistics.add(measurement.getElapsed());
            this.searchModificationStatistics.add(measurement.getSearchModifications());
            this.searchStatistics.add(measurement.getSearches());
            this.candidateStatistics.add(measurement.getCandidates());
            this.maxCandidateStatistics.add(measurement.getMaxCandidate());
            this.hintModificationStatistics.add(measurement.getHintModifications());
            this.matchStatistics.add(measurement.getMatches());
            this.matchModificationStatistics.add(measurement.getMatchModifications());
            this.matchableStatistics.add(measurement.getMatchable());
        }
    }

    /**
     * Print the performance statistics to an output
     *
     * @param writer The output writer
     */
    public void reportStatistics(Writer writer) {
        PrintWriter pw = new PrintWriter(writer);
        pw.println("Performance summary for " + this.factory.getNetworkId());
        pw.println(this.timeStatistics);
        pw.println(this.searchModificationStatistics);
        pw.println(this.searchStatistics);
        pw.println(this.candidateStatistics);
        pw.println(this.maxCandidateStatistics);
        pw.println(this.hintModificationStatistics);
        pw.println(this.matchStatistics);
        pw.println(this.matchModificationStatistics);
        pw.println(this.matchableStatistics);
    }

    /**
     * Create a measurement for instrumenting the search.
     * <p>
     * By default, this returns a plain {@link MatchMeasurement}
     * </p>
     *
     * @return A new measurement, approprate to the matcher
     */
    protected M createMeasurement() {
        return (M) new MatchMeasurement();
    }

    /**
     * Get the number of requests
     *
     * @return The number of requests
     */
    @Override
    public long getRequests() {
        return this.timeStatistics.getCount();
    }

    /**
     * Get a summary of the time elapsed statistics
     *
     * @return The time statitics
     */
    @Override
    public String getTimeStatistics() {
        return this.timeStatistics.toString();
    }

    /**
     * Get a summary of the search statistics
     *
     * @return The search statistics
     */
    @Override
    public String getSearchStatistics() {
        return this.searchStatistics.toString();
    }

    /**
     * Get a summary of the search modification statistics
     *
     * @return The search modification statistics
     */
    @Override
    public String getSearchModificationStatistics() {
        return this.searchModificationStatistics.toString();
    }

    /**
     * Get a summary of the retrieval statistics
     *
     * @return The retrieval statistics
     */
    @Override
    public String getCandidateStatistics() {
        return this.candidateStatistics.toString();
    }

    /**
     * Get a summary of the maximum acceptable candidiate statistics
     *
     * @return The maximum acceptable candidiate statistics
     */
    @Override
    public String getMaxCandidateStatistics() {
        return this.maxCandidateStatistics.toString();
    }

    /**
     * Get a summary of the hint modification statistics
     *
     * @return The hint modification statistics
     */
    @Override
    public String getHintModificationStatistics() {
        return this.hintModificationStatistics.toString();
    }

    /**
     * Get a summary of the hint modification statistics
     *
     * @return The hint modification statistics
     */
    @Override
    public String getMatchStatistics() {
        return this.matchStatistics.toString();
    }

    /**
     * Get a summary of the hint modification statistics
     *
     * @return The hint modification statistics
     */
    @Override
    public String getMatchModificationStatistics() {
        return this.matchModificationStatistics.toString();
    }

    /**
     * Get a summary of the hint modification statistics
     *
     * @return The hint modification statistics
     */
    @Override
    public String getMatchableStatistics() {
        return this.matchableStatistics.toString();
    }
}
