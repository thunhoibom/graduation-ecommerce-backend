package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
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

@Entity
@Table(
    name = "return_requests",
    indexes = {
        @Index(columnList = "return_request_date"),
        @Index(columnList = "return_request_status"),
        @Index(columnList = "return_request_order_id"),
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ReturnRequest
    implements DBEntity {
    private static final long serialVersionUID = 20L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_request_id", nullable = false)
    private Long id;

    @Column(name = "return_request_date", nullable = false)
    @CreationTimestamp
    private Instant date;

    @Column(name = "return_request_last_modified")
    @UpdateTimestamp
    private Instant lastModified;

    @Column(name = "return_request_reason", nullable = false)
    @Size(max = 1000)
    private String reason;

    @Column(name = "return_request_admin_notes")
    @Size(max = 2000)
    private String adminNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_request_status", nullable = false)
    private ReturnRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_request_refund_method", nullable = false)
    private RefundMethod refundMethod;

    @Column(name = "return_request_refund_amount")
    private Integer refundAmount;

    @Column(name = "return_request_tracking_number")
    @Size(max = 100)
    private String trackingNumber;

    @JoinColumn(name = "return_request_order_id", nullable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Order order;

    /**
     * Please note: this copy-constructor only preserves the following relationships.
     * <ul>
     *   <li>Order</li>
     * </ul>
     *
     * @param source The original ReturnRequest
     */
    public ReturnRequest(ReturnRequest source) {
        this.id = source.id;
        this.date = Instant.from(source.date);
        this.lastModified = source.lastModified;
        this.reason = source.reason;
        this.adminNotes = source.adminNotes;
        this.status = source.status;
        this.refundMethod = source.refundMethod;
        this.refundAmount = source.refundAmount;
        this.trackingNumber = source.trackingNumber;
        this.order = null;
    }

    public enum ReturnRequestStatus {
        PENDING,
        APPROVED,
        REJECTED,
        RECEIVED,
        REFUND_PROCESSING,
        REFUND_COMPLETED,
        CANCELLED
    }

    public enum RefundMethod {
        ORIGINAL_PAYMENT,
        STORE_CREDIT,
        BANK_TRANSFER
    }
}
