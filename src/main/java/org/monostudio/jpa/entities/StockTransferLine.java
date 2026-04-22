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
    name = "stock_transfer_lines",
    indexes = {
        @Index(columnList = "stock_transfer_line_transfer_id"),
        @Index(columnList = "stock_transfer_line_variant_id")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"stockTransfer", "variant"})
@ToString(exclude = {"stockTransfer", "variant"})
public class StockTransferLine
    implements DBEntity {
    private static final long serialVersionUID = 37L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_transfer_line_id", nullable = false)
    private Long id;

    @JoinColumn(name = "stock_transfer_line_transfer_id", nullable = false, referencedColumnName = "stock_transfer_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private StockTransfer stockTransfer;

    @JoinColumn(name = "stock_transfer_line_variant_id", nullable = false, referencedColumnName = "variant_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ProductVariant variant;

    @Column(name = "stock_transfer_line_qty", nullable = false)
    private int quantity;
}
