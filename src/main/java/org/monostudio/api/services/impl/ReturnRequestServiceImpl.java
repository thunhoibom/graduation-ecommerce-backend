package org.monostudio.api.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ReturnRequestItemPojo;
import org.monostudio.api.models.ReturnRequestPojo;
import org.monostudio.api.services.ReturnRequestService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.ReturnRequest;
import org.monostudio.jpa.entities.ReturnRequestItem;
import org.monostudio.jpa.entities.StockAdjustment;
import org.monostudio.jpa.repositories.ProductVariantsRepository;
import org.monostudio.jpa.repositories.ReturnRequestItemsRepository;
import org.monostudio.jpa.repositories.ReturnRequestsRepository;
import org.monostudio.jpa.services.conversion.ReturnRequestsConverterService;
import org.monostudio.jpa.services.crud.ReturnRequestsCrudService;
import org.monostudio.api.services.StockAdjustmentService;
import org.monostudio.mailing.MailingService;
import org.monostudio.mailing.MailingServiceException;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Transactional
@Service
public class ReturnRequestServiceImpl
    implements ReturnRequestService {
    private static final Logger logger = LoggerFactory.getLogger(ReturnRequestServiceImpl.class);
    private static final String INVALID_STATE = "The return request is not in a valid state for this operation";
    private final ReturnRequestsRepository returnRequestsRepository;
    private final ReturnRequestItemsRepository itemsRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final ReturnRequestsCrudService crudService;
    private final ReturnRequestsConverterService converterService;
    private final StockAdjustmentService stockAdjustmentService;
    private final MailingService mailingService;

    @Autowired
    public ReturnRequestServiceImpl(
        ReturnRequestsRepository returnRequestsRepository,
        ReturnRequestItemsRepository itemsRepository,
        ProductVariantsRepository productVariantsRepository,
        ReturnRequestsCrudService crudService,
        ReturnRequestsConverterService converterService,
        StockAdjustmentService stockAdjustmentService,
        MailingService mailingService
    ) {
        this.returnRequestsRepository = returnRequestsRepository;
        this.itemsRepository = itemsRepository;
        this.productVariantsRepository = productVariantsRepository;
        this.crudService = crudService;
        this.converterService = converterService;
        this.stockAdjustmentService = stockAdjustmentService;
        this.mailingService = mailingService;
    }

    @Override
    public ReturnRequestPojo createReturnRequest(ReturnRequestPojo input) throws BadInputException {
        if (input.getStatus() == null) {
            input.setStatus(ReturnRequest.ReturnRequestStatus.PENDING.name());
        }
        ReturnRequestPojo result = crudService.create(input);
        // Notify store owners of the new return request
        try {
            mailingService.notifyReturnRequestToOwners(result);
        } catch (MailingServiceException e) {
            logger.warn("Failed to send return request notification: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public ReturnRequestPojo approveReturnRequest(Long id, String adminNotes, Integer refundAmount)
        throws EntityNotFoundException, BadInputException {
        ReturnRequest existing = returnRequestsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Return request not found: " + id));

        if (existing.getStatus() != ReturnRequest.ReturnRequestStatus.PENDING) {
            throw new BadInputException(INVALID_STATE);
        }

        // Release reserved stock for each item
        List<ReturnRequestItem> items = itemsRepository.findByReturnRequestId(id);
        for (ReturnRequestItem item : items) {
            restoreVariantStock(item);
        }

        // Update status to APPROVED
        existing.setStatus(ReturnRequest.ReturnRequestStatus.APPROVED);
        if (adminNotes != null) {
            existing.setAdminNotes(adminNotes);
        }
        if (refundAmount != null) {
            existing.setRefundAmount(refundAmount);
        }

        ReturnRequest saved = returnRequestsRepository.saveAndFlush(existing);
        ReturnRequestPojo pojo = buildReturnRequestPojo(saved, items);

        // Notify customer of approval
        try {
            mailingService.notifyReturnRequestStatusToClient(pojo);
        } catch (MailingServiceException e) {
            logger.warn("Failed to send return approval notification: {}", e.getMessage());
        }

        return pojo;
    }

    @Override
    public ReturnRequestPojo rejectReturnRequest(Long id, String adminNotes)
        throws EntityNotFoundException, BadInputException {
        ReturnRequest existing = returnRequestsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Return request not found: " + id));

        if (existing.getStatus() != ReturnRequest.ReturnRequestStatus.PENDING) {
            throw new BadInputException(INVALID_STATE);
        }

        existing.setStatus(ReturnRequest.ReturnRequestStatus.REJECTED);
        if (adminNotes != null) {
            existing.setAdminNotes(adminNotes);
        }

        ReturnRequest saved = returnRequestsRepository.saveAndFlush(existing);
        List<ReturnRequestItem> items = itemsRepository.findByReturnRequestId(id);
        ReturnRequestPojo pojo = buildReturnRequestPojo(saved, items);
        try {
            mailingService.notifyReturnRequestStatusToClient(pojo);
        } catch (MailingServiceException e) {
            logger.warn("Failed to send return rejection notification: {}", e.getMessage());
        }
        return pojo;
    }

    @Override
    public ReturnRequestPojo markAsReceived(Long id, String adminNotes)
        throws EntityNotFoundException, BadInputException {
        ReturnRequest existing = returnRequestsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Return request not found: " + id));

        if (existing.getStatus() != ReturnRequest.ReturnRequestStatus.APPROVED
            && existing.getStatus() != ReturnRequest.ReturnRequestStatus.REFUND_PROCESSING) {
            throw new BadInputException(INVALID_STATE);
        }

        existing.setStatus(ReturnRequest.ReturnRequestStatus.RECEIVED);
        if (adminNotes != null) {
            existing.setAdminNotes(adminNotes);
        }

        ReturnRequest saved = returnRequestsRepository.saveAndFlush(existing);
        List<ReturnRequestItem> items = itemsRepository.findByReturnRequestId(id);
        ReturnRequestPojo pojo = buildReturnRequestPojo(saved, items);
        try {
            mailingService.notifyReturnRequestStatusToClient(pojo);
        } catch (MailingServiceException e) {
            logger.warn("Failed to send return received notification: {}", e.getMessage());
        }
        return pojo;
    }

    @Override
    public ReturnRequestPojo completeRefund(Long id, String adminNotes)
        throws EntityNotFoundException, BadInputException {
        ReturnRequest existing = returnRequestsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Return request not found: " + id));

        if (existing.getStatus() != ReturnRequest.ReturnRequestStatus.RECEIVED
            && existing.getStatus() != ReturnRequest.ReturnRequestStatus.REFUND_PROCESSING) {
            throw new BadInputException(INVALID_STATE);
        }

        existing.setStatus(ReturnRequest.ReturnRequestStatus.REFUND_COMPLETED);
        if (adminNotes != null) {
            existing.setAdminNotes(adminNotes);
        }

        ReturnRequest saved = returnRequestsRepository.saveAndFlush(existing);
        List<ReturnRequestItem> items = itemsRepository.findByReturnRequestId(id);
        ReturnRequestPojo pojo = buildReturnRequestPojo(saved, items);
        try {
            mailingService.notifyReturnRequestStatusToClient(pojo);
        } catch (MailingServiceException e) {
            logger.warn("Failed to send refund complete notification: {}", e.getMessage());
        }
        return pojo;
    }

    @Override
    public ReturnRequestPojo addTrackingNumber(Long id, String trackingNumber)
        throws EntityNotFoundException, BadInputException {
        ReturnRequest existing = returnRequestsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Return request not found: " + id));

        if (existing.getStatus() == ReturnRequest.ReturnRequestStatus.REFUND_COMPLETED
            || existing.getStatus() == ReturnRequest.ReturnRequestStatus.CANCELLED
            || existing.getStatus() == ReturnRequest.ReturnRequestStatus.REJECTED) {
            throw new BadInputException(INVALID_STATE);
        }

        existing.setTrackingNumber(trackingNumber);
        ReturnRequest saved = returnRequestsRepository.saveAndFlush(existing);
        List<ReturnRequestItem> items = itemsRepository.findByReturnRequestId(id);
        return buildReturnRequestPojo(saved, items);
    }

    @Override
    public ReturnRequestPojo cancelReturnRequest(Long id)
        throws EntityNotFoundException, BadInputException {
        ReturnRequest existing = returnRequestsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Return request not found: " + id));

        if (existing.getStatus() != ReturnRequest.ReturnRequestStatus.PENDING) {
            throw new BadInputException(INVALID_STATE);
        }

        existing.setStatus(ReturnRequest.ReturnRequestStatus.CANCELLED);
        ReturnRequest saved = returnRequestsRepository.saveAndFlush(existing);
        List<ReturnRequestItem> items = itemsRepository.findByReturnRequestId(id);
        return buildReturnRequestPojo(saved, items);
    }

    /**
     * Restores stock to the ProductVariant when a return is approved.
     * The ReturnRequestItem carries a productId; we look up active variants
     * belonging to that product and restore stock to each.
     */
    private void restoreVariantStock(ReturnRequestItem item) {
        if (item.getProduct() == null || item.getProduct().getId() == null) {
            return;
        }
        List<ProductVariant> variants = productVariantsRepository.findByProductId(item.getProduct().getId());
        for (ProductVariant variant : variants) {
            if (variant.isActive()) {
                variant.setStockCurrent(variant.getStockCurrent() + item.getQuantity());
                productVariantsRepository.saveAndFlush(variant);
            }
        }
    }

    private ReturnRequestPojo buildReturnRequestPojo(ReturnRequest entity, List<ReturnRequestItem> items) {
        ReturnRequestPojo target = converterService.convertToPojo(entity);
        List<ReturnRequestItemPojo> itemPojos = items.stream()
            .map(converterService::convertItemToPojo)
            .collect(Collectors.toList());
        target.setItems(itemPojos);
        return target;
    }
}
