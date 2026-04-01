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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import java.util.List;

@Entity
@Table(name = "product_lists",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_list_name"}),
        @UniqueConstraint(columnNames = {"product_list_code"}),
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ProductList
    implements DBEntity {
    private static final long serialVersionUID = 16L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_list_id", nullable = false)
    private Long id;
    @Size(min = 2, max = 50)
    @Column(name = "product_list_name", nullable = false)
    private String name;
    @Size(min = 2, max = 25)
    @Column(name = "product_list_code", nullable = false)
    private String code;
    @Column(name = "product_list_disabled", nullable = false)
    private boolean disabled;
    @OneToMany(mappedBy = "list")
    private List<ProductListItem> items;

    /**
     * Please note: this copy-constructor does not include a ProductList's relationship to its items
     *
     * @param source The original Sell
     */
    public ProductList(ProductList source) {
        this.id = source.id;
        this.name = source.name;
        this.code = source.code;
        this.disabled = source.disabled;
        this.items = null;
    }
}
