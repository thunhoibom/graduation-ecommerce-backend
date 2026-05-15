package org.monostudio.payment.impl.payos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.monostudio.api.models.OrderDetailPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.PaymentRedirectionDetailsPojo;
import org.monostudio.api.models.PaymentResultPojo;
import org.monostudio.api.models.RefundResultPojo;
import org.monostudio.payment.PaymentService;
import org.monostudio.payment.PaymentServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.core.ClientOptions;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.ConfirmWebhookResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Service("PAYOS")
public class PayosPaymentServiceImpl implements PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PayosPaymentServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PayosConfig config;
    private final PayOS payOS;

    @Autowired
    public PayosPaymentServiceImpl(PayosConfig config) {
        this.config = config;
        this.payOS = new PayOS(
            ClientOptions.builder()
                .clientId(config.getClientId())
                .apiKey(config.getApiKey())
                .checksumKey(config.getChecksumKey())
                .build()
        );
    }

    @Override
    public PaymentRedirectionDetailsPojo requestNewPaymentPageDetails(OrderPojo transaction) throws PaymentServiceException {
        validateRequiredConfig(config.getClientId(), "monostudio.payment.payos.client-id");
        validateRequiredConfig(config.getApiKey(), "monostudio.payment.payos.api-key");
        validateRequiredConfig(config.getChecksumKey(), "monostudio.payment.payos.checksum-key");
        validateRequiredConfig(config.getReturnUrl(), "monostudio.payment.payos.return-url");
        validateRequiredConfig(config.getCancelUrl(), "monostudio.payment.payos.cancel-url");

        long orderCode = resolveOrderCode(transaction);
        PaymentLinkItem item = resolvePrimaryItem(transaction);

        try {
            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount((long) transaction.getTotalValue())
                .description(buildDescription(transaction))
                .returnUrl(config.getReturnUrl())
                .cancelUrl(config.getCancelUrl())
                .item(item)
                .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(paymentData);
            String checkoutUrl = response.getCheckoutUrl();
            if (StringUtils.isBlank(checkoutUrl)) {
                throw new PaymentServiceException("PAYOS create payment succeeded but checkoutUrl is missing");
            }

            return PaymentRedirectionDetailsPojo.builder()
                .url(checkoutUrl)
                .token(String.valueOf(orderCode))
                .build();
        } catch (PaymentServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentServiceException("Could not create PAYOS payment link", e);
        }
    }

    @Override
    public int requestPaymentResult(String transactionToken) throws PaymentServiceException {
        return requestPaymentResultWithAmount(transactionToken).getResponseCode();
    }

    @Override
    public PaymentResultPojo requestPaymentResultWithAmount(String transactionToken) throws PaymentServiceException {
        long orderCode = parseOrderCode(transactionToken);
        try {
            PaymentLink paymentLink = payOS.paymentRequests().get(orderCode);
            Map<String, Object> payload = toMap(paymentLink);
            String status = String.valueOf(payload.getOrDefault("status", ""));
            int amountPaid = extractAmount(payload);

            boolean success = "PAID".equalsIgnoreCase(status);
            return PaymentResultPojo.builder()
                .responseCode(success ? 0 : -1)
                .authorizedAmount(amountPaid)
                .build();
        } catch (Exception e) {
            throw new PaymentServiceException("Could not query PAYOS payment status", e);
        }
    }

    @Override
    public String getPaymentResultPageUrl() {
        return config.getBrowserRedirectionUrl();
    }

    @Override
    public RefundResultPojo refund(String transactionToken, int amount) throws PaymentServiceException {
        return RefundResultPojo.builder()
            .success(false)
            .responseCode(-1)
            .errorMessage("PAYOS refund flow has not been enabled yet")
            .build();
    }

    @Override
    public boolean validateCallback(Map<String, String> transactionData) {
        // Browser redirect callback from PAYOS may not include signature.
        // In this case we allow flow to continue and rely on server-side status query.
        if (StringUtils.isBlank(transactionData.get("signature"))) {
            return true;
        }

        try {
            payOS.webhooks().verify(transactionData);
            return true;
        } catch (Exception ex) {
            logger.warn("PAYOS callback signature verification failed: {}", ex.getMessage());
            return false;
        }
    }

    public boolean validateWebhook(Map<String, Object> transactionData) {
        Object signature = transactionData == null ? null : transactionData.get("signature");
        if (signature == null || StringUtils.isBlank(String.valueOf(signature))) {
            return false;
        }

        try {
            payOS.webhooks().verify(transactionData);
            return true;
        } catch (Exception ex) {
            logger.warn("PAYOS webhook signature verification failed: {}", ex.getMessage());
            return false;
        }
    }

    public ConfirmWebhookResponse confirmWebhook(String webhookUrl) throws PaymentServiceException {
        if (StringUtils.isBlank(webhookUrl)) {
            throw new PaymentServiceException("Webhook URL is required");
        }
        try {
            return payOS.webhooks().confirm(webhookUrl);
        } catch (Exception ex) {
            throw new PaymentServiceException("Could not confirm PAYOS webhook URL", ex);
        }
    }

    private void validateRequiredConfig(String value, String keyName) throws PaymentServiceException {
        if (StringUtils.isBlank(value)) {
            throw new PaymentServiceException("Missing configuration: " + keyName);
        }
    }

    private long resolveOrderCode(OrderPojo transaction) throws PaymentServiceException {
        if (StringUtils.isNotBlank(transaction.getToken())) {
            return parseOrderCode(transaction.getToken());
        }
        if (transaction.getBuyOrder() == null) {
            throw new PaymentServiceException("Order id is required to create PAYOS payment link");
        }
        return transaction.getBuyOrder();
    }

    private long parseOrderCode(String token) throws PaymentServiceException {
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException ex) {
            throw new PaymentServiceException("PAYOS orderCode must be numeric, got: " + token);
        }
    }

    private String buildDescription(OrderPojo transaction) {
        Long orderId = transaction.getBuyOrder();
        if (orderId == null) {
            return "THANH TOAN";
        }
        String description = "DH" + orderId;
        return description.length() > 25 ? description.substring(0, 25) : description;
    }

    private PaymentLinkItem resolvePrimaryItem(OrderPojo transaction) {
        Collection<OrderDetailPojo> details = transaction.getDetails() == null ? Collections.emptyList() : transaction.getDetails();
        if (details.isEmpty()) {
            return PaymentLinkItem.builder()
                .name("Mono Studio Order")
                .price((long) transaction.getTotalValue())
                .quantity(1)
                .build();
        }

        OrderDetailPojo firstItem = details.iterator().next();
        String name = StringUtils.defaultIfBlank(firstItem.getDescription(), "Mono Studio Order");
        int price = firstItem.getUnitValue() > 0 ? firstItem.getUnitValue() : transaction.getTotalValue();
        int quantity = firstItem.getUnits() > 0 ? firstItem.getUnits() : 1;

        return PaymentLinkItem.builder()
            .name(name)
            .price((long) price)
            .quantity(quantity)
            .build();
    }

    private Map<String, Object> toMap(Object object) {
        return OBJECT_MAPPER.convertValue(object, Map.class);
    }

    private int extractAmount(Map<String, Object> payload) {
        Object amountPaid = payload.get("amountPaid");
        if (amountPaid instanceof Number number) {
            return number.intValue();
        }
        Object amount = payload.get("amount");
        if (amount instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
