package org.monostudio.payment.impl.webpayplus;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@Configuration
@ConfigurationProperties(prefix = "monostudio.payment.webpayplus")
@Data
public class WebpayplusPaymentProperties {
    private boolean production;
    private String commerceCode;
    private String apiKey;
    @NotBlank
    private String callbackUrl;
    @NotBlank
    private String browserRedirectionUrl;

}
