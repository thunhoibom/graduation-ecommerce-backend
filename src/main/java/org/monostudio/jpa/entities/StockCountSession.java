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
    name = "stock_count_sessions",
    indexes = {
        @Index(columnList = "stock_count_code", unique = true),
        @Index(columnList = "stock_count_status"),
        @Index(columnList = "stock_count_created_at")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"lines"})
@ToString(exclude = {"lines"})
public class StockCountSession
    implements DBEntity {
    private static final long serialVersionUID = 38L;

    public enum StockCountStatus {
        PLANNED,
        IN_PROGRESS,
        COUNTED,
        APPROVED,
        POSTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_count_id", nullable = false)
    private Long id;

    @Size(max = 80)
    @Column(name = "stock_count_code", nullable = false, unique = true, length = 80)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_count_status", nullable = false, length = 40)
    @Builder.Default
    private StockCountStatus status = StockCountStatus.PLANNED;

    @Size(max = 64)
    @Column(name = "stock_count_warehouse_id", nullable = false, length = 64)
    @Builder.Default
    private String warehouseId = "MAIN";

    @Size(max = 64)
    @Column(name = "stock_count_location_code", nullable = false, length = 64)
    @Builder.Default
    private String locationCode = "MAIN-A1";

    @Column(name = "stock_count_planned_at")
    private Instant plannedAt;

    @Column(name = "stock_count_counted_at")
    private Instant countedAt;

    @Column(name = "stock_count_approved_at")
    private Instant approvedAt;

    @Column(name = "stock_count_posted_at")
    private Instant postedAt;

    @Column(name = "stock_count_requested_by")
    private Long requestedBy;

    @Column(name = "stock_count_approved_by")
    private Long approvedBy;

    @Size(max = 1000)
    @Column(name = "stock_count_note", length = 1000)
    private String note;

    @CreationTimestamp
    @Column(name = "stock_count_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "stock_count_updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "stockCountSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockCountLine> lines = new ArrayList<>();
}
