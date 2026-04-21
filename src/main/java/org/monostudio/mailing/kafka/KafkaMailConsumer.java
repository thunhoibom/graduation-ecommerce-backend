package org.monostudio.mailing.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.monostudio.mailing.MailingService;
import org.monostudio.mailing.MailingServiceException;

/**
 * Consumes mail notification events from the Kafka topic
 * and delegates the actual sending to {@link MailingService} (SMTP implementation).
 * <p>
 * Runs in its own thread pool managed by Spring Kafka, so SMTP latency
 * does NOT block any business-logic request thread.
 * </p>
 */
@Component
public class KafkaMailConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMailConsumer.class);
    private final MailingService mailingService;

    public KafkaMailConsumer(MailingService mailingService) {
        this.mailingService = mailingService;
    }

    @KafkaListener(topics = KafkaMailConfig.MAIL_TOPIC, groupId = "mail-consumer-group")
    public void consume(MailEvent event) {
        logger.info("Kafka Consumer: processing event type={}", event.getType());
        try {
            switch (event.getType()) {
                case ORDER_STATUS_TO_CLIENT ->
                        mailingService.notifyOrderStatusToClient(event.getOrder());
                case ORDER_STATUS_TO_OWNERS ->
                        mailingService.notifyOrderStatusToOwners(event.getOrder());
                case LOW_STOCK_ALERT ->
                        mailingService.notifyLowStockAlert(event.getProductName(), event.getCurrentStock());
                case RETURN_REQUEST_STATUS_TO_CLIENT ->
                        mailingService.notifyReturnRequestStatusToClient(event.getReturnRequest());
                case RETURN_REQUEST_TO_OWNERS ->
                        mailingService.notifyReturnRequestToOwners(event.getReturnRequest());
                default -> logger.warn("Kafka Consumer: unknown event type={}", event.getType());
            }
        } catch (MailingServiceException e) {
            // Log and move on — mail failures must not block the consumer or poison the topic.
            logger.error("Kafka Consumer: failed to send mail for event type={}: {}",
                    event.getType(), e.getMessage(), e);
        }
    }
}
