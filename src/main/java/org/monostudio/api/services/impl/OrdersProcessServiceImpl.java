package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.api.models.OrderDetailPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.mailing.MailingService;
import org.monostudio.mailing.MailingServiceException;
import org.monostudio.api.services.DiscountService;
import org.monostudio.api.services.OrdersProcessService;
import org.monostudio.api.services.StockReservationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.OrderDetail;
import org.monostudio.jpa.entities.OrderStatus;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.OrderDetailsRepository;
import org.monostudio.jpa.repositories.OrderStatusesRepository;
import org.monostudio.jpa.services.conversion.ProductsConverterService;
import org.monostudio.jpa.services.conversion.OrdersConverterService;
import org.monostudio.jpa.services.crud.OrdersCrudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final MailingService mailingService;
    private final StockReservationService stockReservationService;
    private final DiscountService discountService;

    public OrdersProcessServiceImpl(
        OrdersCrudService crudService,
        OrdersRepository ordersRepository,
        OrderDetailsRepository orderDetailsRepository,
        OrderStatusesRepository orderStatusesRepository,
        OrdersConverterService converterService,
        ProductsConverterService productConverterService,
        @Autowired(required = false) MailingService mailingService,
        StockReservationService stockReservationService,
        DiscountService discountService
    ) {
        this.crudService = crudService;
        this.ordersRepository = ordersRepository;
        this.orderDetailsRepository = orderDetailsRepository;
        this.orderStatusesRepository = orderStatusesRepository;
        this.converterService = converterService;
        this.productConverterService = productConverterService;
        this.mailingService = mailingService;
        this.stockReservationService = stockReservationService;
        this.discountService = discountService;
    }

    private void sendClientEmail(OrderPojo order) {
        if (mailingService != null) {
            try {
                mailingService.notifyOrderStatusToClient(order);
            } catch (MailingServiceException e) {
                logger.warn("Failed to send order status email to client for order {}: {}",
                    order.getBuyOrder(), e.getMessage());
            }
        }
    }

    private void sendOwnerEmail(OrderPojo order) {
        if (mailingService != null) {
            try {
                mailingService.notifyOrderStatusToOwners(order);
            } catch (MailingServiceException e) {
                logger.warn("Failed to send order status email to owners for order {}: {}",
                    order.getBuyOrder(), e.getMessage());
            }
        }
    }

    // TODO figure out how to shorten below methods
    // TODO to compare statuses use numbers, not strings
    @Override
    public OrderPojo markAsStarted(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!existingOrder.getStatus().getName().equals(ORDER_STATUS_PENDING)) {
            throw new BadInputException(THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION);
        }

        Optional<OrderStatus> startedStatus = orderStatusesRepository.findByName(ORDER_STATUS_PAYMENT_STARTED);
        if (startedStatus.isEmpty()) {
            throw new IllegalStateException(NO_STATUS_MATCHES_THE + " '" + ORDER_STATUS_PAYMENT_STARTED + "' " + NAME_IS_THE_DATABASE_EMPTY_OR_CORRUPT);
        }
        ordersRepository.setStatus(existingOrder.getId(), startedStatus.get());
        ordersRepository.setTransactionToken(existingOrder.getId(), sell.getToken());

        OrderPojo target = this.convertOrThrowException(existingOrder);
        target.setStatus(ORDER_STATUS_PAYMENT_STARTED);
        sendClientEmail(target);
        return target;
    }

    @Override
    public OrderPojo markAsAborted(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!existingOrder.getStatus().getName().equals(ORDER_STATUS_PAYMENT_STARTED)) {
            throw new BadInputException(THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION);
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
        return target;
    }

    @Override
    public OrderPojo markAsFailed(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!existingOrder.getStatus().getName().equals(ORDER_STATUS_PAYMENT_STARTED)) {
            throw new BadInputException(THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION);
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
        return target;
    }

    @Override
    public OrderPojo markAsPaid(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        Order existingOrder = this.fetchExistingOrThrowException(sell);

        if (!existingOrder.getStatus().getName().equals(ORDER_STATUS_PAYMENT_STARTED)) {
            throw new BadInputException(THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION);
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
            String variantSku = detail.getProductVariant() != null
                ? detail.getProductVariant().getSku() : null;
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

        // Confirm stock reservations per-variant (not the whole session)
        // This prevents double-deduction if the same cart is checked out twice
        if (existingOrder.getCartSessionToken() != null) {
            for (OrderDetail detail : orderDetailsRepository.findBySellId(existingOrder.getId())) {
                if (detail.getProductVariant() != null) {
                    stockReservationService.confirmItem(
                        existingOrder.getCartSessionToken(),
                        detail.getProductVariant().getSku()
                    );
                }
            }
        }

        // Redeem discount only after payment is confirmed — not when checkout starts.
        // This prevents "burning" a discount code when the customer abandons or fails payment.
        if (existingOrder.getDiscountCode() != null && !existingOrder.getDiscountCode().isBlank()) {
            int subtotal = existingOrder.getNetValue() + existingOrder.getTaxesValue();
            Long customerId = (existingOrder.getCustomer() != null) ? existingOrder.getCustomer().getId() : null;
            try {
                discountService.redeemDiscount(existingOrder.getDiscountCode(), subtotal, customerId);
            } catch (BadInputException e) {
                // Payment already succeeded — log but do not roll back the order.
                // Admin can manually adjust the discount usage count if needed.
                logger.warn("Failed to redeem discount code '{}' for order {}: {}",
                    existingOrder.getDiscountCode(), existingOrder.getId(), e.getMessage());
            }
        }

        sendClientEmail(target);
        sendOwnerEmail(target);

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

        sendClientEmail(target);
        sendOwnerEmail(target);

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
}
