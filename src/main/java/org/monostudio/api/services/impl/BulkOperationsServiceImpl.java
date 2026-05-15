package org.monostudio.api.services.impl;

import org.monostudio.api.models.BulkOperationResult;
import org.monostudio.api.services.BulkOperationsService;
import org.monostudio.jpa.services.crud.ProductsCrudService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductStatus;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.search.kafka.IndexEventProducer;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BulkOperationsServiceImpl implements BulkOperationsService {

    private final ProductsRepository productsRepository;
    private final ProductsCrudService productsCrudService;
    private final IndexEventProducer indexEventProducer;

    public BulkOperationsServiceImpl(
        ProductsRepository productsRepository,
        ProductsCrudService productsCrudService,
        IndexEventProducer indexEventProducer
    ) {
        this.productsRepository = productsRepository;
        this.productsCrudService = productsCrudService;
        this.indexEventProducer = indexEventProducer;
    }

    @Override
    @Transactional
    public BulkOperationResult bulkPublish(List<Long> ids) throws BadInputException {
        return bulkUpdateStatus(ids, ProductStatus.PUBLISHED);
    }

    @Override
    @Transactional
    public BulkOperationResult bulkUnpublish(List<Long> ids) throws BadInputException {
        return bulkUpdateStatus(ids, ProductStatus.UNLISTED);
    }

    private BulkOperationResult bulkUpdateStatus(List<Long> ids, ProductStatus newStatus)
        throws BadInputException {
        validateIds(ids);

        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long id : ids) {
            try {
                Product product = productsRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
                product.setStatus(newStatus);
                productsRepository.save(product);
                indexEventProducer.sendIndexEvent("PRODUCT", id, "UPDATE");
                successCount++;
            } catch (EntityNotFoundException e) {
                errors.add("ID " + id + ": " + e.getMessage());
            } catch (Exception e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }

        return BulkOperationResult.builder()
            .successCount(successCount)
            .errorCount(errors.size())
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }

    @Override
    @Transactional
    public BulkOperationResult bulkDelete(List<Long> ids) throws BadInputException {
        validateIds(ids);

        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long id : ids) {
            try {
                productsCrudService.delete(id);
                successCount++;
            } catch (EntityNotFoundException e) {
                errors.add("ID " + id + ": " + e.getMessage());
            } catch (Exception e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }

        return BulkOperationResult.builder()
            .successCount(successCount)
            .errorCount(errors.size())
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }

    private void validateIds(List<Long> ids) throws BadInputException {
        if (ids == null || ids.isEmpty()) {
            throw new BadInputException("ID list cannot be empty");
        }
    }
}
