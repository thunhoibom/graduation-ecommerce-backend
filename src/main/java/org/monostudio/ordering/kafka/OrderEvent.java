package org.monostudio.ordering.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lean event payload published to the Kafka {@code order-events} topic.
 * <p>
 * Intentionally keeps only primitive/String fields to avoid complex
 * serialization of nested POJOs and to stay resilient against schema drift.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    private OrderEventType type;

    /** Database ID of the affected order. */
    private Long orderId;

    /**
     * Cart session token associated with the order.
     * Used by the consumer to clear the cart after payment.
     * May be null for orders not placed through a cart session.
     */
    private String cartSessionToken;

    /** The order status at the time this event was published. */
    private String orderStatus;

    // ─── Static factory methods ──────────────────────────────────────────────

    public static OrderEvent orderCreated(Long orderId, String cartSessionToken) {
        return new OrderEvent(OrderEventType.ORDER_CREATED, orderId, cartSessionToken, "PENDING");
    }

    public static OrderEvent orderPaid(Long orderId, String cartSessionToken) {
        return new OrderEvent(OrderEventType.ORDER_PAID, orderId, cartSessionToken, "PAID_UNCONFIRMED");
    }

    public static OrderEvent orderConfirmed(Long orderId) {
        return new OrderEvent(OrderEventType.ORDER_CONFIRMED, orderId, null, "PAID_CONFIRMED");
    }

    public static OrderEvent orderRejected(Long orderId) {
        return new OrderEvent(OrderEventType.ORDER_REJECTED, orderId, null, "REJECTED");
    }

    public static OrderEvent orderCancelled(Long orderId) {
        return new OrderEvent(OrderEventType.ORDER_CANCELLED, orderId, null, "ADMIN_CANCELLED");
    }

    public static OrderEvent orderCompleted(Long orderId) {
        return new OrderEvent(OrderEventType.ORDER_COMPLETED, orderId, null, "COMPLETED");
    }

    public static OrderEvent orderFailed(Long orderId, String cartSessionToken) {
        return new OrderEvent(OrderEventType.ORDER_PAYMENT_FAILED, orderId, cartSessionToken, "PAYMENT_FAILED");
    }

    public static OrderEvent orderAborted(Long orderId, String cartSessionToken) {
        return new OrderEvent(OrderEventType.ORDER_PAYMENT_ABORTED, orderId, cartSessionToken, "PAYMENT_CANCELLED");
    }
}
