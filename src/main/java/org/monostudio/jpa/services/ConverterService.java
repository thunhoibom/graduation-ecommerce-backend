package org.monostudio.jpa.services;

import org.monostudio.common.exceptions.BadInputException;

/**
 * Type-safe interface for converting Entities to Pojos and viceversa
 *
 * @param <P> The Pojo class
 * @param <E> The Entity class
 */
public interface ConverterService<P, E> {

    /**
     * Straightly converts a Pojo to a new <code>@Entity</code>, assuming that the Pojo is already <code>@Valid</code>.
     * <b>This includes relationships to other entities</b>.<br/>
     * This method DOES NOT persist data.
     *
     * @param source The source Pojo.
     * @return A new entity, prepared to be saved to the database.
     * @throws BadInputException If the source object does not include required data or has invalid values
     */
    E convertToNewEntity(P source) throws BadInputException;

    /**
     * Creates a clone @Entity, then updates it with new data from a Pojo class,
     * putting differences in properties one-by-one. It does not include relationships to other entities.
     * This method DOES NOT persist data.
     *
     * @param source The Pojo containing data updates.
     * @param target The target entity.
     * @return An updated instance of the @Entity, prepared to be saved to the database.
     * @deprecated Please use {@link PatchService}'s method instead
     */
    @Deprecated(forRemoval = true, since = "0.2.0-SNAPSHOT")
    E applyChangesToExistingEntity(P source, E target);

    /**
     * Converts an existing @Entity to its Pojo equivalent, but only including one-to-one relationships to other entities.
     *
     * @param source The source @Entity.
     * @return The resulting Pojo, or null if the @Entity isn't persistent
     */
    P convertToPojo(E source);
}
