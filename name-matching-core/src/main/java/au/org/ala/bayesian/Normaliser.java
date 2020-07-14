package au.org.ala.bayesian;

import au.org.ala.names.model.Identifiable;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 * Normalise a string in a consistent way.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
abstract public class Normaliser extends Identifiable {
    /**
     * Construct an empty normaliser with an arbitrary identifier.
     */
    public Normaliser() {
    }

    /**
     * Construct with a defined identifier.
     *
     * @param id The identifier
     */
    public Normaliser(String id) {
        super(id);
    }

    /**
     * Normalise a string.
     * <p>
     * If the string is null or empty, the normaliser should return null.
     * </p>
     *
     * @param s The string to normalise, may be null
     *
     * @return The normalised string
     */
    abstract public String normalise(String s);

    /**
     * Generate the code to create this normaliser
     *
     * @return normaliser code
     */
    abstract public String getCreator();
}
