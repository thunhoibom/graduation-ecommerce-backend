package org.monostudio.ordering.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.monostudio.jpa.repositories.CartItemsRepository;
import org.monostudio.jpa.repositories.CartSessionsRepository;

/**
 * Consumes order lifecycle events from the Kafka {@code order-events} topic
 * and handles asynchronous post-processing that does NOT need to be part
 * of the main business transaction.
 *
 * <p>Current responsibilities:
 * <ul>
 *   <li><b>ORDER_PAID</b>: Clear the customer's cart session so stale items
 *       don't reappear if the session is reused.</li>
 *   <li><b>ORDER_PAYMENT_FAILED / ORDER_PAYMENT_ABORTED</b>: Extensible
 *       hook for future fraud-detection or analytics pipelines.</li>
 * </ul>
 * </p>
 */
@Component
public class KafkaOrderConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaOrderConsumer.class);

    private final CartSessionsRepository cartSessionsRepository;
    private final CartItemsRepository cartItemsRepository;

    public KafkaOrderConsumer(CartSessionsRepository cartSessionsRepository,
                              CartItemsRepository cartItemsRepository) {
        this.cartSessionsRepository = cartSessionsRepository;
        this.cartItemsRepository = cartItemsRepository;
    }

    @KafkaListener(topics = KafkaOrderConfig.ORDER_TOPIC, groupId = "order-consumer-group")
    public void consume(OrderEvent event) {
        logger.info("Kafka OrderConsumer: received type={} orderId={}", event.getType(), event.getOrderId());
        try {
            switch (event.getType()) {
                case ORDER_PAID -> handleOrderPaid(event);
                case ORDER_PAYMENT_FAILED, ORDER_PAYMENT_ABORTED -> handlePaymentNotCompleted(event);
                default -> logger.debug("Kafka OrderConsumer: no handler for type={}", event.getType());
            }
        } catch (Exception e) {
            // Log and swallow — consumer failures must not block the topic
            logger.error("Kafka OrderConsumer: error processing event type={} orderId={}: {}",
                    event.getType(), event.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Clears the cart session after a successful payment so the cart appears
     * empty when the customer next visits.
     *
     * <p>This is done asynchronously — by the time this runs the order DB record
     * is already committed. A failure here only means the cart is not cleared
     * immediately; it will expire naturally or on the next session check.</p>
     */
    private void handleOrderPaid(OrderEvent event) {
        String token = event.getCartSessionToken();
        if (token == null || token.isBlank()) {
            logger.debug("OrderConsumer ORDER_PAID: no cart session token — nothing to clear (orderId={})",
                    event.getOrderId());
            return;
        }

        cartSessionsRepository.findByToken(token).ifPresentOrElse(
                session -> {
                    cartItemsRepository.deleteAllBySessionId(session.getId());
                    logger.info("OrderConsumer ORDER_PAID: cart session {} cleared (orderId={})",
                            token, event.getOrderId());
                },
                () -> logger.warn("OrderConsumer ORDER_PAID: cart session {} not found (orderId={})",
                        token, event.getOrderId())
        );
    }

    /**
     * Extensible hook for payment failure / abort events.
     * Currently logs for observability; future: feed analytics or fraud detection.
     */
    private void handlePaymentNotCompleted(OrderEvent event) {
        logger.warn("OrderConsumer {}: orderId={} cartToken={}",
                event.getType(), event.getOrderId(), event.getCartSessionToken());
    }
}
