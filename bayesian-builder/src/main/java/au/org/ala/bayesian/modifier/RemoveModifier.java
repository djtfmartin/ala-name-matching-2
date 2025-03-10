package au.org.ala.bayesian.modifier;

import au.org.ala.bayesian.NetworkCompiler;
import au.org.ala.bayesian.Observable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Remove the value of an entry
 */
public class RemoveModifier extends BaseModifier {
    protected RemoveModifier() {
    }

    public RemoveModifier(String id, Collection<Observable> observables, boolean nullDerived) {
        super(id, observables, nullDerived);
    }

    /**
     * The set of observables that need to be present for this modification to succeed.
     *
     * @return The set of observavles that need to be present
     * @see #getAnyCondition()
     */
    @Override
    public Collection<Observable> getConditions() {
        return this.getModified();
    }

    /**
     * Can we proceed with any conditonal variable being true or must we have all of them?
     *
     * @return True if any variable being present allows the modifier
     */
    @Override
    public boolean getAnyCondition() {
        return true;
    }

    /**
     * Generate code that will perform the modification.
     *
     * @param compiler The source network
     * @param from The name of the variable that holds the classification to modify
     * @param to   The name of the variable that holds the resulting classification
     * @return The code needed to modify the classification
     */
    @Override
    public List<String> generate(NetworkCompiler compiler, String from, String to) {
        List<String> statements = new ArrayList<>();
        this.checkModifiable(compiler, from, statements);
        statements.add(to + " = " + from + ".clone();");
        for (Observable observable: this.getModified())
            statements.add(to + "." + observable.getJavaVariable() + " = null;");
        if (this.isNullDerived())
            this.nullDependents(compiler, to, statements);
        return statements;
    }
}
