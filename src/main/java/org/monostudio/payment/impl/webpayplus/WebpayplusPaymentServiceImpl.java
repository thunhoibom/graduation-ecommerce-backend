package org.monostudio.payment.impl.webpayplus;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.common.IntegrationCommerceCodes;
import cl.transbank.common.IntegrationType;
import cl.transbank.webpay.common.WebpayOptions;
import cl.transbank.webpay.exception.TransactionCommitException;
import cl.transbank.webpay.exception.TransactionCreateException;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.PaymentRedirectionDetailsPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.PaymentResultPojo;
import org.monostudio.api.models.RefundResultPojo;
import org.monostudio.payment.PaymentService;
import org.monostudio.payment.PaymentServiceException;

import java.io.IOException;

/**
 * Implements Webpay Plus as a payment gateway, using their official SDK.<br/>
 * Usage of Webpay Plus requires an affiliation to Transbank.
 * You can <a href="https://transbank.cl/">know more about Transbank here</a>.<br/><br/>
 * Documentation for the SDK and general guidelines for using Webpay Plus
 * <a href="https://transbankdevelopers.cl/referencia/webpay">can be found here</a>.
 */
@Service
public class WebpayplusPaymentServiceImpl
    implements PaymentService {
    private final Logger logger = LoggerFactory.getLogger(WebpayplusPaymentServiceImpl.class);
    private final WebpayplusPaymentProperties properties;

    @Autowired
    public WebpayplusPaymentServiceImpl(
        WebpayplusPaymentProperties properties
    ) {
        this.properties = properties;
    }

    @Override
    public PaymentRedirectionDetailsPojo requestNewPaymentPageDetails(OrderPojo transaction) throws PaymentServiceException {
        String buyOrder = transaction.getBuyOrder().toString();
        String sessionId = String.valueOf(transaction.hashCode());
        // System stores amounts in cents; Webpay expects the currency's base unit (dollars)
        double amount = transaction.getTotalValue() / 100.0;
        String returnUrl = properties.getCallbackUrl();

        WebpayPlus.Transaction webpayTransaction = this.createWebpayTransaction();

        try {
            WebpayPlusTransactionCreateResponse webpayResponse = webpayTransaction.create(
                buyOrder, sessionId, amount, returnUrl
            );
            return PaymentRedirectionDetailsPojo.builder()
                .url(webpayResponse.getUrl())
                .token(webpayResponse.getToken())
                .build();
        } catch (TransactionCreateException | IOException exc) {
            logger.error("Exception raised while creating transaction: ", exc);
            throw new PaymentServiceException("Webpay could not create a new transaction");
        }
    }

    @Override
    public int requestPaymentResult(String transactionToken) throws PaymentServiceException {
        return requestPaymentResultWithAmount(transactionToken).getResponseCode();
    }

    @Override
    public PaymentResultPojo requestPaymentResultWithAmount(String transactionToken) throws PaymentServiceException {
        WebpayPlus.Transaction webpayTransaction = createWebpayTransaction();
        try {
            var commitResponse = webpayTransaction.commit(transactionToken);
            // Webpay returns amount in dollars; convert back to cents for comparison
            int authorizedAmount = (int) Math.round(commitResponse.getAmount() * 100);
            return PaymentResultPojo.builder()
                .responseCode(commitResponse.getResponseCode())
                .authorizedAmount(authorizedAmount)
                .build();
        } catch (TransactionCommitException exc) {
            // Return failure response instead of throwing — caller handles the logic
            return PaymentResultPojo.builder()
                .responseCode(1)
                .authorizedAmount(0)
                .build();
        } catch (IOException exc) {
            logger.error("Exception raised while requesting transaction result: ", exc);
            throw new PaymentServiceException("Webpay failed to confirm the transaction");
        }
    }

    @Override
    public String getPaymentResultPageUrl() {
        return properties.getBrowserRedirectionUrl();
    }

    @Override
    public RefundResultPojo refund(String transactionToken, int amount) throws PaymentServiceException {
        WebpayPlus.Transaction webpayTransaction = createWebpayTransaction();
        try {
            // Transbank amount is in the currency's base unit (dollars, not cents)
            double refundAmount = amount / 100.0;
            var response = webpayTransaction.refund(transactionToken, refundAmount);
            return RefundResultPojo.builder()
                .success(response.getResponseCode() == 0)
                .responseCode(response.getResponseCode())
                .type(response.getType())
                .balance(response.getBalance() != 0 ? (long) response.getBalance() : null)
                .build();
        } catch (Exception exc) {
            logger.error("Refund failed for token {}: {}", transactionToken, exc.getMessage());
            throw new PaymentServiceException("Refund failed: " + exc.getMessage(), exc);
        }
    }

    private WebpayPlus.Transaction createWebpayTransaction() {
        String commerceCode = IntegrationCommerceCodes.WEBPAY_PLUS;
        String apiKey = IntegrationApiKeys.WEBPAY;
        IntegrationType integrationType = IntegrationType.TEST;
        if (Boolean.TRUE.equals(properties.isProduction())) {
            commerceCode = properties.getCommerceCode();
            apiKey = properties.getApiKey();
            integrationType = IntegrationType.LIVE;
        }

        WebpayOptions wpOptions = new WebpayOptions(commerceCode, apiKey, integrationType);

        return new WebpayPlus.Transaction(wpOptions);
    }
}
