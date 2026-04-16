package org.monostudio.api;

import org.monostudio.api.models.DataPagePojo;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * {@link org.springframework.web.bind.annotation.RestController} that handles requests for reading data from a persistence context.
 *
 * @param <M> Its signature model class
 */
public interface DataController<M> {

    /**
     * Get a paged collection of data.
     *
     * @param requestParams A {@link java.util.Map} of key/value String pairs containing the parameters for reading the data.
     * @return An instance of {@link org.monostudio.api.models.DataPagePojo} containing the data itself, and information about that page of data.
     */
    DataPagePojo<M> readMany(@NotNull Map<String, String> requestParams);

    /**
     * Get a single item by its id.
     *
     * @param id The id of the item to retrieve.
     * @return The item.
     * @throws EntityNotFoundException When no item matches the given id.
     */
    M getById(Long id) throws EntityNotFoundException;
}
