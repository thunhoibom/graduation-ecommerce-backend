package org.monostudio.jpa.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
    name = "stock_transfers",
    indexes = {
        @Index(columnList = "stock_transfer_code", unique = true),
        @Index(columnList = "stock_transfer_status"),
        @Index(columnList = "stock_transfer_created_at")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"lines"})
@ToString(exclude = {"lines"})
public class StockTransfer
    implements DBEntity {
    private static final long serialVersionUID = 36L;

    public enum StockTransferStatus {
        DRAFT,
        SUBMITTED,
        APPROVED,
        IN_TRANSIT,
        COMPLETED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_transfer_id", nullable = false)
    private Long id;

    @Size(max = 80)
    @Column(name = "stock_transfer_code", nullable = false, unique = true, length = 80)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_transfer_status", nullable = false, length = 40)
    @Builder.Default
    private StockTransferStatus status = StockTransferStatus.DRAFT;

    @Size(max = 64)
    @Column(name = "stock_transfer_warehouse_id", nullable = false, length = 64)
    @Builder.Default
    private String warehouseId = "MAIN";

    @Size(max = 64)
    @Column(name = "stock_transfer_from_location", nullable = false, length = 64)
    private String fromLocation;

    @Size(max = 64)
    @Column(name = "stock_transfer_to_location", nullable = false, length = 64)
    private String toLocation;

    @Column(name = "stock_transfer_requested_by")
    private Long requestedBy;

    @Column(name = "stock_transfer_approved_by")
    private Long approvedBy;

    @Column(name = "stock_transfer_submitted_at")
    private Instant submittedAt;

    @Column(name = "stock_transfer_approved_at")
    private Instant approvedAt;

    @Column(name = "stock_transfer_completed_at")
    private Instant completedAt;

    @Size(max = 1000)
    @Column(name = "stock_transfer_note", length = 1000)
    private String note;

    @CreationTimestamp
    @Column(name = "stock_transfer_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "stock_transfer_updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "stockTransfer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockTransferLine> lines = new ArrayList<>();
}
