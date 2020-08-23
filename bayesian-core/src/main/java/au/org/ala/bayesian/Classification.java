package au.org.ala.bayesian;

import lombok.NonNull;
import org.gbif.dwc.terms.Term;

import java.util.Collection;

/**
 * The base class for classifications.
 * <p>
 * Classifications hold the vector of evidence that make up a individual piece of data.
 * </p>
 */
public interface Classification<C extends Classification> {
    /**
     * Get the type of the classification
     *
     * @return The sort of thing this classification is supposed to match.
     */
    public @NonNull Term getType();

    /**
     * Get the analyser used with this classification.
     *
     * @return The analyser
     */
    public @NonNull Analyser<C> getAnalyser();

    /**
     * Get any issues recorded with this classification.
     * <p>
     * The analyser in {@link #getAnalyser()} may addf issues as required.
     * </p>
     *
     * @return Any issues associated with the classification/
     */
    public @NonNull Issues getIssues();

    /**
     * Get a list of observations that match this classification.
     * <p>
     * This can be used to query an underlying name matcher for candidiate matches.
     * </p>
     *
     * @return This classification as a list of observations
     */
    public Collection<Observation> toObservations();

    /**
     * Infer empty elements of the classification from the network definition.
     * <p>
     * This method is usually generated to implement any derivations that are
     * specified by {@link Observable#getDerivation()}.
     * The method can be used to perform common derivations without requiring further coding.
     * </p>
     *
     * @throws InferenceException if unable to calculate an inferred value
     * @throws StoreException if unable to retrieve a source value
     */
    public void infer() throws InferenceException, StoreException;

    /**
     *
     * Populate this classification from a classifier.
     * <p>
     * This allows a classification to be built that matches the classifier.
     * </p>
     * @param classifier The classifier that contains the original data
     * @param overwrite Overwrite what is already in the classification
     *
     * @throws InferenceException
     */
    public void populate(Classifier classifier, boolean overwrite) throws InferenceException;

    /**
     * Translate this classification into a classifier.
     *
     * @param classifier The empty classifier to populate
     *
     * @throws InferenceException if unable to translate
     * @throws StoreException if unable to store the translation
     */
    public void translate(Classifier classifier) throws InferenceException, StoreException;
}
