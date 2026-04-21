package org.monostudio.ordering.kafka;

/**
 * Represents the type of order lifecycle event published to Kafka.
 * Consumers can subscribe to specific types to react accordingly.
 */
public enum OrderEventType {
    ORDER_CREATED,
    ORDER_PAYMENT_STARTED,
    ORDER_PAID,           // Payment confirmed — triggers cart clearing, downstream processing
    ORDER_CONFIRMED,      // Admin confirmed the order
    ORDER_REJECTED,       // Admin rejected the order
    ORDER_CANCELLED,      // Admin cancelled the order
    ORDER_COMPLETED,      // Order delivered / completed
    ORDER_PAYMENT_FAILED,
    ORDER_PAYMENT_ABORTED
}
