package org.monostudio.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.monostudio.jpa.DBEntity;

@Entity
@Table(
    name = "purchase_order_lines",
    indexes = {
        @Index(columnList = "po_line_po_id"),
        @Index(columnList = "po_line_variant_id")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"purchaseOrder", "variant"})
@ToString(exclude = {"purchaseOrder", "variant"})
public class PurchaseOrderLine
    implements DBEntity {
    private static final long serialVersionUID = 33L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_line_id", nullable = false)
    private Long id;

    @JoinColumn(name = "po_line_po_id", nullable = false, referencedColumnName = "po_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PurchaseOrder purchaseOrder;

    @JoinColumn(name = "po_line_variant_id", nullable = false, referencedColumnName = "variant_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ProductVariant variant;

    @Column(name = "po_line_ordered_qty", nullable = false)
    private int orderedQty;

    @Column(name = "po_line_received_qty", nullable = false)
    @Builder.Default
    private int receivedQty = 0;

    @Column(name = "po_line_unit_cost")
    private Integer unitCost;

    @Size(max = 500)
    @Column(name = "po_line_note", length = 500)
    private String note;
}
