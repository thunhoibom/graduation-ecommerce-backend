package org.monostudio.jpa.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.monostudio.jpa.DBEntity;

@Entity
@Table(
    name = "goods_receipts",
    indexes = {
        @Index(columnList = "goods_receipt_code", unique = true),
        @Index(columnList = "goods_receipt_po_id"),
        @Index(columnList = "goods_receipt_created_at")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"purchaseOrder", "lines"})
@ToString(exclude = {"purchaseOrder", "lines"})
public class GoodsReceipt
    implements DBEntity {
    private static final long serialVersionUID = 34L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goods_receipt_id", nullable = false)
    private Long id;

    @Size(max = 80)
    @Column(name = "goods_receipt_code", nullable = false, unique = true, length = 80)
    private String code;

    @JoinColumn(name = "goods_receipt_po_id", nullable = false, referencedColumnName = "po_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "goods_receipt_received_by")
    private Long receivedBy;

    @Size(max = 1000)
    @Column(name = "goods_receipt_note", length = 1000)
    private String note;

    @CreationTimestamp
    @Column(name = "goods_receipt_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GoodsReceiptLine> lines = new ArrayList<>();
}
