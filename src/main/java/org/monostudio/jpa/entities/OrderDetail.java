package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "order_details")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class OrderDetail
    implements DBEntity {
    private static final long serialVersionUID = 15L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_detail_id", nullable = false)
    private Long id;
    @Column(name = "order_detail_units", nullable = false)
    private int units;
    @Column(name = "order_detail_unit_value", nullable = false)
    private Integer unitValue;
    @Column(name = "order_detail_description", nullable = false)
    @Size(max = 260)
    private String description;
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", updatable = false,
        foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(optional = false)
    private Product product;
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", insertable = false, updatable = false,
        foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Order order;

    /**
     * Please note: this copy-constructor does not include a OrderDetail's relationships
     *
     * @param source The original OrderDetail
     */
    public OrderDetail(OrderDetail source) {
        this.id = source.id;
        this.units = source.units;
        this.unitValue = source.unitValue;
        this.description = source.description;
        this.product = null;
        this.order = null;
    }
}
