package org.monostudio.payment.impl.vnpay;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class VnpayConfig {
    @Value("${monostudio.payment.vnpay.tmn-code}")
    private String tmnCode;

    @Value("${monostudio.payment.vnpay.hash-secret}")
    private String hashSecret;

    @Value("${monostudio.payment.vnpay.url}")
    private String url;

    @Value("${monostudio.payment.vnpay.return-url}")
    private String returnUrl;

    @Value("${monostudio.payment.vnpay.browser-redirection-url}")
    private String browserRedirectionUrl;
}
