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
    name = "goods_receipt_lines",
    indexes = {
        @Index(columnList = "goods_receipt_line_receipt_id"),
        @Index(columnList = "goods_receipt_line_po_line_id")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"goodsReceipt", "purchaseOrderLine"})
@ToString(exclude = {"goodsReceipt", "purchaseOrderLine"})
public class GoodsReceiptLine
    implements DBEntity {
    private static final long serialVersionUID = 35L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goods_receipt_line_id", nullable = false)
    private Long id;

    @JoinColumn(name = "goods_receipt_line_receipt_id", nullable = false, referencedColumnName = "goods_receipt_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private GoodsReceipt goodsReceipt;

    @JoinColumn(name = "goods_receipt_line_po_line_id", nullable = false, referencedColumnName = "po_line_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PurchaseOrderLine purchaseOrderLine;

    @Column(name = "goods_receipt_line_received_qty", nullable = false)
    private int receivedQty;
}
