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
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_list_items")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ProductListItem
    implements DBEntity {
    private static final long serialVersionUID = 17L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_list_item_id", nullable = false)
    private Long id;
    @JoinColumn(name = "product_list_id", nullable = false)
    @ManyToOne(optional = false)
    private ProductList list;
    @JoinColumn(name = "product_id", nullable = false)
    @ManyToOne(optional = false)
    private Product product;

    /**
     * Please note: this copy-constructor does not include a ProductListItem's relationships
     *
     * @param source The original Sell
     */
    public ProductListItem(ProductListItem source) {
        this.id = source.id;
        this.list = null;
        this.product = null;
    }
}
