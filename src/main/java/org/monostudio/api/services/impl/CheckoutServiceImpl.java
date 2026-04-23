package org.monostudio.api.services.impl;

import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.monostudio.api.models.CartItemPojo;
import org.monostudio.api.models.CartPricingResult;
import org.monostudio.api.models.CheckoutStartRequest;
import org.monostudio.api.models.OrderDetailPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.PaymentRedirectionDetailsPojo;
import org.monostudio.api.models.PaymentResultPojo;
import org.monostudio.api.models.ShippingRateRequestContext;
import org.monostudio.api.services.AdminNotificationService;
import org.monostudio.api.services.CartPricingService;
import org.monostudio.api.services.CheckoutService;
import org.monostudio.api.services.OrdersProcessService;
import org.monostudio.api.services.StockReservationService;
import org.monostudio.api.services.ShippingMethodsService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.CartItem;
import org.monostudio.jpa.entities.CartSession;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.jpa.repositories.CartSessionsRepository;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.jpa.repositories.OrderDetailsRepository;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.repositories.ShippingMethodsRepository;
import org.monostudio.jpa.services.conversion.OrdersConverterService;
import org.monostudio.jpa.services.conversion.ProductsConverterService;
import org.monostudio.jpa.services.crud.CustomersCrudService;
import org.monostudio.jpa.services.crud.OrdersCrudService;
import org.monostudio.jpa.services.predicates.OrdersPredicateService;
import org.monostudio.payment.PaymentService;
import org.monostudio.payment.PaymentServiceException;
import org.monostudio.jpa.entities.PaymentCallbackLog;
import org.monostudio.jpa.entities.PaymentCallbackLog.CallbackResult;
import org.monostudio.jpa.repositories.PaymentCallbackLogRepository;

import jakarta.persistence.EntityNotFoundException;
import java.lang.Math;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.monostudio.config.Constants.ORDER_PAYMENT_STATUS_PAYMENT_STARTED;
@Service
public class CheckoutServiceImpl
    implements CheckoutService {
    private final Logger logger = LoggerFactory.getLogger(CheckoutServiceImpl.class);
    private final OrdersCrudService ordersCrudService;
    private final OrdersProcessService ordersProcessService;
    private final OrdersPredicateService ordersPredicateService;
    private final OrdersConverterService ordersConverterService;
    private final ProductsConverterService productsConverterService;
    private final CustomersCrudService customersCrudService;
    private final ShippingMethodsRepository shippingMethodsRepository;
    private final ProductsRepository productsRepository;
    private final CartSessionsRepository cartSessionsRepository;
    private final OrderDetailsRepository orderDetailsRepository;
    private final OrdersRepository ordersRepository;
    private final Map<String, PaymentService> paymentServices;
    private final StockReservationService stockReservationService;
    private final CartPricingService cartPricingService;
    private final CustomersRepository customersRepository;
    private final ShippingMethodsService shippingMethodsService;
    private final PaymentCallbackLogRepository paymentCallbackLogRepository;
    private final AdminNotificationService adminNotificationService;

    static final double TAX_PERCENT = 0.19;

    @Autowired
    public CheckoutServiceImpl(
        OrdersCrudService ordersCrudService,
        OrdersProcessService ordersProcessService,
        OrdersPredicateService ordersPredicateService,
        OrdersConverterService ordersConverterService,
        ProductsConverterService productsConverterService,
        CustomersCrudService customersCrudService,
        ShippingMethodsRepository shippingMethodsRepository,
        ProductsRepository productsRepository,
        CartSessionsRepository cartSessionsRepository,
        OrderDetailsRepository orderDetailsRepository,
        OrdersRepository ordersRepository,
        Map<String, PaymentService> paymentServices,
        StockReservationService stockReservationService,
        CartPricingService cartPricingService,
        CustomersRepository customersRepository,
        ShippingMethodsService shippingMethodsService,
        PaymentCallbackLogRepository paymentCallbackLogRepository,
        AdminNotificationService adminNotificationService
    ) {
        this.ordersCrudService = ordersCrudService;
        this.ordersProcessService = ordersProcessService;
        this.ordersPredicateService = ordersPredicateService;
        this.ordersConverterService = ordersConverterService;
        this.productsConverterService = productsConverterService;
        this.customersCrudService = customersCrudService;
        this.shippingMethodsRepository = shippingMethodsRepository;
        this.productsRepository = productsRepository;
        this.cartSessionsRepository = cartSessionsRepository;
        this.orderDetailsRepository = orderDetailsRepository;
        this.ordersRepository = ordersRepository;
        this.paymentServices = paymentServices;
        this.stockReservationService = stockReservationService;
        this.cartPricingService = cartPricingService;
        this.customersRepository = customersRepository;
        this.shippingMethodsService = shippingMethodsService;
        this.paymentCallbackLogRepository = paymentCallbackLogRepository;
        this.adminNotificationService = adminNotificationService;
    }

    /**
     * Full checkout flow using CartSession:
     *
     * 1. Fetch cart items from CartSession (validates cart is non-empty)
     * 2. Validate and reserve stock per cart item via StockReservationService
     * 3. Compute subtotal and taxes
     * 4. Resolve and validate shipping method
     * 5. Compute shipping fee (0 if FREE_SHIPPING discount or free threshold met)
     * 6. Validate discount code via DiscountService (increments use count on success)
     * 7. Build OrderPojo with correct transportValue
     * 8. Create order in PENDING status
     * 9. Return payment URL (VNPAY)
     */
    @Override
    @Transactional
    public PaymentRedirectionDetailsPojo startCheckout(CheckoutStartRequest request)
        throws BadInputException, PaymentServiceException {

        // ── 1. Resolve cart session ──────────────────────────────────────────────
        CartSession cart = cartSessionsRepository.findByTokenDeep(request.getSessionToken())
            .orElseThrow(() -> new BadInputException("Cart session not found: " + request.getSessionToken()));
        List<CartItem> cartItems = cart.getItems().stream().toList();
        if (cartItems.isEmpty()) {
            throw new BadInputException("Cart is empty — nothing to checkout");
        }

        // ── 1b. Re-validate stock availability ───────────────────────────────────
        // Stock was reserved at add-to-cart time. Validate it is still available
        // and hasn't expired or been stolen by another checkout session.
        // NOTE: prepareForCheckout also releases any expired reservations first.
        stockReservationService.expireStaleReservations();
        List<CartItemPojo> unavailable = validateCartStockForCheckout(cartItems);
        if (!unavailable.isEmpty()) {
            String unavailableSkus = unavailable.stream()
                .map(CartItemPojo::getVariantSkuResolved)
                .collect(Collectors.joining(", "));
            throw new BadInputException(
                "Some items are no longer available: " + unavailableSkus);
        }

        // ── 2. Validate & reserve stock, compute order details ─────────────────
        List<OrderDetailPojo> orderDetails = resolveCartItems(cartItems, cart.getToken());

        // ── 3. Compute subtotals ─────────────────────────────────────────────────
        int netValue = 0;
        int taxesValue = 0;
        int totalItems = 0;
        for (OrderDetailPojo detail : orderDetails) {
            int unitNet = detail.getUnitValue();
            int unitTax = (int) (unitNet * TAX_PERCENT);
            netValue += (unitNet - unitTax) * detail.getUnits();
            taxesValue += unitTax * detail.getUnits();
            totalItems += detail.getUnits();
        }
        int subtotal = netValue + taxesValue;

        // ── 4. Resolve shipping method ──────────────────────────────────────────
        ShippingMethod shippingMethod = shippingMethodsRepository.findById(request.getShippingMethodId())
            .orElseThrow(() -> new BadInputException("Shipping method not found: " + request.getShippingMethodId()));
        if (!shippingMethod.isActive()) {
            throw new BadInputException("Shipping method is not active: " + shippingMethod.getName());
        }

        // ── 5. Compute shipping fee ─────────────────────────────────────────────
        // Compute fee with distance tracking if applicable
        Double lat = request.getShippingAddress() != null ? request.getShippingAddress().getLatitude() : null;
        Double lng = request.getShippingAddress() != null ? request.getShippingAddress().getLongitude() : null;
        ShippingRateRequestContext shippingRateContext = ShippingRateRequestContext.builder()
            .subtotal(subtotal)
            .latitude(lat)
            .longitude(lng)
            .toDistrictId(request.getShippingAddress() != null ? request.getShippingAddress().getDistrictId() : null)
            .toWardCode(request.getShippingAddress() != null ? request.getShippingAddress().getWardCode() : null)
            .build();
        int shippingFee = shippingMethodsService.computeRate(shippingMethod, shippingRateContext).getFee();

        // ── 6. Pricing (promotion rules + optional coupon) — same logic as POST /public/cart/calculate ──
        Long checkoutCustomerId = resolveCustomerIdForCheckout(request);
        CartPricingResult pricing = cartPricingService.calculateForSession(
            cart, request.getDiscountCode(), checkoutCustomerId
        );

        boolean freeShippingDiscount = Boolean.TRUE.equals(pricing.getFreeShipping());
        if (freeShippingDiscount) {
            shippingFee = 0;
        }

        int discountAmount = pricing.getDiscountAmount() != null ? pricing.getDiscountAmount() : 0;

        // ── 7. Build OrderPojo ───────────────────────────────────────────────────
        OrderPojo orderPojo = OrderPojo.builder()
            .transportValue(shippingFee)
            .netValue(netValue)
            .taxValue(taxesValue)
            .totalItems(totalItems)
            .paymentType(request.getPaymentType())
            .billingType(request.getBillingType())
            .customer(request.getCustomer())
            .shippingAddress(request.getShippingAddress())
            .billingCompany(request.getBillingCompany())
            .billingAddress(request.getBillingAddress())
            .shipper(shippingMethod.getName())
            .details(orderDetails)
            .discountCode(StringUtils.isNotBlank(pricing.getAppliedDiscountCode())
                ? pricing.getAppliedDiscountCode()
                : null)
            .discountValue(discountAmount)
            .cartSessionToken(request.getSessionToken())
            .build();

        // ── 8. Create order ─────────────────────────────────────────────────────
        OrderPojo createdOrder = ordersCrudService.create(orderPojo);
        adminNotificationService.publishOrderCreated(createdOrder);

        // ── 9. Request payment URL ──────────────────────────────────────────────
        PaymentService paymentService = paymentServices.get(createdOrder.getPaymentType());
        if (paymentService == null) {
            throw new BadInputException("Payment method not supported: " + createdOrder.getPaymentType());
        }

        PaymentRedirectionDetailsPojo paymentDetails =
            paymentService.requestNewPaymentPageDetails(createdOrder);

        if ("COD".equals(createdOrder.getPaymentType())) {
            // COD follows Pending -> Paid, Unconfirmed directly (no online payment session).
            createdOrder.setToken(paymentDetails.getToken());
            ordersRepository.setTransactionToken(createdOrder.getId(), paymentDetails.getToken());
            ordersProcessService.markAsPaid(createdOrder);
        } else {
            createdOrder.setToken(paymentDetails.getToken());
            ordersProcessService.markAsStarted(createdOrder);
        }

        logger.info("Checkout started: orderId={}, token={}, total={}, shipping={}, discount={}",
            createdOrder.getBuyOrder(), paymentDetails.getToken(), createdOrder.getTotalValue(), shippingFee, discountAmount);

        return paymentDetails;
    }

    /**
     * Converts CartItems into OrderDetailPojos, reserving stock via StockReservationService.
     */
    private List<OrderDetailPojo> resolveCartItems(List<CartItem> cartItems, String sessionToken) throws BadInputException {
        List<OrderDetailPojo> details = cartItems.stream()
            .map(item -> {
                try {
                    return resolveCartItem(item, sessionToken);
                } catch (BadInputException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList());

        // Validate all items before any reservation is committed (fail-fast)
        // Note: reservations are already done inside resolveCartItem; on exception the
        // transaction rolls back, releasing any partial reservations.
        return details;
    }

    private OrderDetailPojo resolveCartItem(CartItem item, String sessionToken) throws BadInputException {
        ProductVariant variant = item.getVariant();
        int units = item.getQuantity();

        // NOTE: Stock is already reserved at add-to-cart time (CartServiceImpl.addItem).
        // Checkout must NOT re-reserve — doing so would double-count stockReserved.
        // This method only validates variant is still active and has sufficient available stock.
        Integer available = stockReservationService.getAvailableStock(variant.getSku());
        if (available == null || available < units) {
            throw new BadInputException(
                "Insufficient stock for variant '" + variant.getSku()
                    + "': requested " + units + ", available " + Math.max(0, available != null ? available : 0));
        }

        Product product = variant.getProduct();
        int unitValue = product.getPrice() + variant.getPriceModifier();
        String description = units + "x " + product.getName();

        return OrderDetailPojo.builder()
            .units(units)
            .unitValue(unitValue)
            .product(productsConverterService.convertToPojo(product))
            .description(description)
            .variantId(variant.getId())
            .build();
    }

    // ─── Legacy / unchanged methods below ─────────────────────────────────────

    @Override
    public PaymentRedirectionDetailsPojo requestTransactionStart(OrderPojo transaction) throws PaymentServiceException, BadInputException {
        PaymentService paymentService = paymentServices.get(transaction.getPaymentType());
        if (paymentService == null) {
            throw new BadInputException("Payment method not supported: " + transaction.getPaymentType());
        }
        PaymentRedirectionDetailsPojo response = paymentService.requestNewPaymentPageDetails(transaction);
        try {
            transaction.setToken(response.getToken());
            ordersProcessService.markAsStarted(transaction);
            return response;
        } catch (EntityNotFoundException exc) {
            throw new IllegalStateException("The server had a problem requesting the transaction", exc);
        }
    }

    @Override
    public OrderPojo confirmTransaction(String transactionToken, boolean wasAborted)
        throws EntityNotFoundException, PaymentServiceException {
        // P0.4: Idempotency — if this token has already been processed, skip reprocessing.
        // This prevents double-confirm (double stock deduction) and double-abort.
        // The token is the gateway's unique identifier for this payment attempt.
        if (paymentCallbackLogRepository.existsByToken(transactionToken)) {
            OrderPojo existing = this.getOrderWithMatchingToken(transactionToken);
            logger.info("Payment callback token {} already processed (orderId={}) — skipping duplicate callback",
                transactionToken, existing.getBuyOrder());
            return existing;
        }

        OrderPojo sellByToken = this.getSellRequestedWithMatchingToken(transactionToken);
        try {
            OrderPojo result;
            if (wasAborted) {
                result = ordersProcessService.markAsAborted(sellByToken);
                // P0.4: Log abort callback for idempotency
                logCallback(transactionToken, sellByToken.getBuyOrder(),
                    CallbackResult.ABORTED, result.getStatus(), null);
            } else {
                result = this.processSellPaymentStatus(sellByToken, transactionToken);
            }
            return result;
        } catch (BadInputException e) {
            logger.error("Incorrect state of sell, was: {}", sellByToken.getStatus());
            throw new IllegalStateException("Transaction could not be confirmed");
        }
    }

    @Override
    public URI generateResultPageUrl(String transactionToken) {
        OrderPojo order = this.getOrderWithMatchingToken(transactionToken);
        PaymentService paymentService = paymentServices.get(order.getPaymentType());
        try {
            String url = (paymentService.getPaymentResultPageUrl() + "?token=" + transactionToken);
            return new URL(url).toURI();
        } catch (MalformedURLException | URISyntaxException ex) {
            logger.error("Malformed redirection URL; make sure the 'final URL for payment method' property is correctly configured.", ex);
            throw new IllegalStateException("Transaction was confirmed, but server had an unexpected malfunction");
        }
    }

    private OrderPojo processSellPaymentStatus(OrderPojo sellByToken, String transactionToken)
        throws EntityNotFoundException, PaymentServiceException {
        PaymentService paymentService = paymentServices.get(sellByToken.getPaymentType());
        PaymentResultPojo result = paymentService.requestPaymentResultWithAmount(transactionToken);
        OrderPojo outcome;
        CallbackResult callbackResult;
        try {
            if (result.getResponseCode() != 0) {
                outcome = ordersProcessService.markAsFailed(sellByToken);
                callbackResult = CallbackResult.GATEWAY_ERROR;
            } else {
                // CRITICAL: verify the authorized amount matches the order total.
                // This prevents fraud where a lower amount is charged but the order is created for more.
                int authorized = result.getAuthorizedAmount();
                int orderTotal = sellByToken.getTotalValue();
                if (authorized != orderTotal && shouldVerifyAuthorizedAmount(sellByToken.getPaymentType())) {
                    logger.error("Payment amount mismatch for token {}: authorized={}, orderTotal={}",
                        transactionToken, authorized, orderTotal);
                    // Treat as failed — do not mark as paid for a mismatched amount.
                    // Admin must investigate and handle manually.
                    outcome = ordersProcessService.markAsFailed(sellByToken);
                    callbackResult = CallbackResult.ABORTED;
                } else {
                    outcome = ordersProcessService.markAsPaid(sellByToken);
                    callbackResult = CallbackResult.SUCCESS;
                }
            }
        } catch (BadInputException e) {
            logger.error("Incorrect state of sell, was: {}", sellByToken.getStatus());
            throw new IllegalStateException("Transaction could not be confirmed");
        }

        // P0.4: Log this callback so future duplicate callbacks are safely ignored.
        // The existsByToken check at the top of confirmTransaction() prevents reprocessing.
        // We use a try-block because the gateway may have already committed the transaction —
        // failing to log should NOT roll back the payment confirmation.
        try {
            PaymentCallbackLog logEntry = PaymentCallbackLog.builder()
                .token(transactionToken)
                .orderId(sellByToken.getBuyOrder())
                .result(callbackResult)
                .orderStatusAfter(outcome.getStatus())
                .authorizedAmount(result.getAuthorizedAmount())
                .build();
            paymentCallbackLogRepository.saveAndFlush(logEntry);
        } catch (Exception e) {
            // Log but do NOT fail — the payment has already been processed.
            // Worst case: next duplicate callback will reprocess, but markAsPaid preconditions
            // will reject it if the order is already PAID_UNCONFIRMED.
            logger.error("Failed to log payment callback for token {}: {}",
                transactionToken, e.getMessage());
        }

        return outcome;
    }

    @Override
    public OrderPojo getOrderByToken(String token) throws EntityNotFoundException {
        return this.getOrderWithMatchingToken(token);
    }

    @Override
    public org.monostudio.payment.PaymentService getPaymentService(String paymentType) {
        return paymentServices.get(paymentType);
    }

    private OrderPojo getSellRequestedWithMatchingToken(String transactionToken) throws EntityNotFoundException {
        Map<String, String> startedWithTokenMatcher = new HashMap<>(Map.of(
            "paymentStatus", ORDER_PAYMENT_STATUS_PAYMENT_STARTED,
            "token", transactionToken));
        Predicate startedTransactionWithMatchingToken = ordersPredicateService.parseMap(startedWithTokenMatcher);
        return ordersCrudService.readOne(startedTransactionWithMatchingToken);
    }

    private OrderPojo getOrderWithMatchingToken(String transactionToken) throws EntityNotFoundException {
        Order order = ordersRepository.findByTransactionToken(transactionToken)
            .orElseThrow(() -> new EntityNotFoundException("Order with transaction token not found: " + transactionToken));
        return ordersConverterService.convertToPojo(order);
    }

    private Long resolveCustomerIdForCheckout(CheckoutStartRequest request) {
        if (request.getCustomer() == null || StringUtils.isBlank(request.getCustomer().getEmail())) {
            return null;
        }
        var matches = customersRepository.findAllByPersonEmail(request.getCustomer().getEmail().trim());
        return matches.isEmpty() ? null : matches.get(0).getId();
    }

    /**
     * Validates that all cart items still have sufficient available stock at checkout time.
     * Differs from CartServiceImpl.validateCartStock in that it checks reservation availability
     * (stockCurrent - stockReserved), not just variant active status.
     *
     * @param cartItems Active cart items
     * @return List of unavailable items (empty = all OK)
     */
    private List<CartItemPojo> validateCartStockForCheckout(List<CartItem> cartItems) {
        return cartItems.stream()
            .filter(item -> {
                ProductVariant variant = item.getVariant();
                Integer available = stockReservationService.getAvailableStock(variant.getSku());
                return available == null || available < item.getQuantity() || !variant.isActive();
            })
            .map(item -> CartItemPojo.builder()
                .variantSkuResolved(item.getVariant().getSku())
                .quantity(item.getQuantity())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * P0.4: Logs a payment callback for idempotency tracking.
     * Failures are swallowed — callback processing must not fail due to logging.
     */
    private void logCallback(String token, Long orderId,
                             CallbackResult result, String orderStatusAfter, Integer authorizedAmount) {
        try {
            PaymentCallbackLog logEntry = PaymentCallbackLog.builder()
                .token(token)
                .orderId(orderId)
                .result(result)
                .orderStatusAfter(orderStatusAfter)
                .authorizedAmount(authorizedAmount)
                .build();
            paymentCallbackLogRepository.saveAndFlush(logEntry);
        } catch (Exception e) {
            logger.error("Failed to log payment callback for token {}: {}",
                token, e.getMessage());
        }
    }

    private boolean shouldVerifyAuthorizedAmount(String paymentType) {
        return !"COD".equalsIgnoreCase(paymentType) && !"VNPAY".equalsIgnoreCase(paymentType);
    }
}
