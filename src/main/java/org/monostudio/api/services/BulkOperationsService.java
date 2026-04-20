package org.monostudio.api.services;

import org.monostudio.api.models.BulkOperationResult;
import org.monostudio.common.exceptions.BadInputException;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;

/**
 * Service for bulk operations on entities.
 * Supports bulk publish/unpublish/delete for products.
 */
public interface BulkOperationsService {

    /**
     * Publish multiple products at once.
     *
     * @param ids Product IDs to publish
     * @return Result with success/error counts
     * @throws BadInputException When ids list is empty
     */
    BulkOperationResult bulkPublish(List<Long> ids)
        throws BadInputException, EntityNotFoundException;

    /**
     * Unpublish multiple products at once.
     *
     * @param ids Product IDs to unpublish
     * @return Result with success/error counts
     * @throws BadInputException When ids list is empty
     */
    BulkOperationResult bulkUnpublish(List<Long> ids)
        throws BadInputException, EntityNotFoundException;

    /**
     * Delete multiple products at once.
     *
     * @param ids Product IDs to delete
     * @return Result with success/error counts
     * @throws BadInputException When ids list is empty
     */
    BulkOperationResult bulkDelete(List<Long> ids)
        throws BadInputException, EntityNotFoundException;
}
