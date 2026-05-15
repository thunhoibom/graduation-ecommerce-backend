package org.monostudio.payment.impl.payos;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class PayosConfig {
    @Value("${monostudio.payment.payos.client-id:}")
    private String clientId;

    @Value("${monostudio.payment.payos.api-key:}")
    private String apiKey;

    @Value("${monostudio.payment.payos.checksum-key:}")
    private String checksumKey;

    @Value("${monostudio.payment.payos.return-url:}")
    private String returnUrl;

    @Value("${monostudio.payment.payos.cancel-url:}")
    private String cancelUrl;

    @Value("${monostudio.payment.payos.browser-redirection-url:}")
    private String browserRedirectionUrl;
}
