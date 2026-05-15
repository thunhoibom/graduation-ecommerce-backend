package org.monostudio.api.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ReturnRequestItemPojo;
import org.monostudio.api.models.ReturnRequestPojo;
import org.monostudio.api.services.ReturnRequestService;
import org.monostudio.api.services.ReturnShipmentService;
import org.monostudio.api.services.LoyaltyService;
import org.monostudio.api.services.support.OrderRefundPaymentStatusSupport;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.config.Constants;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.ReturnRequest;
import org.monostudio.jpa.entities.ReturnRequestItem;
import org.monostudio.jpa.entities.StockAdjustment;
import org.monostudio.jpa.repositories.ProductVariantsRepository;
import org.monostudio.jpa.repositories.StockReservationsRepository;
import org.monostudio.jpa.repositories.OrderDetailsRepository;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.ReturnRequestItemsRepository;
import org.monostudio.jpa.repositories.ReturnRequestsRepository;
import org.monostudio.jpa.services.conversion.ReturnRequestsConverterService;
import org.monostudio.jpa.services.crud.ReturnRequestsCrudService;
import org.monostudio.api.services.StockAdjustmentService;
import org.monostudio.mailing.kafka.KafkaMailProducer;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
@Service
public class ReturnRequestServiceImpl
    implements ReturnRequestService {
    private static final Logger logger = LoggerFactory.getLogger(ReturnRequestServiceImpl.class);
    private static final String INVALID_STATE = "The return request is not in a valid state for this operation";
    private static final Set<ReturnRequest.ReturnRequestStatus> OPEN_RETURN_REQUEST_STATUSES = Set.of(
        ReturnRequest.ReturnRequestStatus.PENDING,
        ReturnRequest.ReturnRequestStatus.APPROVED,
        ReturnRequest.ReturnRequestStatus.RECEIVED,
        ReturnRequest.ReturnRequestStatus.REFUND_PROCESSING
    );
    private final ReturnRequestsRepository returnRequestsRepository;
    private final ReturnRequestItemsRepository itemsRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final StockReservationsRepository stockReservationsRepository;
    private final OrderDetailsRepository orderDetailsRepository;
    private final OrdersRepository ordersRepository;
    private final ReturnRequestsCrudService crudService;
    private final ReturnRequestsConverterService converterService;
    private final StockAdjustmentService stockAdjustmentService;
    private final KafkaMailProducer kafkaMailProducer;
    private final LoyaltyService loyaltyService;
    private final ReturnShipmentService returnShipmentService;

    @Autowired
    public ReturnRequestServiceImpl(
        ReturnRequestsRepository returnRequestsRepository,
        ReturnRequestItemsRepository itemsRepository,
        ProductVariantsRepository productVariantsRepository,
        StockReservationsRepository stockReservationsRepository,
        OrderDetailsRepository orderDetailsRepository,
        OrdersRepository ordersRepository,
        ReturnRequestsCrudService crudService,
        ReturnRequestsConverterService converterService,
        StockAdjustmentService stockAdjustmentService,
        KafkaMailProducer kafkaMailProducer,
        LoyaltyService loyaltyService,
        @Autowired(required = false) ReturnShipmentService returnShipmentService
    ) {
        this.returnRequestsRepository = returnRequestsRepository;
        this.itemsRepository = itemsRepository;
        this.productVariantsRepository = productVariantsRepository;
        this.stockReservationsRepository = stockReservationsRepository;
        this.orderDetailsRepository = orderDetailsRepository;
        this.ordersRepository = ordersRepository;
        this.crudService = crudService;
        this.converterService = converterService;
        this.stockAdjustmentService = stockAdjustmentService;
        this.kafkaMailProducer = kafkaMailProducer;
        this.loyaltyService = loyaltyService;
        this.returnShipmentService = returnShipmentService;
    }

    @Override
    public ReturnRequestPojo createReturnRequest(ReturnRequestPojo input) throws BadInputException {
        if (input.getStatus() == null) {
            input.setStatus(ReturnRequest.ReturnRequestStatus.PENDING.name());
        }
        validateManualRefundBankInfo(input);

        // Validate return quantities against the original order before accepting the request.
        // A customer cannot return more than they ordered.
        if (input.getOrderId() != null) {
            validateOrderEligibleForReturn(input.getOrderId());
        }
        if (input.getItems() != null && input.getOrderId() != null) {
            validateReturnQuantities(input.getOrderId(), input.getItems());
        }

        ReturnRequestPojo result = crudService.create(input);
        kafkaMailProducer.sendReturnRequestToOwners(result);
        kafkaMailProducer.sendReturnRequestStatusToClient(result);
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

        // Automatically create GHN return-shipment so customer gets a label immediately.
        // This runs after DB commit; failures are enqueued for retry with exponential backoff.
        if (returnShipmentService != null) {
            try {
                returnShipmentService.requestReturnShipmentCreation(saved.getId());
                // Reload pojo after tracking number may have been set
                ReturnRequest reloaded = returnRequestsRepository.findById(saved.getId()).orElse(saved);
                pojo = buildReturnRequestPojo(reloaded, items);
            } catch (Exception e) {
                logger.warn("Return shipment creation failed for returnRequest={}: {} — will retry automatically",
                    saved.getId(), e.getMessage());
            }
        }

        // Notify customer of approval (email includes trackingNumber if already resolved)
        kafkaMailProducer.sendReturnRequestStatusToClient(pojo);

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
        kafkaMailProducer.sendReturnRequestStatusToClient(pojo);
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

        existing.setStatus(ReturnRequest.ReturnRequestStatus.RECEIVED);
        existing.setQcStatus(ReturnRequest.QcStatus.PENDING);
        if (adminNotes != null) {
            existing.setAdminNotes(adminNotes);
        }

        ReturnRequest saved = returnRequestsRepository.saveAndFlush(existing);
        ReturnRequestPojo pojo = buildReturnRequestPojo(saved, items);
        kafkaMailProducer.sendReturnRequestStatusToClient(pojo);
        return pojo;
    }

    @Override
    public ReturnRequestPojo submitQc(
        Long id,
        String result,
        String qcNotes,
        String qcPhotoUrls
    ) throws EntityNotFoundException, BadInputException {
        ReturnRequest existing = returnRequestsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Return request not found: " + id));

        if (existing.getStatus() != ReturnRequest.ReturnRequestStatus.RECEIVED) {
            throw new BadInputException(INVALID_STATE);
        }
        if (existing.getQcStatus() != ReturnRequest.QcStatus.PENDING) {
            throw new BadInputException("Warehouse QC has already been recorded for this return request");
        }
        if (result == null || result.isBlank()) {
            throw new BadInputException("QC result is required");
        }

        ReturnRequest.QcStatus qcResult;
        try {
            qcResult = ReturnRequest.QcStatus.valueOf(result.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadInputException("Unsupported QC result: " + result);
        }
        if (qcResult == ReturnRequest.QcStatus.PENDING) {
            throw new BadInputException("QC result must be PASSED or FAILED");
        }

        List<ReturnRequestItem> items = itemsRepository.findByReturnRequestId(id);
        existing.setQcCompletedAt(Instant.now());
        if (qcNotes != null && !qcNotes.isBlank()) {
            existing.setQcNotes(qcNotes.trim());
        }
        if (qcPhotoUrls != null && !qcPhotoUrls.isBlank()) {
            existing.setQcPhotoUrls(qcPhotoUrls.trim());
        }

        if (qcResult == ReturnRequest.QcStatus.PASSED) {
            for (ReturnRequestItem item : items) {
                restoreVariantStock(item);
            }
            existing.setQcStatus(ReturnRequest.QcStatus.PASSED);
        } else {
            existing.setQcStatus(ReturnRequest.QcStatus.FAILED);
            existing.setStatus(ReturnRequest.ReturnRequestStatus.REJECTED);
            if (qcNotes != null && !qcNotes.isBlank()) {
                String failureNote = "QC không đạt: " + qcNotes.trim();
                String existingNotes = existing.getAdminNotes();
                existing.setAdminNotes(existingNotes == null ? failureNote : existingNotes + "\n" + failureNote);
            }
        }

        ReturnRequest saved = returnRequestsRepository.saveAndFlush(existing);
        ReturnRequestPojo pojo = buildReturnRequestPojo(saved, items);
        kafkaMailProducer.sendReturnRequestStatusToClient(pojo);
        return pojo;
    }

    @Override
    public ReturnRequestPojo completeRefund(
        Long id,
        String adminNotes,
        String refundProofUrl,
        String refundReference,
        Instant refundedAt
    )
        throws EntityNotFoundException, BadInputException {
        ReturnRequest existing = returnRequestsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Return request not found: " + id));

        if (existing.getStatus() != ReturnRequest.ReturnRequestStatus.RECEIVED
            && existing.getStatus() != ReturnRequest.ReturnRequestStatus.REFUND_PROCESSING) {
            throw new BadInputException(INVALID_STATE);
        }
        if (existing.getQcStatus() != ReturnRequest.QcStatus.PASSED) {
            throw new BadInputException("Return request must pass warehouse QC before refund can be completed");
        }

        // ── Refund amount validation ─────────────────────────────────────────────
        // Validate refund amount BEFORE calling the gateway.
        Integer requestedRefundAmount = existing.getRefundAmount();
        org.monostudio.jpa.entities.Order order = existing.getOrder();
        if (order == null) {
            throw new BadInputException("Cannot complete refund — return request has no associated order");
        }
        List<ReturnRequestItem> items = itemsRepository.findByReturnRequestId(id);
        if (requestedRefundAmount == null || requestedRefundAmount <= 0) {
            int estimatedRefundAmount = estimateRefundAmountFromItems(order.getId(), items);
            if (estimatedRefundAmount > 0) {
                requestedRefundAmount = estimatedRefundAmount;
                existing.setRefundAmount(estimatedRefundAmount);
                logger.info("Return {} refundAmount missing; using estimated fallback amount={}",
                    id, estimatedRefundAmount);
            }
        }
        if (requestedRefundAmount == null || requestedRefundAmount <= 0) {
            throw new BadInputException("Refund amount must be greater than zero");
        }

        int orderTotal = order.getTotalValue();
        int alreadyRefunded = order.getTotalRefundedAmount();
        int remainingRefundable = Math.max(0, orderTotal - alreadyRefunded);

        // Cap refund to what is remaining on the order
        int actualRefundAmount = Math.min(requestedRefundAmount, remainingRefundable);
        if (actualRefundAmount == 0) {
            throw new BadInputException(
                "No remaining amount to refund on this order. Already refunded: " + alreadyRefunded);
        }
        if (requestedRefundAmount > remainingRefundable) {
            logger.warn("Return {}: requested refund {} exceeds remaining {}. Capping to {}.",
                id, requestedRefundAmount, remainingRefundable, actualRefundAmount);
        }

        if (existing.getRefundMethod() != null
            && existing.getRefundMethod() != ReturnRequest.RefundMethod.BANK_TRANSFER) {
            throw new BadInputException("Return refunds are processed via manual bank transfer only");
        }
        if ((refundProofUrl == null || refundProofUrl.isBlank())
            && (refundReference == null || refundReference.isBlank())) {
            throw new BadInputException("Manual bank refund requires transfer receipt image or refundReference");
        }

        order.setTotalRefundedAmount(alreadyRefunded + actualRefundAmount);
        OrderRefundPaymentStatusSupport.syncAfterRefund(order, ordersRepository);
        ordersRepository.saveAndFlush(order);

        existing.setStatus(ReturnRequest.ReturnRequestStatus.REFUND_COMPLETED);
        if (refundProofUrl != null && !refundProofUrl.isBlank()) {
            existing.setRefundProofUrl(refundProofUrl.trim());
        }
        if (refundReference != null && !refundReference.isBlank()) {
            existing.setRefundReference(refundReference.trim());
        }
        existing.setRefundedAt(refundedAt != null ? refundedAt : Instant.now());
        if (adminNotes != null) {
            existing.setAdminNotes(adminNotes);
        }

        ReturnRequest saved = returnRequestsRepository.saveAndFlush(existing);
        try {
            loyaltyService.syncRefundReversalForOrder(order.getId());
        } catch (RuntimeException e) {
            logger.error("Failed to sync loyalty refund reversal for order {}: {}", order.getId(), e.getMessage());
        }
        ReturnRequestPojo pojo = buildReturnRequestPojo(saved, items);
        kafkaMailProducer.sendReturnRequestStatusToClient(pojo);
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
        ReturnRequestPojo pojo = buildReturnRequestPojo(saved, items);
        kafkaMailProducer.sendReturnRequestStatusToClient(pojo);
        return pojo;
    }

    @Override
    public ReturnRequestPojo startRefund(Long id, String adminNotes)
        throws EntityNotFoundException, BadInputException {
        ReturnRequest existing = returnRequestsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Return request not found: " + id));

        // Allow start refund only after warehouse QC passes.
        if (existing.getStatus() != ReturnRequest.ReturnRequestStatus.RECEIVED) {
            throw new BadInputException(INVALID_STATE);
        }
        if (existing.getQcStatus() != ReturnRequest.QcStatus.PASSED) {
            throw new BadInputException("Return request must pass warehouse QC before refund can start");
        }

        existing.setStatus(ReturnRequest.ReturnRequestStatus.REFUND_PROCESSING);
        if (adminNotes != null) {
            String existingNotes = existing.getAdminNotes();
            existing.setAdminNotes(existingNotes == null ? adminNotes : existingNotes + "\n" + adminNotes);
        }

        ReturnRequest saved = returnRequestsRepository.saveAndFlush(existing);
        List<ReturnRequestItem> items = itemsRepository.findByReturnRequestId(id);
        ReturnRequestPojo pojo = buildReturnRequestPojo(saved, items);
        kafkaMailProducer.sendReturnRequestStatusToClient(pojo);
        return pojo;
    }

    @Override
    public ReturnRequestPojo addNote(Long id, String note)
        throws EntityNotFoundException, BadInputException {
        if (note == null || note.isBlank()) {
            throw new BadInputException("Note content cannot be empty");
        }
        ReturnRequest existing = returnRequestsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Return request not found: " + id));

        String existingNotes = existing.getAdminNotes();
        existing.setAdminNotes(existingNotes == null ? note : existingNotes + "\n" + note);
        ReturnRequest saved = returnRequestsRepository.saveAndFlush(existing);
        List<ReturnRequestItem> items = itemsRepository.findByReturnRequestId(id);
        return buildReturnRequestPojo(saved, items);
    }

    /**
     * Restores stock when returned goods pass warehouse QC.
     *
     * Priority: restore to the exact variant if available (variantId field is set).
     * Fallback: restore to all active variants of the product (legacy behavior).
     *
     * Stock is restored only after QC passes, not when goods are first received.
     *
     * Uses atomic native SQL (restoreStock) + writes an audit log entry so the
     * stock movement is traceable in the StockAdjustment ledger.
     */
    private void restoreVariantStock(ReturnRequestItem item) {
        // Preferred path: restore to the exact variant that was purchased
        if (item.getVariant() != null && item.getVariant().getId() != null) {
            ProductVariant variant = productVariantsRepository.getById(item.getVariant().getId());
            if (variant != null) {
                // Atomic: stockCurrent += qty, stockReserved -= qty (for consistency)
                stockReservationsRepository.restoreStock(variant.getId(), item.getQuantity());

                // Audit trail — RETURN_RESTORED records the stock movement
                stockAdjustmentService.recordForVariant(
                    variant,
                    StockAdjustment.StockAdjustmentReason.RETURN_RESTORED,
                    item.getQuantity(),
                    "Return QC passed, stock restored to inventory",
                    null,    // no cart session
                    null,    // no order
                    item.getReturnRequest() != null ? item.getReturnRequest().getId() : null,
                    item.getId()
                );

                logger.info("Restored {} units to variant {} (return id={})",
                    item.getQuantity(), variant.getSku(),
                    item.getReturnRequest() != null ? item.getReturnRequest().getId() : "unknown");
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
                stockReservationsRepository.restoreStock(variant.getId(), item.getQuantity());

                stockAdjustmentService.recordForVariant(
                    variant,
                    StockAdjustment.StockAdjustmentReason.RETURN_RESTORED,
                    item.getQuantity(),
                    "Return received, stock restored to inventory (fallback — no variant recorded)",
                    null,
                    null,
                    item.getReturnRequest() != null ? item.getReturnRequest().getId() : null,
                    item.getId()
                );

                logger.info("Restored {} units to variant {} (return, fallback path)",
                    item.getQuantity(), variant.getSku());
            }
        }
    }

    /**
     * Validates that no return item quantity exceeds what was originally ordered.
     * Prevents customers from claiming more units than they actually purchased.
     *
     * @param orderId         Original order ID
     * @param returnItems      Items being returned
     * @throws BadInputException if any item quantity exceeds the order quantity
     */
    private void validateOrderEligibleForReturn(Long orderId) throws BadInputException {
        Order order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new BadInputException("Order not found: " + orderId));

        String fulfillment = order.getFulfillmentStatus();
        if (!isReturnEligibleFulfillment(fulfillment)) {
            throw new BadInputException("This order is not eligible for a return request");
        }

        int total = Math.max(0, order.getTotalValue());
        int refunded = Math.max(0, order.getTotalRefundedAmount());
        if (total > 0 && refunded >= total) {
            throw new BadInputException("This order has already been fully refunded");
        }
        if (Constants.ORDER_PAYMENT_STATUS_REFUNDED.equalsIgnoreCase(order.getPaymentStatus())) {
            throw new BadInputException("This order has already been fully refunded");
        }

        Optional<ReturnRequest> existing = returnRequestsRepository.findByOrderId(orderId);
        if (existing.isPresent() && OPEN_RETURN_REQUEST_STATUSES.contains(existing.get().getStatus())) {
            throw new BadInputException("A return request is already being processed for this order");
        }
        if (existing.isPresent()
            && existing.get().getStatus() == ReturnRequest.ReturnRequestStatus.REFUND_COMPLETED) {
            throw new BadInputException("This order already has a completed return request");
        }
    }

    private boolean isReturnEligibleFulfillment(String fulfillment) {
        if (fulfillment == null || fulfillment.isBlank()) {
            return false;
        }
        return Constants.ORDER_FULFILLMENT_STATUS_COMPLETED.equals(fulfillment)
            || "DELIVERED".equals(fulfillment)
            || "COMPLETED".equals(fulfillment)
            || "CANCELLED".equals(fulfillment);
    }

    private void validateReturnQuantities(Long orderId, Collection<ReturnRequestItemPojo> returnItems)
        throws BadInputException {
        if (returnItems == null || returnItems.isEmpty()) {
            return;
        }

        // Build a map of ordered quantities by variantId (or productId as fallback)
        Map<Long, Integer> orderedQuantityByVariant = orderDetailsRepository.findBySellId(orderId)
            .stream()
            .collect(Collectors.toMap(
                d -> d.getProductVariant() != null ? d.getProductVariant().getId() : 0L,
                d -> d.getUnits(),
                (a, b) -> a // in case of duplicate key, keep first
            ));

        for (ReturnRequestItemPojo returnItem : returnItems) {
            Long variantId = returnItem.getVariantId();
            int requestedQty = returnItem.getQuantity();

            if (requestedQty <= 0) {
                throw new BadInputException(
                    "Return quantity must be greater than zero"
                        + (variantId != null ? " for variant " + variantId : ""));
            }

            if (variantId != null && variantId != 0L) {
                Integer orderedQty = orderedQuantityByVariant.get(variantId);
                if (orderedQty == null) {
                    throw new BadInputException(
                        "Cannot return variant " + variantId
                            + " — it was not part of order " + orderId);
                }
                if (requestedQty > orderedQty) {
                    throw new BadInputException(
                        "Cannot return " + requestedQty + " units of variant " + variantId
                            + ": only " + orderedQty + " were ordered");
                }
            }
            // If no variantId, we cannot do precise validation — allow it (admin review will catch it)
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

    private int estimateRefundAmountFromItems(Long orderId, List<ReturnRequestItem> items) {
        if (orderId == null || items == null || items.isEmpty()) {
            return 0;
        }
        Map<Long, Integer> byVariantUnitValue = orderDetailsRepository.findBySellId(orderId).stream()
            .filter(detail -> detail.getProductVariant() != null && detail.getProductVariant().getId() != null)
            .collect(Collectors.toMap(
                detail -> detail.getProductVariant().getId(),
                detail -> detail.getUnitValue() != null ? detail.getUnitValue() : 0,
                (a, b) -> a
            ));
        return items.stream()
            .mapToInt(item -> {
                Long variantId = item.getVariant() != null ? item.getVariant().getId() : null;
                Integer unitValue = variantId != null ? byVariantUnitValue.get(variantId) : null;
                if (unitValue == null) {
                    return 0;
                }
                return Math.max(item.getQuantity(), 0) * Math.max(unitValue, 0);
            })
            .sum();
    }

    private void validateManualRefundBankInfo(ReturnRequestPojo input) throws BadInputException {
        if (input == null) {
            return;
        }
        if (input.getRefundMethod() == null || input.getRefundMethod().isBlank()) {
            input.setRefundMethod(ReturnRequest.RefundMethod.BANK_TRANSFER.name());
        }
        if (!ReturnRequest.RefundMethod.BANK_TRANSFER.name().equalsIgnoreCase(input.getRefundMethod())) {
            throw new BadInputException("Only BANK_TRANSFER refunds are supported");
        }
        if (input.getRefundBankName() == null || input.getRefundBankName().isBlank()) {
            throw new BadInputException("Bank name is required for BANK_TRANSFER");
        }
        if (input.getRefundBankAccountNumber() == null || input.getRefundBankAccountNumber().isBlank()) {
            throw new BadInputException("Bank account number is required for BANK_TRANSFER");
        }
        if (input.getRefundBankAccountHolder() == null || input.getRefundBankAccountHolder().isBlank()) {
            throw new BadInputException("Bank account holder is required for BANK_TRANSFER");
        }
    }
}
