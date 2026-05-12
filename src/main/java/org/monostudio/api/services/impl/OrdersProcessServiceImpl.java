package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
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
import org.monostudio.api.services.LoyaltyService;
import org.monostudio.api.services.OrdersProcessService;
import org.monostudio.api.services.StockReservationService;
import org.monostudio.api.services.ShipmentOrchestratorService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.OrderDetail;
import org.monostudio.jpa.entities.StockAdjustment;
import org.monostudio.jpa.repositories.CartItemsRepository;
import org.monostudio.jpa.repositories.CartSessionsRepository;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.OrderDetailsRepository;
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

import static org.monostudio.config.Constants.ORDER_FULFILLMENT_STATUS_COMPLETED;
import static org.monostudio.config.Constants.ORDER_FULFILLMENT_STATUS_CONFIRMED;
import static org.monostudio.config.Constants.ORDER_FULFILLMENT_STATUS_DELIVERY_CANCELLED;
import static org.monostudio.config.Constants.ORDER_FULFILLMENT_STATUS_DELIVERY_FAILED;
import static org.monostudio.config.Constants.ORDER_FULFILLMENT_STATUS_DELIVERY_ON_ROUTE;
import static org.monostudio.config.Constants.ORDER_FULFILLMENT_STATUS_PENDING;
import static org.monostudio.config.Constants.ORDER_FULFILLMENT_STATUS_PROCESSING;
import static org.monostudio.config.Constants.ORDER_FULFILLMENT_STATUS_REJECTED;
import static org.monostudio.config.Constants.ORDER_FULFILLMENT_STATUS_RETURNED;
import static org.monostudio.config.Constants.ORDER_PAYMENT_STATUS_PAID;
import static org.monostudio.config.Constants.ORDER_PAYMENT_STATUS_PAYMENT_CANCELLED;
import static org.monostudio.config.Constants.ORDER_PAYMENT_STATUS_PAYMENT_FAILED;
import static org.monostudio.config.Constants.ORDER_PAYMENT_STATUS_PAYMENT_STARTED;
import static org.monostudio.config.Constants.ORDER_PAYMENT_STATUS_REFUNDED;
import static org.monostudio.config.Constants.ORDER_PAYMENT_STATUS_UNPAID;
import static org.monostudio.config.Constants.LOYALTY_EVENT_REVERSE_REJECTED;
import static org.monostudio.config.Constants.LOYALTY_EVENT_REVERSE_RETURNED;
import org.monostudio.config.cache.CacheNames;

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
    private final OrdersConverterService converterService;
    private final ProductsConverterService productConverterService;
    private final KafkaMailProducer kafkaMailProducer;
    private final KafkaOrderProducer kafkaOrderProducer;
    private final StockReservationService stockReservationService;
    private final DiscountService discountService;
    private final Map<String, PaymentService> paymentServices;
    private final RefundRetryService refundRetryService;
    private final LoyaltyService loyaltyService;
    private final CartSessionsRepository cartSessionsRepository;
    private final CartItemsRepository cartItemsRepository;
    private final ShipmentOrchestratorService shipmentOrchestratorService;

    public OrdersProcessServiceImpl(
        OrdersCrudService crudService,
        OrdersRepository ordersRepository,
        OrderDetailsRepository orderDetailsRepository,
        OrdersConverterService converterService,
        ProductsConverterService productConverterService,
        KafkaMailProducer kafkaMailProducer,
        KafkaOrderProducer kafkaOrderProducer,
        StockReservationService stockReservationService,
        DiscountService discountService,
        @Autowired(required = false) Map<String, PaymentService> paymentServices,
        @Autowired(required = false) RefundRetryService refundRetryService,
        LoyaltyService loyaltyService,
        CartSessionsRepository cartSessionsRepository,
        CartItemsRepository cartItemsRepository,
        @Autowired(required = false) ShipmentOrchestratorService shipmentOrchestratorService
    ) {
        this.crudService = crudService;
        this.ordersRepository = ordersRepository;
        this.orderDetailsRepository = orderDetailsRepository;
        this.converterService = converterService;
        this.productConverterService = productConverterService;
        this.kafkaMailProducer = kafkaMailProducer;
        this.kafkaOrderProducer = kafkaOrderProducer;
        this.stockReservationService = stockReservationService;
        this.discountService = discountService;
        this.paymentServices = paymentServices;
        this.refundRetryService = refundRetryService;
        this.loyaltyService = loyaltyService;
        this.cartSessionsRepository = cartSessionsRepository;
        this.cartItemsRepository = cartItemsRepository;
        this.shipmentOrchestratorService = shipmentOrchestratorService;
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
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_REVENUE_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_TOP_PRODUCTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, allEntries = true)
    })
    public OrderPojo markAsStarted(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!ORDER_FULFILLMENT_STATUS_PENDING.equals(existingOrder.getFulfillmentStatus())
            || !ORDER_PAYMENT_STATUS_UNPAID.equals(existingOrder.getPaymentStatus())) {
            // P0.5: Reject if order is not in PENDING — cannot restart payment for an already-started order.
            throw new BadInputException(
                "Cannot start payment for order " + existingOrder.getId()
                    + " — current fulfillment/payment is '"
                    + existingOrder.getFulfillmentStatus() + "/" + existingOrder.getPaymentStatus()
                    + "', expected '" + ORDER_FULFILLMENT_STATUS_PENDING + "/" + ORDER_PAYMENT_STATUS_UNPAID + "'.");
        }

        ordersRepository.setPaymentStatus(existingOrder.getId(), ORDER_PAYMENT_STATUS_PAYMENT_STARTED);
        ordersRepository.setTransactionToken(existingOrder.getId(), sell.getToken());

        OrderPojo target = this.convertOrThrowException(existingOrder);
        target.setStatus(target.getFulfillmentStatus());
        target.setPaymentStatus(ORDER_PAYMENT_STATUS_PAYMENT_STARTED);
        kafkaOrderProducer.publishOrderPaymentStarted(existingOrder.getId(), existingOrder.getCartSessionToken());
        sendClientEmail(target);
        return target;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_REVENUE_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_TOP_PRODUCTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, allEntries = true)
    })
    public OrderPojo markAsAborted(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!ORDER_PAYMENT_STATUS_PAYMENT_STARTED.equals(existingOrder.getPaymentStatus())) {
            // P0.5: If order is not in PAYMENT_STARTED, it has already been processed.
            // This can happen when the gateway retries a callback after the first succeeded.
            // The order may already be PAID, CANCELLED, or FAILED — do not reprocess.
            throw new BadInputException(
                "Cannot abort order " + existingOrder.getId()
                    + " — current payment status is '" + existingOrder.getPaymentStatus()
                    + "', expected '" + ORDER_PAYMENT_STATUS_PAYMENT_STARTED + "'. "
                    + "Possible duplicate callback — order may have already been processed.");
        }

        ordersRepository.setPaymentStatus(existingOrder.getId(), ORDER_PAYMENT_STATUS_PAYMENT_CANCELLED);

        OrderPojo target = this.convertOrThrowException(existingOrder);
        target.setStatus(target.getFulfillmentStatus());
        target.setPaymentStatus(ORDER_PAYMENT_STATUS_PAYMENT_CANCELLED);

        // Release stock reservations since payment was cancelled
        if (existingOrder.getCartSessionToken() != null) {
            stockReservationService.release(existingOrder.getCartSessionToken());
        }

        sendClientEmail(target);
        kafkaOrderProducer.publishOrderAborted(existingOrder.getId(), existingOrder.getCartSessionToken());
        return target;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_REVENUE_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_TOP_PRODUCTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, allEntries = true)
    })
    public OrderPojo markAsFailed(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!ORDER_PAYMENT_STATUS_PAYMENT_STARTED.equals(existingOrder.getPaymentStatus())) {
            throw new BadInputException(
                "Cannot mark order " + existingOrder.getId() + " as failed"
                    + " — current payment status is '" + existingOrder.getPaymentStatus()
                    + "', expected '" + ORDER_PAYMENT_STATUS_PAYMENT_STARTED + "'. "
                    + "Possible duplicate callback.");
        }

        ordersRepository.setPaymentStatus(existingOrder.getId(), ORDER_PAYMENT_STATUS_PAYMENT_FAILED);

        OrderPojo target = this.convertOrThrowException(existingOrder);
        target.setStatus(target.getFulfillmentStatus());
        target.setPaymentStatus(ORDER_PAYMENT_STATUS_PAYMENT_FAILED);

        // Release stock reservations since payment failed
        if (existingOrder.getCartSessionToken() != null) {
            stockReservationService.release(existingOrder.getCartSessionToken());
        }

        sendClientEmail(target);
        kafkaOrderProducer.publishOrderFailed(existingOrder.getId(), existingOrder.getCartSessionToken());
        return target;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_REVENUE_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_TOP_PRODUCTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, allEntries = true)
    })
    public OrderPojo markAsPaid(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);
        String currentPaymentStatus = existingOrder.getPaymentStatus();
        String paymentTypeName = existingOrder.getPaymentType() != null ? existingOrder.getPaymentType().getName() : null;
        boolean isCodPendingTransition =
            ORDER_PAYMENT_STATUS_UNPAID.equals(currentPaymentStatus) && "COD".equalsIgnoreCase(paymentTypeName);

        if (!ORDER_PAYMENT_STATUS_PAYMENT_STARTED.equals(currentPaymentStatus) && !isCodPendingTransition) {
            // P0.5: Reject if order is not in PAYMENT_STARTED.
            // This guards against race conditions and duplicate webhook callbacks.
            // Note: PaymentCallbackLog in CheckoutServiceImpl is the primary defense;
            // this is the secondary defense at the service layer.
            throw new BadInputException(
                "Cannot mark order " + existingOrder.getId() + " as paid"
                    + " — current payment status is '" + existingOrder.getPaymentStatus()
                    + "', expected '" + ORDER_PAYMENT_STATUS_PAYMENT_STARTED + "'"
                    + " (or '" + ORDER_PAYMENT_STATUS_UNPAID + "' for COD). "
                    + "This may be a duplicate payment callback. "
                    + "If the order should already be PAID, no action is needed.");
        }

        ordersRepository.setPaymentStatus(existingOrder.getId(), ORDER_PAYMENT_STATUS_PAID);

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
        target.setStatus(target.getFulfillmentStatus());
        target.setPaymentStatus(ORDER_PAYMENT_STATUS_PAID);
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

        // Loyalty accrual should not block order status progression in case of transient errors.
        try {
            loyaltyService.awardForPaidOrder(existingOrder.getId());
        } catch (RuntimeException e) {
            logger.error("Failed to award loyalty points for order {}: {}", existingOrder.getId(), e.getMessage());
        }

        sendClientEmail(target);
        sendOwnerEmail(target);
        // Publish ORDER_PAID event → KafkaOrderConsumer clears the cart session asynchronously
        kafkaOrderProducer.publishOrderPaid(existingOrder.getId(), existingOrder.getCartSessionToken());

        return target;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_REVENUE_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_TOP_PRODUCTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, allEntries = true)
    })
    public OrderPojo markAsConfirmed(OrderPojo sell)
        throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!ORDER_FULFILLMENT_STATUS_PENDING.equals(existingOrder.getFulfillmentStatus())
            || !ORDER_PAYMENT_STATUS_PAID.equals(existingOrder.getPaymentStatus())) {
            throw new BadInputException(THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION);
        }

        ordersRepository.setFulfillmentStatus(existingOrder.getId(), ORDER_FULFILLMENT_STATUS_PROCESSING);

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
        target.setStatus(ORDER_FULFILLMENT_STATUS_PROCESSING);
        target.setFulfillmentStatus(ORDER_FULFILLMENT_STATUS_PROCESSING);

        if (shipmentOrchestratorService != null) {
            shipmentOrchestratorService.requestShipmentCreation(existingOrder.getId());
        }

        sendClientEmail(target);
        kafkaOrderProducer.publishOrderConfirmed(existingOrder.getId());

        return target;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_REVENUE_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_TOP_PRODUCTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, allEntries = true)
    })
    public OrderPojo markAsRejected(OrderPojo sell)
        throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!ORDER_FULFILLMENT_STATUS_PENDING.equals(existingOrder.getFulfillmentStatus())
            || !ORDER_PAYMENT_STATUS_PAID.equals(existingOrder.getPaymentStatus())) {
            throw new BadInputException(THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION);
        }

        // P2: Fetch lazy-loaded fields BEFORE status update clears the persistence context
        String paymentTypeName = existingOrder.getPaymentType() != null ? existingOrder.getPaymentType().getName() : null;

        ordersRepository.setFulfillmentStatus(existingOrder.getId(), ORDER_FULFILLMENT_STATUS_REJECTED);
        ordersRepository.setPaymentStatus(existingOrder.getId(), ORDER_PAYMENT_STATUS_REFUNDED);

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
        target.setStatus(ORDER_FULFILLMENT_STATUS_REJECTED);
        target.setFulfillmentStatus(ORDER_FULFILLMENT_STATUS_REJECTED);
        target.setPaymentStatus(ORDER_PAYMENT_STATUS_REFUNDED);

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

        try {
            loyaltyService.reverseForOrder(existingOrder.getId(), LOYALTY_EVENT_REVERSE_REJECTED);
        } catch (RuntimeException e) {
            logger.error("Failed to reverse loyalty points for rejected order {}: {}", existingOrder.getId(), e.getMessage());
        }

        sendClientEmail(target);
        sendOwnerEmail(target);
        kafkaOrderProducer.publishOrderRejected(existingOrder.getId());

        return target;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_REVENUE_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_TOP_PRODUCTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, allEntries = true)
    })
    public OrderPojo markAsCompleted(OrderPojo sell)
        throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!ORDER_FULFILLMENT_STATUS_DELIVERY_ON_ROUTE.equals(existingOrder.getFulfillmentStatus())) {
            throw new BadInputException(THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION);
        }

        ordersRepository.setFulfillmentStatus(existingOrder.getId(), ORDER_FULFILLMENT_STATUS_COMPLETED);

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
        target.setStatus(ORDER_FULFILLMENT_STATUS_COMPLETED);
        target.setFulfillmentStatus(ORDER_FULFILLMENT_STATUS_COMPLETED);

        sendClientEmail(target);
        kafkaOrderProducer.publishOrderCompleted(existingOrder.getId());

        return target;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_REVENUE_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_TOP_PRODUCTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, allEntries = true)
    })
    public OrderPojo markAsDeliveryOnRoute(OrderPojo sell)
        throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);
        String currentStatus = existingOrder.getFulfillmentStatus();
        boolean canHandover =
            ORDER_FULFILLMENT_STATUS_PROCESSING.equals(currentStatus)
                || ORDER_FULFILLMENT_STATUS_CONFIRMED.equals(currentStatus); // Legacy/Backup support
        if (!canHandover) {
            throw new BadInputException(
                "Cannot move order " + existingOrder.getId()
                    + " to '" + ORDER_FULFILLMENT_STATUS_DELIVERY_ON_ROUTE + "'"
                    + " — current status is '" + currentStatus
                    + "', expected '" + ORDER_FULFILLMENT_STATUS_PROCESSING + "'.");
        }

        ordersRepository.setFulfillmentStatus(existingOrder.getId(), ORDER_FULFILLMENT_STATUS_DELIVERY_ON_ROUTE);
        OrderPojo target = this.convertOrThrowException(existingOrder);
        target.setStatus(ORDER_FULFILLMENT_STATUS_DELIVERY_ON_ROUTE);
        target.setFulfillmentStatus(ORDER_FULFILLMENT_STATUS_DELIVERY_ON_ROUTE);
        sendClientEmail(target);
        sendOwnerEmail(target);
        return target;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_REVENUE_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_TOP_PRODUCTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, allEntries = true)
    })
    public OrderPojo markAsDeliveryFailed(OrderPojo sell)
        throws BadInputException, EntityNotFoundException {
        return moveStatus(
            sell,
            ORDER_FULFILLMENT_STATUS_DELIVERY_ON_ROUTE,
            ORDER_FULFILLMENT_STATUS_DELIVERY_FAILED
        );
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_REVENUE_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_TOP_PRODUCTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, allEntries = true)
    })
    public OrderPojo markAsDeliveryCancelled(OrderPojo sell)
        throws BadInputException, EntityNotFoundException {
        return moveStatus(
            sell,
            ORDER_FULFILLMENT_STATUS_DELIVERY_ON_ROUTE,
            ORDER_FULFILLMENT_STATUS_DELIVERY_CANCELLED
        );
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_REVENUE_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_TOP_PRODUCTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, allEntries = true)
    })
    public OrderPojo markAsReturned(OrderPojo sell)
        throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);
        String currentStatus = existingOrder.getFulfillmentStatus();
        boolean canReturn =
            ORDER_FULFILLMENT_STATUS_DELIVERY_FAILED.equals(currentStatus)
                || ORDER_FULFILLMENT_STATUS_DELIVERY_CANCELLED.equals(currentStatus)
                || ORDER_FULFILLMENT_STATUS_COMPLETED.equals(currentStatus);
        if (!canReturn) {
            throw new BadInputException(
                "Cannot mark order " + existingOrder.getId() + " as returned"
                    + " — current status is '" + currentStatus + "'.");
        }

        ordersRepository.setFulfillmentStatus(existingOrder.getId(), ORDER_FULFILLMENT_STATUS_RETURNED);
        OrderPojo target = this.convertOrThrowException(existingOrder);
        target.setStatus(ORDER_FULFILLMENT_STATUS_RETURNED);
        target.setFulfillmentStatus(ORDER_FULFILLMENT_STATUS_RETURNED);
        try {
            loyaltyService.reverseForOrder(existingOrder.getId(), LOYALTY_EVENT_REVERSE_RETURNED);
        } catch (RuntimeException e) {
            logger.error("Failed to reverse loyalty points for returned order {}: {}", existingOrder.getId(), e.getMessage());
        }
        sendClientEmail(target);
        sendOwnerEmail(target);
        return target;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_REVENUE_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_TOP_PRODUCTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, allEntries = true)
    })
    public OrderPojo markAsAdminCancelled(OrderPojo sell, String reason)
        throws BadInputException, EntityNotFoundException {
        Order existingOrder = fetchExistingOrThrowException(sell);

        String currentStatus = existingOrder.getFulfillmentStatus();
        if (!ORDER_FULFILLMENT_STATUS_DELIVERY_ON_ROUTE.equals(currentStatus)) {
            throw new BadInputException(
                "Cannot recall order in status '" + currentStatus + "'");
        }

        logger.info("Order {} requested delivery recall. Reason: {}", existingOrder.getId(), reason);
        return markAsDeliveryCancelled(sell);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_REVENUE_STATS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_TOP_PRODUCTS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, allEntries = true)
    })
    public int expireStalePaymentSessions() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.MINUTES);
        List<Order> stale = ordersRepository.findByPaymentStatusAndDateBefore(
            ORDER_PAYMENT_STATUS_PAYMENT_STARTED, cutoff);

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

    private OrderPojo moveStatus(OrderPojo sell, String fromStatus, String toStatus)
        throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);
        if (!existingOrder.getFulfillmentStatus().equals(fromStatus)) {
            throw new BadInputException(
                "Cannot move order " + existingOrder.getId()
                    + " to '" + toStatus + "'"
                    + " — current status is '" + existingOrder.getFulfillmentStatus()
                    + "', expected '" + fromStatus + "'.");
        }

        ordersRepository.setFulfillmentStatus(existingOrder.getId(), toStatus);
        OrderPojo target = this.convertOrThrowException(existingOrder);
        target.setStatus(toStatus);
        target.setFulfillmentStatus(toStatus);
        sendClientEmail(target);
        sendOwnerEmail(target);
        return target;
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
