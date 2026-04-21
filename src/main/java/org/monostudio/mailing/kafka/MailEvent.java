package org.monostudio.mailing.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.ReturnRequestPojo;

/**
 * Payload object transmitted over the Kafka mail-events topic.
 * Only the fields relevant to the given {@link MailEventType} will be populated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailEvent {

    private MailEventType type;

    /** Populated for ORDER_STATUS_TO_CLIENT and ORDER_STATUS_TO_OWNERS events. */
    private OrderPojo order;

    /** Populated for RETURN_REQUEST_* events. */
    private ReturnRequestPojo returnRequest;

    /** Populated for LOW_STOCK_ALERT events. */
    private String productName;
    private Integer currentStock;

    // ─── Static factory methods ──────────────────────────────────────────────────

    public static MailEvent forOrderToClient(OrderPojo order) {
        MailEvent e = new MailEvent();
        e.setType(MailEventType.ORDER_STATUS_TO_CLIENT);
        e.setOrder(order);
        return e;
    }

    public static MailEvent forOrderToOwners(OrderPojo order) {
        MailEvent e = new MailEvent();
        e.setType(MailEventType.ORDER_STATUS_TO_OWNERS);
        e.setOrder(order);
        return e;
    }

    public static MailEvent forLowStock(String productName, int stock) {
        MailEvent e = new MailEvent();
        e.setType(MailEventType.LOW_STOCK_ALERT);
        e.setProductName(productName);
        e.setCurrentStock(stock);
        return e;
    }

    public static MailEvent forReturnRequestToClient(ReturnRequestPojo request) {
        MailEvent e = new MailEvent();
        e.setType(MailEventType.RETURN_REQUEST_STATUS_TO_CLIENT);
        e.setReturnRequest(request);
        return e;
    }

    public static MailEvent forReturnRequestToOwners(ReturnRequestPojo request) {
        MailEvent e = new MailEvent();
        e.setType(MailEventType.RETURN_REQUEST_TO_OWNERS);
        e.setReturnRequest(request);
        return e;
    }
}
