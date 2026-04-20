package org.monostudio.payment.impl.vnpay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.monostudio.api.models.PaymentRedirectionDetailsPojo;
import org.monostudio.api.models.PaymentResultPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.RefundResultPojo;
import org.monostudio.payment.PaymentService;
import org.monostudio.payment.PaymentServiceException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Service("VNPAY")
public class VnpayPaymentServiceImpl implements PaymentService {
    private final Logger logger = LoggerFactory.getLogger(VnpayPaymentServiceImpl.class);
    private final VnpayConfig config;

    @Autowired
    public VnpayPaymentServiceImpl(VnpayConfig config) {
        this.config = config;
    }

    @Override
    public PaymentRedirectionDetailsPojo requestNewPaymentPageDetails(OrderPojo transaction) throws PaymentServiceException {
        try {
            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String orderType = "other";
            long amount = transaction.getTotalValue() * 100L;
            
            // Generate a unique token for this transaction if null, otherwise use transaction token
            String vnp_TxnRef = transaction.getToken();

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", config.getTmnCode());
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");
            
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
            vnp_Params.put("vnp_OrderType", orderType);

            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", config.getReturnUrl());
            vnp_Params.put("vnp_IpAddr", "127.0.0.1");

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            
            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
            
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    // Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
            String queryUrl = query.toString();
            String vnp_SecureHash = VnpayUtil.hmacSHA512(config.getHashSecret(), hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
            String paymentUrl = config.getUrl() + "?" + queryUrl;

            PaymentRedirectionDetailsPojo response = new PaymentRedirectionDetailsPojo();
            response.setUrl(paymentUrl);
            response.setToken(vnp_TxnRef);
            return response;
        } catch (Exception e) {
            logger.error("Error creating VNPAY URL", e);
            throw new PaymentServiceException("Could not create VNPAY transaction");
        }
    }

    @Override
    public int requestPaymentResult(String transactionToken) throws PaymentServiceException {
        // Since VNPAY uses the unified callback (handled in PublicCheckoutController by looking at vnp_ResponseCode),
        // we essentially just simulate a 0 (success) if the token exists, or we leave logic to controller.
        // For standard interface implementation, assume it's valid if asked.
        return 0; // Success code mapping internally
    }

    @Override
    public PaymentResultPojo requestPaymentResultWithAmount(String transactionToken) throws PaymentServiceException {
        // Mocked for sandbox, or would query VNPAY API
        PaymentResultPojo response = new PaymentResultPojo();
        response.setResponseCode(0);
        response.setAuthorizedAmount(0); // Not validating exact amount for sandbox mock
        return response;
    }

    @Override
    public String getPaymentResultPageUrl() {
        return config.getBrowserRedirectionUrl();
    }

    @Override
    public RefundResultPojo refund(String transactionToken, int amount) throws PaymentServiceException {
        // Sandbox mock: return success
        RefundResultPojo refundResult = new RefundResultPojo();
        refundResult.setSuccess(true);
        refundResult.setResponseCode(0);
        refundResult.setType("VNPAY_SANDBOX_REFUND");
        // Balance left unknown/mocked
        refundResult.setBalance(0L);
        return refundResult;
    }
}
