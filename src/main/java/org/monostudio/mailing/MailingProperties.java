package org.monostudio.mailing;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * General, implementation-agnostic properties for mailing services
 */
@Validated
@Configuration
@ConfigurationProperties(prefix = "monostudio.mailing")
@Data
public class MailingProperties {
    private String dateFormat;
    private String dateTimezone;
    private String ownerName;
    private String ownerEmail;
    private String senderEmail;
    private String customerOrderPaymentSubject;
    private String customerOrderConfirmationSubject;
    private String customerOrderRejectionSubject;
    private String customerOrderCompletionSubject;
    private String ownerOrderConfirmationSubject;
    private String ownerOrderRejectionSubject;
    private String ownerOrderCompletionSubject;
    private String customerReturnRequestCreatedSubject;
    private String customerReturnRequestApprovedSubject;
    private String customerReturnRequestRejectedSubject;
    private String customerReturnRequestRefundCompletedSubject;
}
