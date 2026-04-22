package org.monostudio.api.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.RefundResultPojo;
import org.monostudio.api.models.ReturnRequestItemPojo;
import org.monostudio.api.models.ReturnRequestPojo;
import org.monostudio.api.services.RefundRetryService;
import org.monostudio.api.services.ReturnRequestService;
import org.monostudio.api.services.LoyaltyService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.ReturnRequest;
import org.monostudio.jpa.entities.ReturnRequestItem;
import org.monostudio.jpa.entities.StockAdjustment;
import org.monostudio.jpa.repositories.ProductVariantsRepository;
import org.monostudio.jpa.repositories.StockReservationsRepository;
import org.monostudio.jpa.repositories.OrderDetailsRepository;
import org.monostudio.jpa.repositories.ReturnRequestItemsRepository;
import org.monostudio.jpa.repositories.ReturnRequestsRepository;
import org.monostudio.jpa.services.conversion.ReturnRequestsConverterService;
import org.monostudio.jpa.services.crud.ReturnRequestsCrudService;
import org.monostudio.api.services.StockAdjustmentService;
import org.monostudio.mailing.kafka.KafkaMailProducer;
import org.monostudio.payment.PaymentService;
import org.monostudio.payment.PaymentServiceException;

import jakarta.persistence.EntityNotFoundException;
import java.util.Collection;
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
    private final StockReservationsRepository stockReservationsRepository;
    private final OrderDetailsRepository orderDetailsRepository;
    private final ReturnRequestsCrudService crudService;
    private final ReturnRequestsConverterService converterService;
    private final StockAdjustmentService stockAdjustmentService;
    private final KafkaMailProducer kafkaMailProducer;
    private final Map<String, PaymentService> paymentServices;
    private final RefundRetryService refundRetryService;
    private final LoyaltyService loyaltyService;

    @Autowired
    public ReturnRequestServiceImpl(
        ReturnRequestsRepository returnRequestsRepository,
        ReturnRequestItemsRepository itemsRepository,
        ProductVariantsRepository productVariantsRepository,
        StockReservationsRepository stockReservationsRepository,
        OrderDetailsRepository orderDetailsRepository,
        ReturnRequestsCrudService crudService,
        ReturnRequestsConverterService converterService,
        StockAdjustmentService stockAdjustmentService,
        KafkaMailProducer kafkaMailProducer,
        @Autowired(required = false) Map<String, PaymentService> paymentServices,
        @Autowired(required = false) RefundRetryService refundRetryService,
        LoyaltyService loyaltyService
    ) {
        this.returnRequestsRepository = returnRequestsRepository;
        this.itemsRepository = itemsRepository;
        this.productVariantsRepository = productVariantsRepository;
        this.stockReservationsRepository = stockReservationsRepository;
        this.orderDetailsRepository = orderDetailsRepository;
        this.crudService = crudService;
        this.converterService = converterService;
        this.stockAdjustmentService = stockAdjustmentService;
        this.kafkaMailProducer = kafkaMailProducer;
        this.paymentServices = paymentServices;
        this.refundRetryService = refundRetryService;
        this.loyaltyService = loyaltyService;
    }

    @Override
    public ReturnRequestPojo createReturnRequest(ReturnRequestPojo input) throws BadInputException {
        if (input.getStatus() == null) {
            input.setStatus(ReturnRequest.ReturnRequestStatus.PENDING.name());
        }

        // Validate return quantities against the original order before accepting the request.
        // A customer cannot return more than they ordered.
        if (input.getItems() != null && input.getOrderId() != null) {
            validateReturnQuantities(input.getOrderId(), input.getItems());
        }

        ReturnRequestPojo result = crudService.create(input);
        // Notify store owners of the new return request
        kafkaMailProducer.sendReturnRequestToOwners(result);
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
        kafkaMailProducer.sendReturnRequestStatusToClient(pojo);
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

        // ── Refund amount validation ─────────────────────────────────────────────
        // Validate refund amount BEFORE calling the gateway.
        Integer requestedRefundAmount = existing.getRefundAmount();
        org.monostudio.jpa.entities.Order order = existing.getOrder();
        if (order == null) {
            throw new BadInputException("Cannot complete refund — return request has no associated order");
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

        // ── Call payment gateway ─────────────────────────────────────────────────
        String token = order.getTransactionToken();
        PaymentService paymentService = paymentServices != null ? paymentServices.get(order.getPaymentType()) : null;
        boolean refundSucceeded = false;
        if (token != null && paymentService != null) {
            try {
                RefundResultPojo result = paymentService.refund(token, actualRefundAmount);
                if (result.isSuccess()) {
                    logger.info("Refund succeeded for return {}: type={}, code={}, amount={}",
                        id, result.getType(), result.getResponseCode(), actualRefundAmount);
                    refundSucceeded = true;
                } else {
                    logger.warn("Refund rejected by gateway for return {}: code={} — enqueuing for retry",
                        id, result.getResponseCode());
                    enqueueRefundRetry(order, "RETURN_COMPLETED", actualRefundAmount);
                }
            } catch (PaymentServiceException e) {
                // Gateway error — enqueue for automatic retry
                logger.error("Refund gateway error for return {}: {} — enqueuing for retry",
                    id, e.getMessage());
                enqueueRefundRetry(order, "RETURN_COMPLETED", actualRefundAmount);
            }
        }

        // ── Update order totals (regardless of gateway outcome) ─────────────────
        // Even if gateway is slow/retrying, update the accounting so we don't double-refund.
        order.setTotalRefundedAmount(alreadyRefunded + actualRefundAmount);

        // Status: only mark REFUND_COMPLETED if gateway succeeded immediately.
        // If enqueued for retry, leave at REFUND_PROCESSING so admins can track it.
        existing.setStatus(
            refundSucceeded
                ? ReturnRequest.ReturnRequestStatus.REFUND_COMPLETED
                : ReturnRequest.ReturnRequestStatus.REFUND_PROCESSING);
        if (adminNotes != null) {
            existing.setAdminNotes(adminNotes);
        }

        ReturnRequest saved = returnRequestsRepository.saveAndFlush(existing);
        if (refundSucceeded) {
            try {
                loyaltyService.syncRefundReversalForOrder(order.getId());
            } catch (RuntimeException e) {
                logger.error("Failed to sync loyalty refund reversal for order {}: {}", order.getId(), e.getMessage());
            }
        }
        List<ReturnRequestItem> items = itemsRepository.findByReturnRequestId(id);
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
        return buildReturnRequestPojo(saved, items);
    }

    @Override
    public ReturnRequestPojo startRefund(Long id, String adminNotes)
        throws EntityNotFoundException, BadInputException {
        ReturnRequest existing = returnRequestsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Return request not found: " + id));

        // Allow start refund from APPROVED or RECEIVED states
        if (existing.getStatus() != ReturnRequest.ReturnRequestStatus.APPROVED
            && existing.getStatus() != ReturnRequest.ReturnRequestStatus.RECEIVED) {
            throw new BadInputException(INVALID_STATE);
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
     * Restores stock when returned goods are received at the warehouse.
     *
     * Priority: restore to the exact variant if available (variantId field is set).
     * Fallback: restore to all active variants of the product (legacy behavior).
     *
     * Stock is restored ONLY at RECEIVED, not at approval — store must physically
     * receive and inspect the goods before returning them to sellable inventory.
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
                    "Return received, stock restored to inventory",
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

    /**
     * Enqueues a failed refund for automatic retry via the RefundRetryQueue.
     * P0.2: Replaces the previous "swallow exception and log" behavior.
     */
    private void enqueueRefundRetry(org.monostudio.jpa.entities.Order order, String reason, int amount) {
        if (refundRetryService == null) {
            // RefundRetryService not available — this is a critical gap.
            logger.error("⚠️  CRITICAL: RefundRetryService not available. "
                    + "Refund FAILED and NOT enqueued for retry. OrderId={}, Amount={}, Reason={}. "
                    + "Manual intervention required.",
                order.getId(), amount, reason);
            return;
        }
        try {
            refundRetryService.enqueueFailedRefund(order.getId(), order.getTransactionToken(), amount, reason);
        } catch (Exception e) {
            logger.error("⚠️  CRITICAL: Failed to enqueue refund retry. "
                    + "OrderId={}, Amount={}, Reason={}, Error={}. Manual intervention required.",
                order.getId(), amount, reason, e.getMessage());
        }
    }
}
