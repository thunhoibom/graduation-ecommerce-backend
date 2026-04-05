package org.monostudio.api.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.RefundResultPojo;
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
import org.monostudio.payment.PaymentService;
import org.monostudio.payment.PaymentServiceException;

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
    private final PaymentService paymentIntegrationService;

    @Autowired
    public ReturnRequestServiceImpl(
        ReturnRequestsRepository returnRequestsRepository,
        ReturnRequestItemsRepository itemsRepository,
        ProductVariantsRepository productVariantsRepository,
        ReturnRequestsCrudService crudService,
        ReturnRequestsConverterService converterService,
        StockAdjustmentService stockAdjustmentService,
        MailingService mailingService,
        PaymentService paymentIntegrationService
    ) {
        this.returnRequestsRepository = returnRequestsRepository;
        this.itemsRepository = itemsRepository;
        this.productVariantsRepository = productVariantsRepository;
        this.crudService = crudService;
        this.converterService = converterService;
        this.stockAdjustmentService = stockAdjustmentService;
        this.mailingService = mailingService;
        this.paymentIntegrationService = paymentIntegrationService;
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

        List<ReturnRequestItem> items = itemsRepository.findByReturnRequestId(id);

        // Update status to APPROVED — stock restore is done in markAsReceived, not here.
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

        List<ReturnRequestItem> items = itemsRepository.findByReturnRequestId(id);

        // Restore stock to the specific variant — done when warehouse confirms receipt of returned goods.
        for (ReturnRequestItem item : items) {
            restoreVariantStock(item);
        }

        existing.setStatus(ReturnRequest.ReturnRequestStatus.RECEIVED);
        if (adminNotes != null) {
            existing.setAdminNotes(adminNotes);
        }

        ReturnRequest saved = returnRequestsRepository.saveAndFlush(existing);
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

        // Call the payment gateway refund API if the original transaction token is available.
        Integer refundAmount = existing.getRefundAmount();
        String token = existing.getOrder() != null ? existing.getOrder().getTransactionToken() : null;
        if (token != null && refundAmount != null && refundAmount > 0) {
            try {
                RefundResultPojo result = paymentIntegrationService.refund(token, refundAmount);
                if (result.isSuccess()) {
                    logger.info("Refund succeeded for return {}: type={}, code={}",
                        id, result.getType(), result.getResponseCode());
                } else {
                    logger.warn("Refund rejected by gateway for return {}: code={}",
                        id, result.getResponseCode());
                }
            } catch (PaymentServiceException e) {
                // Log but do not block — admin must handle manually if refund gateway fails.
                logger.error("Refund gateway error for return {}: {}", id, e.getMessage());
            }
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
     * Restores stock when returned goods are received at the warehouse.
     *
     * Priority: restore to the exact variant if available (variantId field is set).
     * Fallback: restore to all active variants of the product (legacy behavior).
     *
     * Stock is restored ONLY at RECEIVED, not at approval — store must physically
     * receive and inspect the goods before returning them to sellable inventory.
     */
    private void restoreVariantStock(ReturnRequestItem item) {
        // Preferred path: restore to the exact variant that was purchased
        if (item.getVariant() != null && item.getVariant().getId() != null) {
            ProductVariant variant = productVariantsRepository.getById(item.getVariant().getId());
            if (variant != null) {
                variant.setStockCurrent(variant.getStockCurrent() + item.getQuantity());
                productVariantsRepository.saveAndFlush(variant);
                logger.info("Restored {} units to variant {} (return)",
                    item.getQuantity(), variant.getSku());
            }
            return;
        }

        // Fallback: no variant recorded — restore to all active variants of the product
        if (item.getProduct() == null || item.getProduct().getId() == null) {
            logger.warn("Return item {} has neither variant nor product — cannot restore stock",
                item.getId());
            return;
        }
        List<ProductVariant> variants = productVariantsRepository.findByProductId(item.getProduct().getId());
        for (ProductVariant variant : variants) {
            if (variant.isActive()) {
                variant.setStockCurrent(variant.getStockCurrent() + item.getQuantity());
                productVariantsRepository.saveAndFlush(variant);
                logger.info("Restored {} units to variant {} (return, fallback path)",
                    item.getQuantity(), variant.getSku());
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
