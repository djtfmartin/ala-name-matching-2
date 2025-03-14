package au.org.ala.bayesian;

import java.util.Optional;
import java.util.Set;

/**
 * Provides application-specific analysis and extension of evidence supplied.
 * <p>
 * Subclasses can be used to fill out inferred evidence that doesn't
 * fit nicely into a network.
 * For example, it can be used to work out implied rank from a scientific name,
 * if one has not been specified.
 * </p>
 *
 */
public interface Analyser<C extends Classification> {
    /**
     * Analyse the information in a classification and extend the classification
     * as required in preparation for building an index.
     * <p>
     * Generally, if there is a recoverable problem here, fix it.
     * And if the name looks weird, then that's what the source says.
     * </p>
     *
     * @param classification The classification
     *
     * @throws InferenceException if an error occurs during inference
     */
    public void analyseForIndex(C classification) throws InferenceException;

    /**
     * Analyse the information in a classification and extend the classification
     * as required in preparation for search/inference.
     *
     * @param classification The classification
     *
     * @throws InferenceException if an error occurs during inference
     */
    public void analyseForSearch(C classification) throws InferenceException;

    /**
     * Build a collection of base names for the classification.
     * <p>
     * If a classification can be referred to in multiple ways, this method
     * builds the various ways of referring to the classification.
     * </p>
     * <p>
     * As a general principle, if {@link #analyseForIndex(Classification)} and
     * {@link #analyseForSearch(Classification)} would produce different results,
     * this method should ensure that whatever is in the search is included in the
     * set of names.
     * </p>
     *
     * @param classifier The classification
     * @param name The observable that gives the name
     * @param complete The observable that gives the complete name
     * @param additional The observable that gives additional disambiguation, geneerally complete = name + ' ' + additional
     * @param canonical Only include canonical names
     *
     * @return All the names that refer to the classification
     *
     * @throws InferenceException if unable to analyuse the names
     */
    public Set<String> analyseNames(Classifier classifier, Observable name, Optional<Observable> complete, Optional<Observable> additional, boolean canonical) throws InferenceException;
}
