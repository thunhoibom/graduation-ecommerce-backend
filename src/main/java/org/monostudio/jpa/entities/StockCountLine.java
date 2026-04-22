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
    name = "stock_count_lines",
    indexes = {
        @Index(columnList = "stock_count_line_session_id"),
        @Index(columnList = "stock_count_line_variant_id")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"stockCountSession", "variant"})
@ToString(exclude = {"stockCountSession", "variant"})
public class StockCountLine
    implements DBEntity {
    private static final long serialVersionUID = 39L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_count_line_id", nullable = false)
    private Long id;

    @JoinColumn(name = "stock_count_line_session_id", nullable = false, referencedColumnName = "stock_count_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private StockCountSession stockCountSession;

    @JoinColumn(name = "stock_count_line_variant_id", nullable = false, referencedColumnName = "variant_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ProductVariant variant;

    @Column(name = "stock_count_line_expected_qty", nullable = false)
    private int expectedQty;

    @Column(name = "stock_count_line_counted_qty")
    private Integer countedQty;

    @Column(name = "stock_count_line_variance_qty")
    private Integer varianceQty;

    @Size(max = 500)
    @Column(name = "stock_count_line_reason", length = 500)
    private String reason;
}
