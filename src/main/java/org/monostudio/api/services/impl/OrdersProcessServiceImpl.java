package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.api.models.OrderDetailPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.RefundResultPojo;
import org.monostudio.mailing.kafka.KafkaMailProducer;
import org.monostudio.ordering.kafka.KafkaOrderProducer;
import org.monostudio.api.services.RefundRetryService;
import org.monostudio.api.services.DiscountService;
import org.monostudio.api.services.OrdersProcessService;
import org.monostudio.api.services.StockReservationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.OrderDetail;
import org.monostudio.jpa.entities.OrderStatus;
import org.monostudio.jpa.entities.StockAdjustment;
import org.monostudio.jpa.repositories.CartItemsRepository;
import org.monostudio.jpa.repositories.CartSessionsRepository;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.OrderDetailsRepository;
import org.monostudio.jpa.repositories.OrderStatusesRepository;
import org.monostudio.jpa.services.conversion.ProductsConverterService;
import org.monostudio.jpa.services.conversion.OrdersConverterService;
import org.monostudio.jpa.services.crud.OrdersCrudService;
import org.monostudio.payment.PaymentService;
import org.monostudio.payment.PaymentServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.monostudio.config.Constants.ORDER_STATUS_ADMIN_CANCELLED;
import static org.monostudio.config.Constants.ORDER_STATUS_COMPLETED;
import static org.monostudio.config.Constants.ORDER_STATUS_PAID_CONFIRMED;
import static org.monostudio.config.Constants.ORDER_STATUS_PAID_UNCONFIRMED;
import static org.monostudio.config.Constants.ORDER_STATUS_PAYMENT_CANCELLED;
import static org.monostudio.config.Constants.ORDER_STATUS_PAYMENT_FAILED;
import static org.monostudio.config.Constants.ORDER_STATUS_PAYMENT_STARTED;
import static org.monostudio.config.Constants.ORDER_STATUS_PENDING;
import static org.monostudio.config.Constants.ORDER_STATUS_REJECTED;

@Transactional
@Service
public class OrdersProcessServiceImpl
    implements OrdersProcessService {
    private static final String THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION = "The transaction is not in a valid state for this api";
    private static final String NO_STATUS_MATCHES_THE = "No status matches the";
    private static final String NAME_IS_THE_DATABASE_EMPTY_OR_CORRUPT = "name - Is the database empty or corrupt?";
    private final Logger logger = LoggerFactory.getLogger(OrdersProcessServiceImpl.class);
    private final OrdersCrudService crudService;
    private final OrdersRepository ordersRepository;
    private final OrderDetailsRepository orderDetailsRepository;
    private final OrderStatusesRepository orderStatusesRepository;
    private final OrdersConverterService converterService;
    private final ProductsConverterService productConverterService;
    private final KafkaMailProducer kafkaMailProducer;
    private final KafkaOrderProducer kafkaOrderProducer;
    private final StockReservationService stockReservationService;
    private final DiscountService discountService;
    private final Map<String, PaymentService> paymentServices;
    private final RefundRetryService refundRetryService;
    private final CartSessionsRepository cartSessionsRepository;
    private final CartItemsRepository cartItemsRepository;

    public OrdersProcessServiceImpl(
        OrdersCrudService crudService,
        OrdersRepository ordersRepository,
        OrderDetailsRepository orderDetailsRepository,
        OrderStatusesRepository orderStatusesRepository,
        OrdersConverterService converterService,
        ProductsConverterService productConverterService,
        KafkaMailProducer kafkaMailProducer,
        KafkaOrderProducer kafkaOrderProducer,
        StockReservationService stockReservationService,
        DiscountService discountService,
        @Autowired(required = false) Map<String, PaymentService> paymentServices,
        @Autowired(required = false) RefundRetryService refundRetryService,
        CartSessionsRepository cartSessionsRepository,
        CartItemsRepository cartItemsRepository
    ) {
        this.crudService = crudService;
        this.ordersRepository = ordersRepository;
        this.orderDetailsRepository = orderDetailsRepository;
        this.orderStatusesRepository = orderStatusesRepository;
        this.converterService = converterService;
        this.productConverterService = productConverterService;
        this.kafkaMailProducer = kafkaMailProducer;
        this.kafkaOrderProducer = kafkaOrderProducer;
        this.stockReservationService = stockReservationService;
        this.discountService = discountService;
        this.paymentServices = paymentServices;
        this.refundRetryService = refundRetryService;
        this.cartSessionsRepository = cartSessionsRepository;
        this.cartItemsRepository = cartItemsRepository;
    }

    private void sendClientEmail(OrderPojo order) {
        kafkaMailProducer.sendOrderStatusToClient(order);
    }

    private void sendOwnerEmail(OrderPojo order) {
        kafkaMailProducer.sendOrderStatusToOwners(order);
    }

    // TODO figure out how to shorten below methods
    // TODO to compare statuses use numbers, not strings
    @Override
    public OrderPojo markAsStarted(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!existingOrder.getStatus().getName().equals(ORDER_STATUS_PENDING)) {
            // P0.5: Reject if order is not in PENDING — cannot restart payment for an already-started order.
            throw new BadInputException(
                "Cannot start payment for order " + existingOrder.getId()
                    + " — current status is '" + existingOrder.getStatus().getName()
                    + "', expected '" + ORDER_STATUS_PENDING + "'.");
        }

        Optional<OrderStatus> startedStatus = orderStatusesRepository.findByName(ORDER_STATUS_PAYMENT_STARTED);
        if (startedStatus.isEmpty()) {
            throw new IllegalStateException(NO_STATUS_MATCHES_THE + " '" + ORDER_STATUS_PAYMENT_STARTED + "' " + NAME_IS_THE_DATABASE_EMPTY_OR_CORRUPT);
        }
        ordersRepository.setStatus(existingOrder.getId(), startedStatus.get());
        ordersRepository.setTransactionToken(existingOrder.getId(), sell.getToken());

        OrderPojo target = this.convertOrThrowException(existingOrder);
        target.setStatus(ORDER_STATUS_PAYMENT_STARTED);
        kafkaOrderProducer.publishOrderPaymentStarted(existingOrder.getId(), existingOrder.getCartSessionToken());
        sendClientEmail(target);
        return target;
    }

    @Override
    public OrderPojo markAsAborted(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!existingOrder.getStatus().getName().equals(ORDER_STATUS_PAYMENT_STARTED)) {
            // P0.5: If order is not in PAYMENT_STARTED, it has already been processed.
            // This can happen when the gateway retries a callback after the first succeeded.
            // The order may already be PAID, CANCELLED, or FAILED — do not reprocess.
            throw new BadInputException(
                "Cannot abort order " + existingOrder.getId()
                    + " — current status is '" + existingOrder.getStatus().getName()
                    + "', expected '" + ORDER_STATUS_PAYMENT_STARTED + "'. "
                    + "Possible duplicate callback — order may have already been processed.");
        }

        Optional<OrderStatus> abortedStatus = orderStatusesRepository.findByName(ORDER_STATUS_PAYMENT_CANCELLED);
        if (abortedStatus.isEmpty()) {
            throw new IllegalStateException(NO_STATUS_MATCHES_THE + " '" + ORDER_STATUS_PAYMENT_CANCELLED + "' " + NAME_IS_THE_DATABASE_EMPTY_OR_CORRUPT);
        }
        ordersRepository.setStatus(existingOrder.getId(), abortedStatus.get());

        OrderPojo target = this.convertOrThrowException(existingOrder);
        target.setStatus(ORDER_STATUS_PAYMENT_CANCELLED);

        // Release stock reservations since payment was cancelled
        if (existingOrder.getCartSessionToken() != null) {
            stockReservationService.release(existingOrder.getCartSessionToken());
        }

        sendClientEmail(target);
        kafkaOrderProducer.publishOrderAborted(existingOrder.getId(), existingOrder.getCartSessionToken());
        return target;
    }

    @Override
    public OrderPojo markAsFailed(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!existingOrder.getStatus().getName().equals(ORDER_STATUS_PAYMENT_STARTED)) {
            throw new BadInputException(
                "Cannot mark order " + existingOrder.getId() + " as failed"
                    + " — current status is '" + existingOrder.getStatus().getName()
                    + "', expected '" + ORDER_STATUS_PAYMENT_STARTED + "'. "
                    + "Possible duplicate callback.");
        }

        Optional<OrderStatus> failedStatus = orderStatusesRepository.findByName(ORDER_STATUS_PAYMENT_FAILED);
        if (failedStatus.isEmpty()) {
            throw new IllegalStateException(NO_STATUS_MATCHES_THE + " '" + ORDER_STATUS_PAYMENT_FAILED + "' " + NAME_IS_THE_DATABASE_EMPTY_OR_CORRUPT);
        }
        ordersRepository.setStatus(existingOrder.getId(), failedStatus.get());

        OrderPojo target = this.convertOrThrowException(existingOrder);
        target.setStatus(ORDER_STATUS_PAYMENT_FAILED);

        // Release stock reservations since payment failed
        if (existingOrder.getCartSessionToken() != null) {
            stockReservationService.release(existingOrder.getCartSessionToken());
        }

        sendClientEmail(target);
        kafkaOrderProducer.publishOrderFailed(existingOrder.getId(), existingOrder.getCartSessionToken());
        return target;
    }

    @Override
    public OrderPojo markAsPaid(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!existingOrder.getStatus().getName().equals(ORDER_STATUS_PAYMENT_STARTED)) {
            // P0.5: Reject if order is not in PAYMENT_STARTED.
            // This guards against race conditions and duplicate webhook callbacks.
            // Note: PaymentCallbackLog in CheckoutServiceImpl is the primary defense;
            // this is the secondary defense at the service layer.
            throw new BadInputException(
                "Cannot mark order " + existingOrder.getId() + " as paid"
                    + " — current status is '" + existingOrder.getStatus().getName()
                    + "', expected '" + ORDER_STATUS_PAYMENT_STARTED + "'. "
                    + "This may be a duplicate payment callback. "
                    + "If the order should already be PAID, no action is needed.");
        }

        Optional<OrderStatus> paidStatus = orderStatusesRepository.findByName(ORDER_STATUS_PAID_UNCONFIRMED);
        if (paidStatus.isEmpty()) {
            throw new IllegalStateException(NO_STATUS_MATCHES_THE + " '" + ORDER_STATUS_PAID_UNCONFIRMED + "' " + NAME_IS_THE_DATABASE_EMPTY_OR_CORRUPT);
        }
        ordersRepository.setStatus(existingOrder.getId(), paidStatus.get());

        OrderPojo target = this.convertOrThrowException(existingOrder);

        List<OrderDetailPojo> pojoDetails = new ArrayList<>();
        for (OrderDetail detail : orderDetailsRepository.findBySellId(existingOrder.getId())) {
            ProductPojo productPojo = productConverterService.convertToPojo(detail.getProduct());
            OrderDetailPojo orderDetailPojo = OrderDetailPojo.builder()
                .units(detail.getUnits())
                .unitValue(detail.getUnitValue())
                .product(productPojo)
                .variantId(detail.getProductVariant() != null ? detail.getProductVariant().getId() : null)
                .build();
            pojoDetails.add(orderDetailPojo);
        }
        target.setStatus(ORDER_STATUS_PAID_UNCONFIRMED);
        target.setDetails(pojoDetails);

        // Confirm stock reservations per-variant.
        // FAIL-FAST: if ANY item fails to deduct (e.g. stock went to 0 between checkout start
        // and payment confirmation), the entire order is rolled back. The customer must retry.
        // We do NOT allow a "partial fill" order — the payment gateway has already charged the
        // full amount and the customer expects all items.
        if (existingOrder.getCartSessionToken() != null) {
            List<OrderDetail> details = orderDetailsRepository.findBySellId(existingOrder.getId());
            for (OrderDetail detail : details) {
                if (detail.getProductVariant() != null) {
                    // confirmItem throws IllegalStateException on deduct failure → triggers rollback
                    stockReservationService.confirmItem(
                        existingOrder.getCartSessionToken(),
                        detail.getProductVariant().getSku()
                    );
                }
            }
        }

        // Redeem discount only after payment is confirmed — not when checkout starts.
        // This prevents "burning" a discount code when the customer abandons or fails payment.
        // Throwing here is intentional: if redeem fails the transaction is rolled back and the
        // customer must retry. An order cannot be marked PAID while the discount hasn't been
        // recorded — that would let the customer reuse the same code on a second attempt.
        if (existingOrder.getDiscountCode() != null && !existingOrder.getDiscountCode().isBlank()) {
            int subtotal = existingOrder.getNetValue() + existingOrder.getTaxesValue();
            Long customerId = (existingOrder.getCustomer() != null) ? existingOrder.getCustomer().getId() : null;
            discountService.redeemDiscount(existingOrder.getDiscountCode(), subtotal, customerId, existingOrder.getId());
        }

        sendClientEmail(target);
        sendOwnerEmail(target);
        // Publish ORDER_PAID event → KafkaOrderConsumer clears the cart session asynchronously
        kafkaOrderProducer.publishOrderPaid(existingOrder.getId(), existingOrder.getCartSessionToken());

        return target;
    }

    @Override
    public OrderPojo markAsConfirmed(OrderPojo sell)
        throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!existingOrder.getStatus().getName().equals(ORDER_STATUS_PAID_UNCONFIRMED)) {
            throw new BadInputException(THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION);
        }

        Optional<OrderStatus> confirmedStatus = orderStatusesRepository.findByName(ORDER_STATUS_PAID_CONFIRMED);
        if (confirmedStatus.isEmpty()) {
            throw new IllegalStateException(NO_STATUS_MATCHES_THE + " '" + ORDER_STATUS_PAID_CONFIRMED + "' " + NAME_IS_THE_DATABASE_EMPTY_OR_CORRUPT);
        }
        ordersRepository.setStatus(existingOrder.getId(), confirmedStatus.get());

        OrderPojo target = this.convertOrThrowException(existingOrder);

        List<OrderDetailPojo> pojoDetails = new ArrayList<>();
        for (OrderDetail detail : orderDetailsRepository.findBySellId(existingOrder.getId())) {
            ProductPojo productPojo = productConverterService.convertToPojo(detail.getProduct());
            OrderDetailPojo orderDetailPojo = OrderDetailPojo.builder()
                .units(detail.getUnits())
                .unitValue(detail.getUnitValue())
                .product(productPojo)
                .build();
            pojoDetails.add(orderDetailPojo);
        }
        target.setDetails(pojoDetails);
        target.setStatus(ORDER_STATUS_PAID_CONFIRMED);

        sendClientEmail(target);
        kafkaOrderProducer.publishOrderConfirmed(existingOrder.getId());

        return target;
    }

    @Override
    public OrderPojo markAsRejected(OrderPojo sell)
        throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!existingOrder.getStatus().getName().equals(ORDER_STATUS_PAID_UNCONFIRMED)) {
            throw new BadInputException(THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION);
        }

        Optional<OrderStatus> rejectedStatus = orderStatusesRepository.findByName(ORDER_STATUS_REJECTED);
        if (rejectedStatus.isEmpty()) {
            throw new IllegalStateException(NO_STATUS_MATCHES_THE + " '" + ORDER_STATUS_REJECTED + "' " + NAME_IS_THE_DATABASE_EMPTY_OR_CORRUPT);
        }

        // P2: Fetch lazy-loaded fields BEFORE status update clears the persistence context
        String paymentTypeName = existingOrder.getPaymentType() != null ? existingOrder.getPaymentType().getName() : null;

        ordersRepository.setStatus(existingOrder.getId(), rejectedStatus.get());

        OrderPojo target = this.convertOrThrowException(existingOrder);

        List<OrderDetailPojo> pojoDetails = new ArrayList<>();
        for (OrderDetail detail : orderDetailsRepository.findBySellId(existingOrder.getId())) {
            ProductPojo productPojo = productConverterService.convertToPojo(detail.getProduct());
            OrderDetailPojo orderDetailPojo = OrderDetailPojo.builder()
                .units(detail.getUnits())
                .unitValue(detail.getUnitValue())
                .product(productPojo)
                .build();
            pojoDetails.add(orderDetailPojo);
        }
        target.setDetails(pojoDetails);
        target.setStatus(ORDER_STATUS_REJECTED);

        // Restore stockCurrent: the order was rejected after payment, so the stock that was
        // deducted at markAsPaid must be returned to available inventory.
        if (existingOrder.getCartSessionToken() != null) {
            for (OrderDetail detail : orderDetailsRepository.findBySellId(existingOrder.getId())) {
                if (detail.getProductVariant() != null) {
                    stockReservationService.restoreStockCurrent(
                        existingOrder.getCartSessionToken(),
                        detail.getProductVariant().getSku(),
                        detail.getUnits(),
                        existingOrder.getId(),
                        StockAdjustment.StockAdjustmentReason.ORDER_REJECTED
                    );
                }
            }
        }

        // Initiate refund for the customer since payment was already captured.
        PaymentService paymentService = (paymentServices != null && paymentTypeName != null) ? paymentServices.get(paymentTypeName) : null;
        if (existingOrder.getTransactionToken() != null && paymentService != null) {
            try {
                RefundResultPojo result = paymentService.refund(
                    existingOrder.getTransactionToken(),
                    existingOrder.getTotalValue()
                );
                if (result.isSuccess()) {
                    logger.info("Order {} rejected: immediate refund succeeded, type={}",
                        existingOrder.getId(), result.getType());
                } else {
                    logger.warn("Order {} rejected: refund rejected by gateway (code={}) — enqueuing for retry",
                        existingOrder.getId(), result.getResponseCode());
                    enqueueRefundRetry(existingOrder, "ORDER_REJECTED");
                }
            } catch (PaymentServiceException e) {
                logger.error("Order {} rejected: refund gateway error ({}) — enqueuing for retry",
                    existingOrder.getId(), e.getMessage());
                enqueueRefundRetry(existingOrder, "ORDER_REJECTED");
            }
        } else {
            // No payment service — log critical alert so admin knows money is stuck
            logger.error("⚠️  CRITICAL: Order {} rejected but NO payment service available. "
                    + "Customer has been charged {} cents but cannot be refunded automatically.",
                existingOrder.getId(), existingOrder.getTotalValue());
        }

        sendClientEmail(target);
        sendOwnerEmail(target);
        kafkaOrderProducer.publishOrderRejected(existingOrder.getId());

        return target;
    }

    @Override
    public OrderPojo markAsCompleted(OrderPojo sell)
        throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!existingOrder.getStatus().getName().equals(ORDER_STATUS_PAID_CONFIRMED)) {
            throw new BadInputException(THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION);
        }

        Optional<OrderStatus> completedStatus = orderStatusesRepository.findByName(ORDER_STATUS_COMPLETED);
        if (completedStatus.isEmpty()) {
            throw new IllegalStateException(NO_STATUS_MATCHES_THE + " '" + ORDER_STATUS_COMPLETED + "' " + NAME_IS_THE_DATABASE_EMPTY_OR_CORRUPT);
        }
        ordersRepository.setStatus(existingOrder.getId(), completedStatus.get());

        OrderPojo target = this.convertOrThrowException(existingOrder);

        List<OrderDetailPojo> pojoDetails = new ArrayList<>();
        for (OrderDetail detail : orderDetailsRepository.findBySellId(existingOrder.getId())) {
            ProductPojo productPojo = productConverterService.convertToPojo(detail.getProduct());
            OrderDetailPojo orderDetailPojo = OrderDetailPojo.builder()
                .units(detail.getUnits())
                .unitValue(detail.getUnitValue())
                .product(productPojo)
                .build();
            pojoDetails.add(orderDetailPojo);
        }
        target.setDetails(pojoDetails);
        target.setStatus(ORDER_STATUS_COMPLETED);

        sendClientEmail(target);
        kafkaOrderProducer.publishOrderCompleted(existingOrder.getId());

        return target;
    }

    @Override
    public OrderPojo markAsAdminCancelled(OrderPojo sell, String reason)
        throws BadInputException, EntityNotFoundException {
        Order existingOrder = fetchExistingOrThrowException(sell);

        String currentStatus = existingOrder.getStatus().getName();
        boolean canCancel =
            currentStatus.equals(ORDER_STATUS_PENDING)
            || currentStatus.equals(ORDER_STATUS_PAYMENT_STARTED)
            || currentStatus.equals(ORDER_STATUS_PAID_UNCONFIRMED)
            || currentStatus.equals(ORDER_STATUS_PAID_CONFIRMED);

        if (!canCancel) {
            throw new BadInputException(
                "Cannot cancel order in status '" + currentStatus + "'");
        }

        // Determine if payment has already been made BEFORE changing the status
        boolean wasPaid = currentStatus.equals(ORDER_STATUS_PAID_UNCONFIRMED)
            || currentStatus.equals(ORDER_STATUS_PAID_CONFIRMED);

        Optional<OrderStatus> cancelledStatus =
            orderStatusesRepository.findByName(ORDER_STATUS_ADMIN_CANCELLED);
        if (cancelledStatus.isEmpty()) {
            throw new IllegalStateException(
                "Status '" + ORDER_STATUS_ADMIN_CANCELLED
                    + "' not found in DB — has it been seeded?");
        }

        // P2: Fetch lazy-loaded fields BEFORE status update clears the persistence context
        String paymentTypeName = existingOrder.getPaymentType() != null ? existingOrder.getPaymentType().getName() : null;

        ordersRepository.setStatus(existingOrder.getId(), cancelledStatus.get());

        OrderPojo target = convertOrThrowException(existingOrder);
        target.setStatus(ORDER_STATUS_ADMIN_CANCELLED);

        // Restore stock based on current order status:
        // - PENDING / PAYMENT_STARTED: stock was reserved but never deducted → release reservation only
        // - PAID_UNCONFIRMED / PAID_CONFIRMED: stock was deducted at markAsPaid → restore stockCurrent
        if (existingOrder.getCartSessionToken() != null) {
            if (wasPaid) {
                // Stock was already deducted — restore it back to available inventory
                for (OrderDetail detail : orderDetailsRepository.findBySellId(existingOrder.getId())) {
                    if (detail.getProductVariant() != null) {
                        stockReservationService.restoreStockCurrent(
                            existingOrder.getCartSessionToken(),
                            detail.getProductVariant().getSku(),
                            detail.getUnits(),
                            existingOrder.getId(),
                            StockAdjustment.StockAdjustmentReason.ORDER_CANCELLED
                        );
                    }
                }
            } else {
                // Payment not yet made — just release the reservation
                stockReservationService.release(existingOrder.getCartSessionToken());
            }
        }

        // If payment was already made, trigger a refund through the payment gateway.
        PaymentService paymentService = (paymentServices != null && paymentTypeName != null) ? paymentServices.get(paymentTypeName) : null;
        if (wasPaid && existingOrder.getTransactionToken() != null && paymentService != null) {
            try {
                RefundResultPojo result = paymentService.refund(
                    existingOrder.getTransactionToken(),
                    existingOrder.getTotalValue()
                );
                if (result.isSuccess()) {
                    logger.info("Admin cancelled order {}: refund succeeded, type={}",
                        existingOrder.getId(), result.getType());
                } else {
                    logger.warn("Admin cancelled order {}: refund rejected by gateway (code={}) — enqueuing for retry",
                        existingOrder.getId(), result.getResponseCode());
                    enqueueRefundRetry(existingOrder, "ADMIN_CANCELLED");
                }
            } catch (PaymentServiceException e) {
                // Gateway unreachable or errored — enqueue for retry instead of swallowing
                logger.error("Admin cancelled order {}: refund gateway error ({}). "
                        + "Enqueued for automatic retry.",
                    existingOrder.getId(), e.getMessage());
                enqueueRefundRetry(existingOrder, "ADMIN_CANCELLED");
            }
        }

        logger.info("Order {} admin-cancelled. Reason: {}. WasPaid: {}",
            existingOrder.getId(), reason, wasPaid);
        sendClientEmail(target);
        sendOwnerEmail(target);
        kafkaOrderProducer.publishOrderCancelled(existingOrder.getId());

        return target;
    }

    @Override
    public int expireStalePaymentSessions() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.MINUTES);
        List<Order> stale = ordersRepository.findByStatusNameAndDateBefore(
            ORDER_STATUS_PAYMENT_STARTED, cutoff);

        int count = 0;
        for (Order order : stale) {
            try {
                OrderPojo pojo = converterService.convertToPojo(order);
                pojo.setToken(order.getTransactionToken());
                markAsAborted(pojo);
                count++;
            } catch (Exception e) {
                logger.error("Failed to expire stale order {}: {}", order.getId(), e.getMessage());
            }
        }
        return count;
    }

    private Order fetchExistingOrThrowException(OrderPojo sell) throws BadInputException {
        Optional<Order> match = crudService.getExisting(sell);
        if (match.isEmpty()) {
            throw new EntityNotFoundException("No transaction matches given input");
        }
        return match.get();
    }

    private OrderPojo convertOrThrowException(Order existingOrder) {
        Order freshInstance = ordersRepository.getById(existingOrder.getId());
        OrderPojo target = converterService.convertToPojo(freshInstance);
        if (target==null) {
            throw new IllegalStateException("Converter could not turn Sell into its Pojo equivalent");
        }
        return target;
    }

    /**
     * Enqueues a failed refund for automatic retry via the RefundRetryQueue.
     * P0.2: Replaces the previous "swallow exception and log" behavior.
     */
    private void enqueueRefundRetry(Order order, String reason) {
        if (refundRetryService == null) {
            // RefundRetryService not available — this is a critical gap.
            // Log as CRITICAL so it is immediately visible in logs.
            logger.error("⚠️  CRITICAL: RefundRetryService not available. "
                    + "Refund FAILED and NOT enqueued for retry. OrderId={}, Amount={}, Reason={}. "
                    + "Manual intervention required.",
                order.getId(), order.getTotalValue(), reason);
            return;
        }

        try {
            refundRetryService.enqueueFailedRefund(
                order.getId(),
                order.getTransactionToken(),
                order.getTotalValue(),
                reason
            );
        } catch (Exception e) {
            // If even the enqueue fails, this is a critical alert
            logger.error("⚠️  CRITICAL: Failed to enqueue refund retry. "
                    + "OrderId={}, Amount={}, Reason={}, Error={}. Manual intervention required.",
                order.getId(), order.getTotalValue(), reason, e.getMessage());
        }
    }
}
