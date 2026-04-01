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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_images")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ProductImage
    implements DBEntity {
    private static final long serialVersionUID = 12L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_image_id", nullable = false)
    private Long id;
    @JoinColumn(name = "image_id", referencedColumnName = "image_id", updatable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Image image;
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", updatable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Product product;

    /**
     * Please note: this copy-constructor does NOT include a ProductImage's relationships
     *
     * @param source The original ProductImage
     */
    public ProductImage(ProductImage source) {
        this.id = source.id;
        this.image = null;
        this.product = null;
    }
}
