package au.org.ala.bayesian;

import au.org.ala.util.BacktrackingIterator;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provide search services for a classification.
 * <p>
 * A classification is provided and the best possible match for the classification
 * is then returned.
 * </p>
 */
public class ClassificationMatcher<C extends Classification<C>, I extends Inferencer<C>, F extends NetworkFactory<C, I, F>> {
    private static final Logger logger = LoggerFactory.getLogger(ClassificationMatcher.class);

    /** The default possible theshold for something to be considered. @see #isPossible */
    public static double POSSIBLE_THRESHOLD = 0.05;
    /** The default immediately acceptable threshold for something to be regarded as accepted. @see @isImmediateMatch */
    public static double IMMEDIATE_THRESHOLD = 0.99;
    /** The default acceptable threshold for something. @see @isAcceptableMatch */
    public static double ACCEPTABLE_THRESHOLD = 0.90;
    /** The amount that the evidence is allowed to be below the prior probability */
    public static double EVIDENCE_SLOP = 20.0;
    /** The amount of evidence before something is too unlikely */
    public static double EVIDENCE_THRESHOLD = Inference.MINIMUM_PROBABILITY * 10.0;

    /** The default match sort */
    protected Comparator<Match<C>> DEFAULT_SORT = (m1, m2) -> - Double.compare(m1.getProbability().getPosterior(), m2.getProbability().getPosterior());

    @Getter
    private F factory;
    @Getter
    private ClassifierSearcher<?> searcher;
    @Getter
    private I inferencer;
    @Getter
    private Analyser<C> analyser;
    @Getter
    private Optional<Observable> identifier;
    @Getter
    private Optional<Observable> accepted;

    /**
     * Create with a searcher and inferencer.
     *
     * @param factory The factory for creating objects for the matcher to work on
     * @param searcher The mechanism for getting candidiates
     */
    public ClassificationMatcher(F factory, ClassifierSearcher<?> searcher) {
        this.factory = factory;
        this.searcher = searcher;
        this.inferencer = this.factory.createInferencer();
        this.analyser = this.factory.createAnalyser();
        this.identifier = this.factory.getIdentifier();
        this.accepted = this.factory.getAccepted();
    }

    /**
     * Find a match for a classification
     *
     * @param classification The classification to match
     *
     * @return A match, {@link Match#invalidMatch()} is returned if no match is found
     *
     * @throws BayesianException if there is a failure during inference
      */
    @NonNull
    public Match<C> findMatch(@NonNull C classification) throws BayesianException{
        classification.inferForSearch();
        classification = this.prepareForMatching(classification);

        // Immediate search
        Match<C> match = this.findSource(classification, true);
        Match<C> bad = Match.invalidMatch();
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
            modified.inferForSearch();
            match = this.findSource(modified, true);
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
     * @param modify Allow evidence modifications
     *
     * @return A match or null for no match
     *
     * @throws BayesianException if there is an error in search or inference
     */
    protected Match<C> findSource(@NonNull C classification, boolean modify) throws BayesianException {
        List<? extends Classifier> candidates = this.getSearcher().search(classification);
        if (candidates.isEmpty())
            return null;

        // First do a basic match and see if we have something easily matchable
        Match<C> match = this.findMatch(classification, candidates, modify);
        Match<C> bad = null;
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
                match = this.findMatch(modified, candidates, modify);
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
     * @param modify Allow evidence modifications
     *
     * @return A match or null for no match
     *
     * @throws BayesianException if there is an error in search or inference
     */
    protected Match<C> findMatch(@NonNull C classification, List<? extends Classifier> candidates, boolean modify) throws BayesianException {
        // First do a basic match and see if we have something easily matchable
        List<Match<C>> results = this.doMatch(classification, candidates);
        Match<C> match = this.findSingle(classification, results);
        Match<C> bad = null;
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
                results = this.doMatch(modified, candidates);
                match = this.findSingle(modified, results);
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
     *
     * @return The prepared classification
     *
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
     *
     * @return The prepared classification
     *
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
     *
     * @return The prepared classification
     *
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
     *
     * @return The prepared classification
     *
     * @throws BayesianException if unable to prepare the classification
     */
    protected C prepareForHintModification(C classification) throws BayesianException {
        return classification;
    }

    protected List<Match<C>> doMatch(C classification, List<? extends Classifier> candidates) throws BayesianException {
        List<Match<C>> results = new ArrayList<>(candidates.size());
        for (Classifier candidate: candidates) {
            if (!this.isValidCandidate(classification, candidate))
                continue;
            Inference inference = this.inferencer.probability(classification, candidate);
            if (!this.isPossible(classification, candidate, inference))
                continue;
            C candidateClassification = this.factory.createClassification();
            candidateClassification.read(candidate, true);
            Match<C> match = new Match<>(classification, candidate, candidateClassification, inference);
            results.add(match);
        }
        results.sort(this.getMatchSorter());
        results = this.resolve(results);
        results = this.annotate(results);
        return results;
    }

    /**
     * Resolve the results to ensure that accepted values are kept.
     *
     * @param results The results
     *
     * @return The resolved results.
     */
    protected List<Match<C>> resolve(List<Match<C>> results) {
        return results.stream().map(this::resolve).collect(Collectors.toList());
    }

    /**
     * Add the accepted taxon to the match for any synonym.
     *
     * @param match The match
     *
     * @return The original match, if not a synonym or the match with the accepted result
      */
    protected Match<C> resolve(Match<C> match)  {
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
     *
     * @throws BayesianException if there is a problem retrieving or inferring information
     *
     * @return The modified set of matches
     */
    protected List<Match<C>> annotate(List<Match<C>> results) throws BayesianException {
        return results;
    }

    /**
     * Find a single match that is acceptable.
     *
     * @param classification The template classification
     * @param results The list of results
     *
     * @return A single acceptable result, or null
     *
     * @throws BayesianException if there is an inference or retrieval error
     */
    protected Match<C> findSingle(C classification, List<Match<C>> results) throws BayesianException {
        if (!results.isEmpty()) {
            Match<C> first = results.get(0);
            // Look for an immediate match
            if (this.isImmediateMatch(first))
                return first;
            // See if we have a single acceptable answer
            List<Match<C>> acceptable = results.stream().filter(this::isAcceptableMatch).collect(Collectors.toList());
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
     *
     * @return The lowest parent that each
     *
     * @throws BayesianException if unable to compute the lub
     */
    protected Match<C> lub(List<Match<C>> sources) throws BayesianException {
        if (sources.isEmpty())
            return null;

        Match<C> exemplar = sources.get(0);
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
    protected Comparator<Match<C>> getMatchSorter() {
        return DEFAULT_SORT;
    }

    /**
     * Initial check to see if a candidate is anywhere near acceptable.
     * <p>
     * This can be used to winnow the results quickly before doing further inference.
     * </p>
     *
     * @param classification The template classification
     * @param candidate The candidate classifier
     *
     * @return True if this can be used as a possible match
     *
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
     * @param candidate The candidate classifier
     * @param inference The match probability
     *
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
     *
     * @return True if this is a possible match
     */
    protected boolean isImmediateMatch(Match<C> match) {
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
     *
     * @return True if this is an acceptable match
     */
    protected boolean isAcceptableMatch(Match<C> match) {
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
     *
     * @return True if this match is an example of bad evidence.
     */
    protected boolean isBadEvidence(Match<C> match) {
        Inference p = match.getProbability();
        return p.getPrior() > p.getEvidence() * EVIDENCE_SLOP || p.getPrior() > p.getConditional();
    }
}
