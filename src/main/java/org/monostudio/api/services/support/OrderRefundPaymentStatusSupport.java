package org.monostudio.api.services.support;

import lombok.NoArgsConstructor;
import org.monostudio.config.Constants;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.repositories.OrdersRepository;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class OrderRefundPaymentStatusSupport {

    public static void syncAfterRefund(Order order, OrdersRepository ordersRepository) {
        if (order == null || order.getId() == null) {
            return;
        }
        int total = Math.max(0, order.getTotalValue());
        int refunded = Math.max(0, order.getTotalRefundedAmount());
        if (refunded <= 0) {
            return;
        }
        String paymentStatus = total > 0 && refunded >= total
            ? Constants.ORDER_PAYMENT_STATUS_REFUNDED
            : Constants.ORDER_PAYMENT_STATUS_PARTIALLY_REFUNDED;
        ordersRepository.setPaymentStatus(order.getId(), paymentStatus);
        order.setPaymentStatus(paymentStatus);
    }
}
