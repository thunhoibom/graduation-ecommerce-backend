package org.monostudio.mailing.impl.mailgun;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Holds configuration properties for using Mailgun as a mail service provider.<br/>
 * A Mailgun account is required to use this.<br/>
 * Read about Mailgun on <a href="https://www.mailgun.com/">their website here</a>.
 */
@Validated
@Component
@ConfigurationProperties(prefix = "monostudio.mailing.mailgun")
@Profile("mailgun")
@Data
public class MailgunMailingProperties {
    private String apiKey;
    private String domain;
    private String customerOrderPaymentTemplate;
    private String customerOrderConfirmationTemplate;
    private String customerOrderRejectionTemplate;
    private String customerOrderCompletionTemplate;
    private String ownerOrderConfirmationTemplate;
    private String ownerOrderRejectionTemplate;
    private String ownerOrderCompletionTemplate;
}
