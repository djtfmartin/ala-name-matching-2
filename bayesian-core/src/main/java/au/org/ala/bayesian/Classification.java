package au.org.ala.bayesian;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.gbif.dwc.terms.Term;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The base class for classifications.
 * <p>
 * Classifications hold the vector of evidence that make up a individual piece of data.
 * </p>
 */
public interface Classification<C extends Classification<C>> extends Cloneable {
    /**
     * Create a clone of this classification.
     *
     * @return The cloned classification
     */
    @NonNull C clone();
    /**
     * Get the type of the classification
     *
     * @return The sort of thing this classification is supposed to match.
     */
    @NonNull Term getType();

    /**
     * Get the analyser used with this classification.
     *
     * @return The analyser
     */
    @NonNull Analyser<C> getAnalyser();

    /**
     * Get the identifier of this classification
     *
     * @return The identifier
     */
    String getIdentifier();

    /**
     * Get the parent identifier of this classification
     *
     * @return The parent identifier or null for none
     */
    String getParent();

    /**
     * Get the accepted value of this classification
     *
     * @return The accepted identifier or null for none
     */
    String getAccepted();

    /**
     * Get the base name of this classification
     *
     * @return The name
     */
    String getName();

    /**
     * Get any issues recorded with this classification.
     * <p>
     * The analyser in {@link #getAnalyser()} may addf issues as required.
     * </p>
     *
     * @return Any issues associated with the classification/
     */
    @NonNull Issues getIssues();

    /**
     * Add an issue to the issues list.
     * <p>
     * Adding an issue should apply to the classification itself.
     * Shared issues lists need to be disambigauted before being modified.
     * </p>
     *
     * @param issue The issue to add
     */
    void addIssue(Term issue);

    /**
     * Get a list of observations that match this classification.
     * <p>
     * This can be used to query an underlying name matcher for candidiate matches.
     * </p>
     *
     * @return This classification as a list of observations
     */
    Collection<Observation> toObservations();

    /**
     * Infer empty elements of the classification from the network definition
     * in preparation for indexing.
     * <p>
     * This method is usually generated to implement any derivations that are
     * specified by {@link Observable#getDerivation()}.
     * The method can be used to perform common derivations without requiring further coding.
     * </p>
     *
     * @throws InferenceException if unable to calculate an inferred value
     * @throws StoreException if unable to retrieve a source value
     */
    void inferForIndex() throws InferenceException, StoreException;


    /**
     * Infer empty elements of the classification from the network definition
     * in preparation for using this as a search template.
     * <p>
     * This method is usually generated to implement any derivations that are
     * specified by {@link Observable#getDerivation()}.
     * The method can be used to perform common derivations without requiring further coding.
     * </p>
     *
     * @throws InferenceException if unable to calculate an inferred value
     * @throws StoreException if unable to retrieve a source value
     */
    void inferForSearch() throws InferenceException, StoreException;

    /**
     *
     * Read this classification from a classifier.
     * <p>
     * This allows a classification to be built that matches the classifier.
     * </p>
     * @param classifier The classifier that contains the original data
     * @param overwrite Overwrite what is already in the classification
     *
     * @throws InferenceException if unable to compute the population value
     * @throws StoreException if unable to populate the classifier
     */
    void read(Classifier classifier, boolean overwrite) throws InferenceException, StoreException;

    /**
     * Write this classification into a classifier.
     *
     * @param classifier The empty classifier to populate
     * @param overwrite Overwrite what is already in the classifier
     *
     * @throws InferenceException if unable to translate
     * @throws StoreException if unable to store the translation
     */
    void write(Classifier classifier, boolean overwrite) throws InferenceException, StoreException;


    /**
     * The order in which to modify this classification when attempting source searches.
     * <p>
     * Source modifications assume that the search for possible candidates is needs to be done again.
     * </p>
     * <p>
     * Returned is a list of functions that will take a classification and return
     * a modified classification
     * </p>
     * @return
     */
    public List<List<Function<C, C>>> searchModificationOrder();

    /**
     * The order in which to modify this classification when attempting matches.
     * <p>
     * Match modifications assume that the search for possible candidates is acceptable and
     * need not be done again.
     * </p>
     * <p>
     * Returned is a list of functions that will take a classification and return
     * a modified classification
     * </p>
     * @return
     */
    public List<List<Function<C, C>>> matchModificationOrder();
}
