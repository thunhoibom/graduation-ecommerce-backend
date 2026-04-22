package org.monostudio.payment.impl.momo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service("MOMO")
public class MomoPaymentServiceImpl implements PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(MomoPaymentServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String REQUEST_TYPE_CAPTURE_WALLET = "captureWallet";
    private static final String LANG_VI = "vi";

    private final MomoConfig config;

    @Autowired
    public MomoPaymentServiceImpl(MomoConfig config) {
        this.config = config;
    }

    @Override
    public PaymentRedirectionDetailsPojo requestNewPaymentPageDetails(OrderPojo transaction) throws PaymentServiceException {
        validateRequiredConfig(config.getCreateUrl(), "monostudio.payment.momo.create-url");
        validateRequiredConfig(config.getPartnerCode(), "monostudio.payment.momo.partner-code");
        validateRequiredConfig(config.getAccessKey(), "monostudio.payment.momo.access-key");
        validateRequiredConfig(config.getSecretKey(), "monostudio.payment.momo.secret-key");
        validateRequiredConfig(config.getReturnUrl(), "monostudio.payment.momo.return-url");
        validateRequiredConfig(config.getNotifyUrl(), "monostudio.payment.momo.notify-url");

        String orderId = StringUtils.isNotBlank(transaction.getToken())
            ? transaction.getToken()
            : buildOrderId(transaction);
        String requestId = UUID.randomUUID().toString();
        String amount = String.valueOf(transaction.getTotalValue());
        String orderInfo = "Thanh toan don hang #" + transaction.getBuyOrder();

        String rawSignature = "accessKey=" + config.getAccessKey()
            + "&amount=" + amount
            + "&extraData="
            + "&ipnUrl=" + config.getNotifyUrl()
            + "&orderId=" + orderId
            + "&orderInfo=" + orderInfo
            + "&partnerCode=" + config.getPartnerCode()
            + "&redirectUrl=" + config.getReturnUrl()
            + "&requestId=" + requestId
            + "&requestType=" + REQUEST_TYPE_CAPTURE_WALLET;

        String signature = hmacSHA256(config.getSecretKey(), rawSignature);

        Map<String, Object> payload = new HashMap<>();
        payload.put("partnerCode", config.getPartnerCode());
        payload.put("partnerName", "Mono Studio");
        payload.put("storeId", "MonoStudio");
        payload.put("requestId", requestId);
        payload.put("amount", amount);
        payload.put("orderId", orderId);
        payload.put("orderInfo", orderInfo);
        payload.put("redirectUrl", config.getReturnUrl());
        payload.put("ipnUrl", config.getNotifyUrl());
        payload.put("lang", LANG_VI);
        payload.put("requestType", REQUEST_TYPE_CAPTURE_WALLET);
        payload.put("autoCapture", true);
        payload.put("extraData", "");
        payload.put("signature", signature);

        JsonNode response = postJson(config.getCreateUrl(), payload);
        int resultCode = response.path("resultCode").asInt(-1);
        if (resultCode != 0) {
            String message = response.path("message").asText("Unknown MOMO error");
            throw new PaymentServiceException("MOMO create payment failed: " + message + " (resultCode=" + resultCode + ")");
        }

        String payUrl = response.path("payUrl").asText(null);
        if (StringUtils.isBlank(payUrl)) {
            throw new PaymentServiceException("MOMO create payment succeeded but payUrl is missing");
        }

        return PaymentRedirectionDetailsPojo.builder()
            .url(payUrl)
            .token(orderId)
            .build();
    }

    @Override
    public int requestPaymentResult(String transactionToken) throws PaymentServiceException {
        return requestPaymentResultWithAmount(transactionToken).getResponseCode();
    }

    @Override
    public PaymentResultPojo requestPaymentResultWithAmount(String transactionToken) throws PaymentServiceException {
        validateRequiredConfig(config.getQueryUrl(), "monostudio.payment.momo.query-url");
        validateRequiredConfig(config.getPartnerCode(), "monostudio.payment.momo.partner-code");
        validateRequiredConfig(config.getAccessKey(), "monostudio.payment.momo.access-key");
        validateRequiredConfig(config.getSecretKey(), "monostudio.payment.momo.secret-key");

        String requestId = UUID.randomUUID().toString();
        String rawSignature = "accessKey=" + config.getAccessKey()
            + "&orderId=" + transactionToken
            + "&partnerCode=" + config.getPartnerCode()
            + "&requestId=" + requestId;
        String signature = hmacSHA256(config.getSecretKey(), rawSignature);

        Map<String, Object> payload = new HashMap<>();
        payload.put("partnerCode", config.getPartnerCode());
        payload.put("requestId", requestId);
        payload.put("orderId", transactionToken);
        payload.put("lang", LANG_VI);
        payload.put("signature", signature);

        JsonNode response = postJson(config.getQueryUrl(), payload);
        int resultCode = response.path("resultCode").asInt(-1);
        int amount = response.path("amount").asInt(0);

        return PaymentResultPojo.builder()
            .responseCode(resultCode == 0 ? 0 : resultCode)
            .authorizedAmount(amount)
            .build();
    }

    @Override
    public String getPaymentResultPageUrl() {
        return config.getBrowserRedirectionUrl();
    }

    @Override
    public RefundResultPojo refund(String transactionToken, int amount) throws PaymentServiceException {
        if (StringUtils.isBlank(config.getRefundUrl())) {
            throw new PaymentServiceException("MOMO refund URL is not configured");
        }
        return RefundResultPojo.builder()
            .success(false)
            .responseCode(-1)
            .errorMessage("MOMO refund flow has not been enabled yet")
            .build();
    }

    @Override
    public boolean validateCallback(Map<String, String> transactionData) {
        String callbackSignature = transactionData.get("signature");
        if (StringUtils.isBlank(callbackSignature)) {
            return false;
        }

        try {
            String fixedRawData = buildMomoCallbackRawSignature(transactionData);
            String fixedComputed = hmacSHA256(config.getSecretKey(), fixedRawData);
            if (fixedComputed.equals(callbackSignature)) {
                return true;
            }

            // Fallback for payload variants: canonicalized k=v ordering (without signature keys)
            TreeMap<String, String> canonical = new TreeMap<>();
            transactionData.forEach((key, value) -> {
                if (!"signature".equals(key) && !"signatureType".equals(key) && value != null) {
                    canonical.put(key, value);
                }
            });
            String rawDataFallback = canonical.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
            String fallbackComputed = hmacSHA256(config.getSecretKey(), rawDataFallback);
            return fallbackComputed.equals(callbackSignature);
        } catch (Exception e) {
            logger.error("Failed to validate MOMO callback signature", e);
            return false;
        }
    }

    private JsonNode postJson(String endpoint, Map<String, Object> payload) throws PaymentServiceException {
        try {
            String body = OBJECT_MAPPER.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PaymentServiceException("MOMO API returned HTTP " + response.statusCode());
            }
            return OBJECT_MAPPER.readTree(response.body());
        } catch (PaymentServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentServiceException("Could not call MOMO API", e);
        }
    }

    private String hmacSHA256(String key, String data) throws PaymentServiceException {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hash.append(String.format("%02x", value));
            }
            return hash.toString();
        } catch (Exception e) {
            throw new PaymentServiceException("Could not generate MOMO signature", e);
        }
    }

    private void validateRequiredConfig(String value, String keyName) throws PaymentServiceException {
        if (StringUtils.isBlank(value)) {
            throw new PaymentServiceException("Missing configuration: " + keyName);
        }
    }

    private String buildOrderId(OrderPojo transaction) {
        String buyOrder = transaction.getBuyOrder() != null
            ? String.valueOf(transaction.getBuyOrder())
            : "NA";
        return "MOMO-" + buyOrder + "-" + System.currentTimeMillis();
    }

    private String buildMomoCallbackRawSignature(Map<String, String> transactionData) {
        List<String> orderedFields = List.of(
            "partnerCode",
            "orderId",
            "requestId",
            "amount",
            "orderInfo",
            "orderType",
            "transId",
            "resultCode",
            "message",
            "payType",
            "responseTime",
            "extraData"
        );
        Map<String, String> raw = new TreeMap<>();
        raw.put("accessKey", config.getAccessKey());
        for (String field : orderedFields) {
            raw.put(field, StringUtils.defaultString(transactionData.get(field)));
        }
        return "accessKey=" + raw.get("accessKey")
            + "&amount=" + raw.get("amount")
            + "&extraData=" + raw.get("extraData")
            + "&message=" + raw.get("message")
            + "&orderId=" + raw.get("orderId")
            + "&orderInfo=" + raw.get("orderInfo")
            + "&orderType=" + raw.get("orderType")
            + "&partnerCode=" + raw.get("partnerCode")
            + "&payType=" + raw.get("payType")
            + "&requestId=" + raw.get("requestId")
            + "&responseTime=" + raw.get("responseTime")
            + "&resultCode=" + raw.get("resultCode")
            + "&transId=" + raw.get("transId");
    }
}
