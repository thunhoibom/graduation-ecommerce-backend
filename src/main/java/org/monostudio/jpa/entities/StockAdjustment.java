package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Audit log entry for every stock mutation that occurs in the system.
 * Every change to variant stock (reservation, release, payment deduction, return restore,
 * manual adjustment) is recorded here for traceability.
 */
@Entity
@Table(
    name = "stock_adjustments",
    indexes = {
        @Index(columnList = "stock_adjustment_date"),
        @Index(columnList = "stock_adjustment_variant_id"),
        @Index(columnList = "stock_adjustment_reason"),
        @Index(columnList = "stock_adjustment_session_id"),
        @Index(columnList = "stock_adjustment_order_id"),
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class StockAdjustment
    implements DBEntity {
    private static final long serialVersionUID = 22L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_adjustment_id", nullable = false)
    private Long id;

    @Column(name = "stock_adjustment_date", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant date;

    @JoinColumn(name = "stock_adjustment_variant_id", nullable = false,
        referencedColumnName = "variant_id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ProductVariant variant;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_adjustment_reason", nullable = false, updatable = false)
    private StockAdjustmentReason reason;

    /**
     * Numeric stock change. Positive = added to current stock, Negative = deducted from current stock.
     */
    @Column(name = "stock_adjustment_quantity_delta", nullable = false, updatable = false)
    private int quantityDelta;

    /**
     * Snapshot of the variant's stockCurrent BEFORE this adjustment was applied.
     */
    @Column(name = "stock_adjustment_stock_before", nullable = false, updatable = false)
    private int stockBefore;

    /**
     * Snapshot of the variant's stockCurrent AFTER this adjustment was applied.
     */
    @Column(name = "stock_adjustment_stock_after", nullable = false, updatable = false)
    private int stockAfter;

    /**
     * Human-readable description of this adjustment for audit trail.
     */
    @Column(name = "stock_adjustment_description", length = 500)
    @Size(max = 500)
    private String description;

    /**
     * The cart session that triggered this adjustment (if applicable).
     */
    @Column(name = "stock_adjustment_session_id", length = 64)
    private String sessionId;

    /**
     * The order ID associated with this adjustment (if applicable).
     */
    @Column(name = "stock_adjustment_order_id")
    private Long orderId;

    /**
     * The return request ID associated with this adjustment (if applicable).
     */
    @Column(name = "stock_adjustment_return_request_id")
    private Long returnRequestId;

    /**
     * Admin user ID who initiated this adjustment (for manual adjustments).
     */
    @Column(name = "stock_adjustment_performed_by")
    private Long performedBy;

    public enum StockAdjustmentReason {
        /** A cart reservation was created — stockReserved incremented, available stock decreased */
        RESERVATION_CREATED,
        /** A cart reservation was released (cart cleared or item removed) — stockReserved decremented */
        RESERVATION_RELEASED,
        /** Payment was confirmed — stockCurrent deducted, stockReserved decremented */
        PAYMENT_CONFIRMED,
        /** Payment failed or was aborted — reserved stock released back to available */
        PAYMENT_ABORTED,
        /** A return was approved — stockCurrent restored */
        RETURN_RESTORED,
        /** Admin manually adjusted stock */
        MANUAL_ADJUSTMENT,
        /** Periodic stock sync or inventory recount */
        STOCK_RECOUNT,
        /** Admin cancelled a paid order — stockCurrent restored to inventory */
        ORDER_CANCELLED,
        /** Admin rejected a paid order — stockCurrent restored to inventory */
        ORDER_REJECTED,
        /** Goods receipt from purchase order */
        PURCHASE_ORDER_RECEIPT,
        /** Internal transfer takes stock out of source location */
        TRANSFER_OUTBOUND,
        /** Internal transfer puts stock into destination location */
        TRANSFER_INBOUND,
        /** Variance posted from approved stock count */
        STOCK_COUNT_VARIANCE
    }

    /**
     * Copy-constructor — does NOT copy relationships.
     */
    public StockAdjustment(StockAdjustment source) {
        this.id = source.id;
        this.date = Instant.from(source.date);
        this.reason = source.reason;
        this.quantityDelta = source.quantityDelta;
        this.stockBefore = source.stockBefore;
        this.stockAfter = source.stockAfter;
        this.description = source.description;
        this.sessionId = source.sessionId;
        this.orderId = source.orderId;
        this.returnRequestId = source.returnRequestId;
        this.performedBy = source.performedBy;
        this.variant = null;
    }
}
