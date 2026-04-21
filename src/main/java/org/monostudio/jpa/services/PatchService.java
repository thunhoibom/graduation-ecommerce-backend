package org.monostudio.jpa.services;

import org.monostudio.common.exceptions.BadInputException;

import java.util.Map;

/**
 * Type-safe interface for partially modifying instances of entity classses.
 *
 * @param <P> The Pojo class containing changes
 * @param <E> The Entity class
 */
public interface PatchService<P, E> {

    /**
     * Creates a clone of the provided Entity and updates it using the key/value pairs in the provided Map.<br/>
     *
     * @param changes  A map containing keys and values to update with.
     * @param existing The target entity.
     * @return An updated instance of the @Entity, prepared to be saved to the database.
     * @throws BadInputException If the object with changes has invalid values
     */
    E patchExistingEntity(Map<String, Object> changes, E existing) throws BadInputException;

    /**
     * Creates a clone Entity, then updates it with new data from a Pojo,
     * setting differences in properties one-by-one, and returns it.
     * It does not include relationships to other entities.
     *
     * @param changes  The Pojo containing data updates.
     * @param existing The target entity.
     * @return An updated instance of the @Entity, prepared to be saved to the database.
     * @throws BadInputException If the object with changes has invalid values
     * @deprecated Use {@link #patchExistingEntity(Map, Object)} instead where possible.
     */
    @Deprecated(since = "0.2.0-SNAPSHOT", forRemoval = true)
    E patchExistingEntity(P changes, E existing) throws BadInputException;
}
