package org.monostudio.mailing.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.ReturnRequestPojo;

/**
 * Publishes mail notification events to the Kafka topic.
 * <p>
 * Business services should inject this class instead of {@link org.monostudio.mailing.MailingService}
 * directly, so that mail sending is decoupled from the main request thread.
 * </p>
 */
@Component
public class KafkaMailProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMailProducer.class);
    private final KafkaTemplate<String, MailEvent> kafkaTemplate;

    public KafkaMailProducer(KafkaTemplate<String, MailEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Enqueue a notification to the customer about their order status change. */
    public void sendOrderStatusToClient(OrderPojo order) {
        MailEvent event = MailEvent.forOrderToClient(order);
        kafkaTemplate.send(KafkaMailConfig.MAIL_TOPIC, event);
        logger.info("Kafka: queued ORDER_STATUS_TO_CLIENT for order #{}", order.getBuyOrder());
    }

    /** Enqueue a notification to the store owners about a new/updated order. */
    public void sendOrderStatusToOwners(OrderPojo order) {
        MailEvent event = MailEvent.forOrderToOwners(order);
        kafkaTemplate.send(KafkaMailConfig.MAIL_TOPIC, event);
        logger.info("Kafka: queued ORDER_STATUS_TO_OWNERS for order #{}", order.getBuyOrder());
    }

    /** Enqueue a low-stock alert to the store owners. */
    public void sendLowStockAlert(String productName, int stock) {
        MailEvent event = MailEvent.forLowStock(productName, stock);
        kafkaTemplate.send(KafkaMailConfig.MAIL_TOPIC, event);
        logger.info("Kafka: queued LOW_STOCK_ALERT for product '{}'", productName);
    }

    /** Enqueue a return-request status update notification to the customer. */
    public void sendReturnRequestStatusToClient(ReturnRequestPojo request) {
        MailEvent event = MailEvent.forReturnRequestToClient(request);
        kafkaTemplate.send(KafkaMailConfig.MAIL_TOPIC, event);
        logger.info("Kafka: queued RETURN_REQUEST_STATUS_TO_CLIENT for request #{}", request.getId());
    }

    /** Enqueue a new-return-request notification to the store owners. */
    public void sendReturnRequestToOwners(ReturnRequestPojo request) {
        MailEvent event = MailEvent.forReturnRequestToOwners(request);
        kafkaTemplate.send(KafkaMailConfig.MAIL_TOPIC, event);
        logger.info("Kafka: queued RETURN_REQUEST_TO_OWNERS for request #{}", request.getId());
    }
}
