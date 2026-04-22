package org.monostudio.jpa.entities;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

@Entity
@Table(
    name = "purchase_orders",
    indexes = {
        @Index(columnList = "po_code", unique = true),
        @Index(columnList = "po_status"),
        @Index(columnList = "po_supplier_id"),
        @Index(columnList = "po_created_at")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"supplier", "lines"})
@ToString(exclude = {"supplier", "lines"})
public class PurchaseOrder
    implements DBEntity {
    private static final long serialVersionUID = 32L;

    public enum PurchaseOrderStatus {
        DRAFT,
        SUBMITTED,
        APPROVED,
        PARTIALLY_RECEIVED,
        RECEIVED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_id", nullable = false)
    private Long id;

    @Size(max = 80)
    @Column(name = "po_code", nullable = false, unique = true, length = 80)
    private String code;

    @JoinColumn(name = "po_supplier_id", nullable = false, referencedColumnName = "supplier_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "po_status", nullable = false, length = 40)
    @Builder.Default
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(name = "po_expected_date")
    private Instant expectedDate;

    @Column(name = "po_submitted_at")
    private Instant submittedAt;

    @Column(name = "po_approved_at")
    private Instant approvedAt;

    @Column(name = "po_received_at")
    private Instant receivedAt;

    @Column(name = "po_requested_by")
    private Long requestedBy;

    @Column(name = "po_approved_by")
    private Long approvedBy;

    @Size(max = 64)
    @Column(name = "po_warehouse_id", nullable = false, length = 64)
    @Builder.Default
    private String warehouseId = "MAIN";

    @Size(max = 64)
    @Column(name = "po_location_code", nullable = false, length = 64)
    @Builder.Default
    private String locationCode = "MAIN-A1";

    @Size(max = 1000)
    @Column(name = "po_note", length = 1000)
    private String note;

    @CreationTimestamp
    @Column(name = "po_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "po_updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderLine> lines = new ArrayList<>();
}
