package org.monostudio.payment.impl.cod;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.PaymentRedirectionDetailsPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.PaymentResultPojo;
import org.monostudio.api.models.RefundResultPojo;
import org.monostudio.payment.PaymentService;
import org.monostudio.payment.PaymentServiceException;
import org.monostudio.payment.impl.vnpay.VnpayConfig;

import java.util.UUID;

/**
 * Implementation for Cash on Delivery (COD) payment method.
 * It doesn't require an external gateway.
 */
@Service("COD")
public class CODPaymentServiceImpl
    implements PaymentService {

    private final VnpayConfig config;

    @Autowired
    public CODPaymentServiceImpl(VnpayConfig config) {
        this.config = config;
    }

    @Override
    public PaymentRedirectionDetailsPojo requestNewPaymentPageDetails(OrderPojo transaction) {
        // For COD, we don't redirect to an external site.
        // We can redirect back to our own result page immediately.
        return PaymentRedirectionDetailsPojo.builder()
            .url(getPaymentResultPageUrl())
            .token("COD-" + UUID.randomUUID().toString())
            .build();
    }

    @Override
    public int requestPaymentResult(String transactionToken) {
        return 0; // Success
    }

    @Override
    public PaymentResultPojo requestPaymentResultWithAmount(String transactionToken) {
        return PaymentResultPojo.builder()
            .responseCode(0)
            .authorizedAmount(0) // Not applicable for COD at this stage
            .build();
    }

    @Override
    public String getPaymentResultPageUrl() {
        return config.getBrowserRedirectionUrl();
    }

    @Override
    public RefundResultPojo refund(String transactionToken, int amount) {
        return RefundResultPojo.builder()
            .success(true)
            .responseCode(0)
            .build();
    }
}
