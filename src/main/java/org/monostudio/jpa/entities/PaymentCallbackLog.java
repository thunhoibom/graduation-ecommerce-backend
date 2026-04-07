package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Tracks processed payment callbacks to provide idempotency.
 *
 * <p>When the payment gateway calls back via {@code /public/checkout/validate},
 * we check if this token has already been processed. If yes, we skip processing
 * to prevent double-confirm (double stock deduction) or double-abort.</p>
 *
 * <p>Each token is processed exactly once. Concurrent callbacks for the same
 * token are safely ignored — the first to insert wins, subsequent attempts
 * hit a unique constraint violation.</p>
 */
@Entity
@Table(
    name = "payment_callback_log",
    indexes = {
        @Index(columnList = "callback_token", unique = true),
        @Index(columnList = "callback_order_id"),
        @Index(columnList = "callback_processed_at")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class PaymentCallbackLog
    implements DBEntity {
    private static final long serialVersionUID = 1L;

    public enum CallbackResult {
        /** Payment succeeded — order marked PAID_UNCONFIRMED */
        SUCCESS,
        /** Payment failed or amount mismatch — order marked PAYMENT_FAILED or PAYMENT_CANCELLED */
        ABORTED,
        /** Payment gateway returned an error or unexpected code */
        GATEWAY_ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "callback_id", nullable = false)
    private Long id;

    /**
     * The payment gateway token — unique identifier for this callback.
     * Webpay Plus token (or whichever gateway is used).
     */
    @Column(name = "callback_token", nullable = false, unique = true, length = 128)
    private String token;

    /**
     * The order ID this callback was for.
     */
    @Column(name = "callback_order_id", nullable = false)
    private Long orderId;

    /**
     * How this callback was processed.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "callback_result", nullable = false, length = 20)
    private CallbackResult result;

    /**
     * The order status AFTER processing (e.g. "Payment Failed", "Paid, Unconfirmed").
     * Useful for debugging and auditing.
     */
    @Column(name = "callback_order_status_after", nullable = false, length = 50)
    private String orderStatusAfter;

    /**
     * The authorized amount returned by the gateway (for SUCCESS callbacks).
     * Useful for fraud auditing.
     */
    @Column(name = "callback_authorized_amount")
    private Integer authorizedAmount;

    @CreationTimestamp
    @Column(name = "callback_processed_at", nullable = false, updatable = false)
    private Instant processedAt;
}