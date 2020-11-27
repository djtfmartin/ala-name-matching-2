package au.org.ala.bayesian;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A modification that can be applied to a classification.
 * <p>
 * Essentially, a modification allows a classification to be changed in some way
 * so that a variation of the classification can be tried.
 * </p>
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class Modifier extends Identifiable {
    /** Any issue associated with this modifier */
    @JsonProperty
    @Getter
    @Setter
    private URI issue;

    /**
     * Construct an empty identifiable object with an arbitrary identifier.
     */
    public Modifier() {
        super();
    }

    /**
     * Construct for a supplied identifier and URI.
     *
     * @param id  The identifier
     * @param uri The URI (may be null)
     */
    public Modifier(String id, URI uri) {
        super(id, uri);
    }

    /**
     * Construct with a defined identifier.
     *
     * @param id The identifier
     */
    public Modifier(String id) {
        super(id);
    }

    /**
     * Construct with a defined URI.
     *
     * @param uri The uri
     */
    public Modifier(URI uri) {
        super(uri);
    }

    /**
     * The base set of observables this modification will alter.
     * <p>
     * This set does not include any derived observations and can, therefore
     * be
     * </p>
     * @return
     */
    @JsonIgnore
    abstract public Set<Observable> getModified();


    /**
     * The set of and derived observables this modification will alter
     * indirectly via changing the base variables.
     *
     * @param compiled The compiled network to use
     *
     * @return The list of modified observables
     */
    @JsonIgnore
    public Set<Observable> getDependents(NetworkCompiler compiled) {
        Set<Observable> base = this.getModified();
        Set<Observable> modified = compiled.getOrderedNodes().stream()
            .map(NetworkCompiler.Node::getObservable)
            .filter(o -> o.getDerivation() != null && o.getDerivation().getInputs().stream().anyMatch(i -> base.contains(i)))
            .collect(Collectors.toSet());
        return modified;
    }

    /**
     * Generate code that will perform the modification.
     *
     * @param compiler The compiled network
     * @param from The name of the variable that holds the classification to modify
     * @param to The name of the variable that holds the resulting classification
     *
     * @return A list of statements needed to modify the classification
     */
    abstract public List<String> generate(NetworkCompiler compiler, String from, String to);

    /**
     * Clear any dependent values.
     * <p>
     * A utility method for use with {@link #generate(NetworkCompiler, String, String)}
     * </p>
     *
     * @param compiler The compiled network
     * @param var The variable name for the classification instance
     * @param statements The list of statenments to add to
     */
    protected void nullDependents(NetworkCompiler compiler, String var, List<String> statements) {
        for (Observable observable: this.getDependents(compiler))
            statements.add(var + "." + observable.getJavaVariable() + " = null;");
    }
}
