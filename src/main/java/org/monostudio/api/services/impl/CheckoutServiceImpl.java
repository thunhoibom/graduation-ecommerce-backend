package org.monostudio.api.services.impl;

import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.PaymentRedirectionDetailsPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.services.CheckoutService;
import org.monostudio.api.services.OrdersProcessService;
import org.monostudio.common.exceptions.BadInputException;
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
import java.util.Map;

import static org.monostudio.config.Constants.ORDER_STATUS_PAYMENT_STARTED;

@Service
public class CheckoutServiceImpl
    implements CheckoutService {
    private final Logger logger = LoggerFactory.getLogger(CheckoutServiceImpl.class);
    private final OrdersCrudService ordersCrudService;
    private final OrdersProcessService ordersProcessService;
    private final OrdersPredicateService ordersPredicateService;
    private final PaymentService paymentIntegrationService;

    @Autowired
    public CheckoutServiceImpl(
        OrdersCrudService ordersCrudService,
        OrdersProcessService ordersProcessService,
        OrdersPredicateService ordersPredicateService,
        PaymentService paymentIntegrationService
    ) {
        this.ordersCrudService = ordersCrudService;
        this.ordersProcessService = ordersProcessService;
        this.ordersPredicateService = ordersPredicateService;
        this.paymentIntegrationService = paymentIntegrationService;
    }

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
        // TODO add a switch to either use path params or query params
        try {
            String url = (paymentIntegrationService.getPaymentResultPageUrl() + "?token=" + transactionToken);
            return new URL(url).toURI();
        } catch (MalformedURLException | URISyntaxException ex) {
            logger.error("Malformed redirection URL; make sure the 'final URL for payment method' property is correctly configured.", ex);
            throw new IllegalStateException("Transaction was confirmed, but server had an unexpected malfunction");
        }
    }

    /**
     * Fetches the result of a transaction from the payment payment service and updates it in the database.
     *
     * @throws EntityNotFoundException If no transaction has a matching token.
     * @throws PaymentServiceException As raised at payment level.
     */
    private OrderPojo processSellPaymentStatus(OrderPojo sellByToken)
        throws EntityNotFoundException, PaymentServiceException {
        int statusCode = paymentIntegrationService.requestPaymentResult(sellByToken.getToken());
        try {
            if (statusCode!=0) {
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
