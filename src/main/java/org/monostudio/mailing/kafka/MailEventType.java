package org.monostudio.mailing.kafka;

/**
 * Represents the type of mail notification to be sent.
 * Used as a discriminator field in {@link MailEvent} to route
 * the correct call to {@link org.monostudio.mailing.MailingService}.
 */
public enum MailEventType {
    ORDER_STATUS_TO_CLIENT,
    ORDER_STATUS_TO_OWNERS,
    LOW_STOCK_ALERT,
    RETURN_REQUEST_STATUS_TO_CLIENT,
    RETURN_REQUEST_TO_OWNERS
}
