package org.monostudio.ordering.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes order lifecycle events to the Kafka {@code order-events} topic.
 * <p>
 * Inject this into services that need to broadcast order state changes
 * without knowing the downstream consumers.
 * </p>
 */
@Component
public class KafkaOrderProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaOrderProducer.class);
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public KafkaOrderProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(Long orderId, String cartSessionToken) {
        publish(OrderEvent.orderCreated(orderId, cartSessionToken));
    }

    public void publishOrderPaymentStarted(Long orderId, String cartSessionToken) {
        publish(new OrderEvent(OrderEventType.ORDER_PAYMENT_STARTED, orderId, cartSessionToken, "PAYMENT_STARTED"));
    }

    public void publishOrderPaid(Long orderId, String cartSessionToken) {
        publish(OrderEvent.orderPaid(orderId, cartSessionToken));
    }

    public void publishOrderConfirmed(Long orderId) {
        publish(OrderEvent.orderConfirmed(orderId));
    }

    public void publishOrderRejected(Long orderId) {
        publish(OrderEvent.orderRejected(orderId));
    }

    public void publishOrderCancelled(Long orderId) {
        publish(OrderEvent.orderCancelled(orderId));
    }

    public void publishOrderCompleted(Long orderId) {
        publish(OrderEvent.orderCompleted(orderId));
    }

    public void publishOrderFailed(Long orderId, String cartSessionToken) {
        publish(OrderEvent.orderFailed(orderId, cartSessionToken));
    }

    public void publishOrderAborted(Long orderId, String cartSessionToken) {
        publish(OrderEvent.orderAborted(orderId, cartSessionToken));
    }

    private void publish(OrderEvent event) {
        // Use orderId as partition key so all events for the same order are ordered
        kafkaTemplate.send(KafkaOrderConfig.ORDER_TOPIC, String.valueOf(event.getOrderId()), event);
        logger.info("Kafka: published order event type={} orderId={}", event.getType(), event.getOrderId());
    }
}
