package org.monostudio.jpa.services.crud.impl;

import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ReturnRequestItemPojo;
import org.monostudio.api.models.ReturnRequestPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ReturnRequest;
import org.monostudio.jpa.entities.ReturnRequestItem;
import org.monostudio.jpa.repositories.ReturnRequestItemsRepository;
import org.monostudio.jpa.repositories.ReturnRequestsRepository;
import org.monostudio.jpa.services.conversion.ReturnRequestsConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.ReturnRequestsCrudService;
import org.monostudio.jpa.services.patch.ReturnRequestsPatchService;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Service
public class ReturnRequestsCrudServiceImpl
    extends CrudGenericService<ReturnRequestPojo, ReturnRequest>
    implements ReturnRequestsCrudService {
    private final ReturnRequestsRepository returnRequestsRepository;
    private final ReturnRequestsConverterService converterService;
    private final ReturnRequestsPatchService patchService;
    private final ReturnRequestItemsRepository itemsRepository;

    @Autowired
    public ReturnRequestsCrudServiceImpl(
        ReturnRequestsRepository returnRequestsRepository,
        ReturnRequestsConverterService converterService,
        ReturnRequestsPatchService patchService,
        ReturnRequestItemsRepository itemsRepository
    ) {
        super(returnRequestsRepository, converterService, patchService);
        this.returnRequestsRepository = returnRequestsRepository;
        this.converterService = converterService;
        this.patchService = patchService;
        this.itemsRepository = itemsRepository;
    }

    @Override
    public Optional<ReturnRequest> getExisting(ReturnRequestPojo input) throws BadInputException {
        if (input.getId() != null) {
            return returnRequestsRepository.findById(input.getId());
        }
        if (input.getOrderId() != null) {
            return returnRequestsRepository.findByOrderId(input.getOrderId());
        }
        return Optional.empty();
    }

    @Override
    public ReturnRequestPojo readOne(Predicate conditions) throws EntityNotFoundException {
        Optional<ReturnRequest> match = returnRequestsRepository.findOne(conditions);
        if (match.isEmpty()) {
            throw new EntityNotFoundException(ITEM_NOT_FOUND);
        }
        ReturnRequest found = match.get();
        ReturnRequestPojo target = converterService.convertToPojo(found);

        List<ReturnRequestItem> items = itemsRepository.findByReturnRequestId(found.getId());
        List<ReturnRequestItemPojo> itemPojos = items.stream()
            .map(converterService::convertItemToPojo)
            .collect(Collectors.toList());
        target.setItems(itemPojos);

        return target;
    }

    @Override
    public ReturnRequestPojo create(ReturnRequestPojo input) throws BadInputException {
        validateInputPojoBeforeCreation(input);
        ReturnRequest entity = converterService.convertToNewEntity(input);
        entity = returnRequestsRepository.saveAndFlush(entity);

        if (input.getItems() != null && !input.getItems().isEmpty()) {
            List<ReturnRequestItem> itemEntities = new ArrayList<>();
            for (ReturnRequestItemPojo itemPojo : input.getItems()) {
                ReturnRequestItem itemEntity = new ReturnRequestItem();
                itemEntity.setQuantity(itemPojo.getQuantity());
                itemEntity.setReason(itemPojo.getReason());
                itemEntity.setActive(true);
                itemEntity.setReturnRequest(entity);
                if (itemPojo.getVariantId() != null) {
                    org.monostudio.jpa.entities.ProductVariant variant =
                        new org.monostudio.jpa.entities.ProductVariant();
                    variant.setId(itemPojo.getVariantId());
                    itemEntity.setVariant(variant);
                } else if (itemPojo.getProductId() != null) {
                    org.monostudio.jpa.entities.Product product =
                        new org.monostudio.jpa.entities.Product();
                    product.setId(itemPojo.getProductId());
                    itemEntity.setProduct(product);
                }
                itemEntities.add(itemEntity);
            }
            itemsRepository.saveAll(itemEntities);
            // Reload items via repository to ensure they are in the persistence context
            List<ReturnRequestItem> savedItems = itemsRepository.findByReturnRequestId(entity.getId());
            List<ReturnRequestItemPojo> savedItemPojos = savedItems.stream()
                .map(converterService::convertItemToPojo)
                .collect(Collectors.toList());
            ReturnRequestPojo result = converterService.convertToPojo(entity);
            result.setItems(savedItemPojos);
            return result;
        }

        ReturnRequestPojo result = converterService.convertToPojo(entity);
        result.setItems(List.of());
        return result;
    }

    @Override
    protected ReturnRequest flushPartialChanges(Map<String, Object> changes, ReturnRequest existingEntity) throws BadInputException {
        ReturnRequest updatedEntity = patchService.patchExistingEntity(changes, existingEntity);
        return returnRequestsRepository.saveAndFlush(updatedEntity);
    }
}
