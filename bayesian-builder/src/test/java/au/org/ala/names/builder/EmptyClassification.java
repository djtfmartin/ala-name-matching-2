package au.org.ala.names.builder;

import au.org.ala.bayesian.*;
import lombok.Getter;
import lombok.NonNull;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * An empty classification for simple tests.
 */
public class EmptyClassification implements Classification<EmptyClassification> {
    @Getter
    private NullAnalyser<EmptyClassification> analyser = new NullAnalyser<>();
    @Getter
    private Issues issues = new Issues();

    @Override
    public @NonNull Term getType() {
        return DwcTerm.Taxon;
    }

    @Override
    public Collection<Observation> toObservations() {
        return Collections.emptyList();
    }

    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public String getParent() {
        return null;
    }

    @Override
    public String getAccepted() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void inferForIndex() {
    }

    @Override
    public void inferForSearch() {
    }

    /**
     * Create a clone of this classification.
     *
     * @return The cloned classification
     */
    @Override
    public @NonNull EmptyClassification clone() {
        return (EmptyClassification) this.clone();
    }

    /**
     * Add an issue to the issues list.
     * <p>
     * Adding an issue should apply to the classification itself.
     * Shared issues lists need to be disambigauted before being modified.
     * </p>
     *
     * @param issue The issue to add
     */
    @Override
    public void addIssue(Term issue) {
    }


    /**
     * The order in which to modify this classification.
     * <p>
     * Returned is a list of functions that will take a classification and return
     * a modified classification
     * </p>
     *
     * @return
     */
    @Override
    public List<List<Function<EmptyClassification, EmptyClassification>>> searchModificationOrder() {
        return Collections.emptyList();
    }

    /**
     * The order in which to modify this classification.
     * <p>
     * Returned is a list of functions that will take a classification and return
     * a modified classification
     * </p>
     *
     * @return
     */
    @Override
    public List<List<Function<EmptyClassification, EmptyClassification>>> matchModificationOrder() {
        return Collections.emptyList();
    }

    @Override
    public void read(Classifier classifier, boolean overwrite) {
    }

    @Override
    public void write(Classifier classifier, boolean overwrite) {
    }
}
