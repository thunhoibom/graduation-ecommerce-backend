package org.monostudio.api.services.impl;

import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.CheckoutStartRequest;
import org.monostudio.api.models.DiscountValidationResult;
import org.monostudio.api.models.OrderDetailPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.PaymentRedirectionDetailsPojo;
import org.monostudio.api.services.CheckoutService;
import org.monostudio.api.services.DiscountService;
import org.monostudio.api.services.OrdersProcessService;
import org.monostudio.api.services.StockReservationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.CartItem;
import org.monostudio.jpa.entities.CartSession;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.jpa.repositories.CartSessionsRepository;
import org.monostudio.jpa.repositories.OrderDetailsRepository;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.repositories.ShippingMethodsRepository;
import org.monostudio.jpa.services.conversion.OrdersConverterService;
import org.monostudio.jpa.services.conversion.ProductsConverterService;
import org.monostudio.jpa.services.crud.CustomersCrudService;
import org.monostudio.jpa.services.crud.OrdersCrudService;
import org.monostudio.jpa.services.predicates.OrdersPredicateService;
import org.monostudio.payment.PaymentService;
import org.monostudio.payment.PaymentServiceException;

import jakarta.persistence.EntityNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.monostudio.config.Constants.ORDER_STATUS_PAYMENT_STARTED;
import static org.monostudio.jpa.entities.DiscountCode.TYPE_FREE_SHIPPING;

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
    private final PaymentService paymentIntegrationService;
    private final StockReservationService stockReservationService;
    private final DiscountService discountService;

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
        PaymentService paymentIntegrationService,
        StockReservationService stockReservationService,
        DiscountService discountService
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
        this.paymentIntegrationService = paymentIntegrationService;
        this.stockReservationService = stockReservationService;
        this.discountService = discountService;
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
     * 9. Return payment URL (Webpay Plus)
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

        // ── 2. Validate & reserve stock, compute order details ─────────────────
        List<OrderDetailPojo> orderDetails = resolveCartItems(cartItems);

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
        // Base free-threshold check first
        int shippingFee = computeShippingFee(shippingMethod, subtotal);

        // ── 6. Validate and redeem discount ─────────────────────────────────────
        DiscountValidationResult discountResult = discountService.validateDiscount(
            request.getDiscountCode(), subtotal
        );
        if (!discountResult.isValid()) {
            throw new BadInputException("Discount error: " + discountResult.getMessage());
        }

        // FREE_SHIPPING: override shipping fee to 0 after base free-threshold check
        boolean freeShippingDiscount = TYPE_FREE_SHIPPING.equals(discountResult.getType());
        if (freeShippingDiscount) {
            shippingFee = 0;
        }

        int discountAmount = discountResult.getDiscountAmount();

        // Redeem discount (increment use count) — only after all validations pass
        Long customerId = null; // TODO: resolve from authenticated user / request
        discountService.redeemDiscount(request.getDiscountCode(), subtotal, customerId);

        // ── 7. Build OrderPojo ───────────────────────────────────────────────────
        int totalValue = subtotal + shippingFee - discountAmount;

        OrderPojo orderPojo = OrderPojo.builder()
            .netValue(netValue)
            .taxValue(taxesValue)
            .transportValue(shippingFee)
            .totalValue(Math.max(0, totalValue))
            .totalItems(totalItems)
            .paymentType(request.getPaymentType())
            .billingType(request.getBillingType())
            .customer(request.getCustomer())
            .shippingAddress(request.getShippingAddress())
            .billingCompany(request.getBillingCompany())
            .billingAddress(request.getBillingAddress())
            .shipper(shippingMethod.getName())
            .details(orderDetails)
            .build();

        // ── 8. Create order ─────────────────────────────────────────────────────
        OrderPojo createdOrder = ordersCrudService.create(orderPojo);

        // ── 9. Request payment URL ──────────────────────────────────────────────
        PaymentRedirectionDetailsPojo paymentDetails =
            paymentIntegrationService.requestNewPaymentPageDetails(createdOrder);

        createdOrder.setToken(paymentDetails.getToken());
        ordersProcessService.markAsStarted(createdOrder);

        logger.info("Checkout started: orderId={}, token={}, total={}, shipping={}, discount={}",
            createdOrder.getBuyOrder(), paymentDetails.getToken(), totalValue, shippingFee, discountAmount);

        return paymentDetails;
    }

    /**
     * Converts CartItems into OrderDetailPojos, reserving stock via StockReservationService.
     */
    private List<OrderDetailPojo> resolveCartItems(List<CartItem> cartItems) throws BadInputException {
        List<OrderDetailPojo> details = cartItems.stream()
            .map(this::resolveCartItem)
            .collect(Collectors.toList());

        // Validate all items before any reservation is committed (fail-fast)
        // Note: reservations are already done inside resolveCartItem; on exception the
        // transaction rolls back, releasing any partial reservations.
        return details;
    }

    private OrderDetailPojo resolveCartItem(CartItem item) throws BadInputException {
        ProductVariant variant = item.getVariant();
        int units = item.getQuantity();

        // Reserve stock for the duration of checkout
        stockReservationService.reserveStock(variant.getId(), units);

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

    /**
     * Computes shipping fee: 0 if subtotal >= freeShippingThreshold, else baseFee.
     * Note: FREE_SHIPPING discount type is handled separately in startCheckout().
     */
    private int computeShippingFee(ShippingMethod method, int subtotal) {
        if (method.getFreeShippingThreshold() != null && subtotal >= method.getFreeShippingThreshold()) {
            return 0;
        }
        return method.getBaseFee();
    }

    // ─── Legacy / unchanged methods below ─────────────────────────────────────

    @Override
    public PaymentRedirectionDetailsPojo requestTransactionStart(OrderPojo transaction) throws PaymentServiceException, BadInputException {
        PaymentRedirectionDetailsPojo response = paymentIntegrationService.requestNewPaymentPageDetails(transaction);
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
        OrderPojo sellByToken = this.getSellRequestedWithMatchingToken(transactionToken);
        try {
            if (wasAborted) {
                return ordersProcessService.markAsAborted(sellByToken);
            } else {
                return this.processSellPaymentStatus(sellByToken);
            }
        } catch (BadInputException e) {
            logger.error("Incorrect state of sell, was: {}", sellByToken.getStatus());
            throw new IllegalStateException("Transaction could not be confirmed");
        }
    }

    @Override
    public URI generateResultPageUrl(String transactionToken) {
        try {
            String url = (paymentIntegrationService.getPaymentResultPageUrl() + "?token=" + transactionToken);
            return new URL(url).toURI();
        } catch (MalformedURLException | URISyntaxException ex) {
            logger.error("Malformed redirection URL; make sure the 'final URL for payment method' property is correctly configured.", ex);
            throw new IllegalStateException("Transaction was confirmed, but server had an unexpected malfunction");
        }
    }

    private OrderPojo processSellPaymentStatus(OrderPojo sellByToken)
        throws EntityNotFoundException, PaymentServiceException {
        int statusCode = paymentIntegrationService.requestPaymentResult(sellByToken.getToken());
        try {
            if (statusCode != 0) {
                return ordersProcessService.markAsFailed(sellByToken);
            } else {
                return ordersProcessService.markAsPaid(sellByToken);
            }
        } catch (BadInputException e) {
            logger.error("Incorrect state of sell, was: {}", sellByToken.getStatus());
            throw new IllegalStateException("Transaction could not be confirmed");
        }
    }

    private OrderPojo getSellRequestedWithMatchingToken(String transactionToken) throws EntityNotFoundException {
        Map<String, String> startedWithTokenMatcher = new HashMap<>(Map.of(
            "statusCode", ORDER_STATUS_PAYMENT_STARTED,
            "token", transactionToken));
        Predicate startedTransactionWithMatchingToken = ordersPredicateService.parseMap(startedWithTokenMatcher);
        return ordersCrudService.readOne(startedTransactionWithMatchingToken);
    }
}
