package org.monostudio.payment.impl.momo;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class MomoConfig {
    @Value("${monostudio.payment.momo.partner-code:}")
    private String partnerCode;

    @Value("${monostudio.payment.momo.access-key:}")
    private String accessKey;

    @Value("${monostudio.payment.momo.secret-key:}")
    private String secretKey;

    @Value("${monostudio.payment.momo.create-url:}")
    private String createUrl;

    @Value("${monostudio.payment.momo.query-url:}")
    private String queryUrl;

    @Value("${monostudio.payment.momo.refund-url:}")
    private String refundUrl;

    @Value("${monostudio.payment.momo.return-url:}")
    private String returnUrl;

    @Value("${monostudio.payment.momo.notify-url:}")
    private String notifyUrl;

    @Value("${monostudio.payment.momo.browser-redirection-url:}")
    private String browserRedirectionUrl;
}
