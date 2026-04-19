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
import jakarta.persistence.Table;

@Entity
@Table(name = "shipping_methods")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ShippingMethod
    implements DBEntity {
    private static final long serialVersionUID = 19L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipping_method_id", nullable = false)
    private Long id;
    @Column(name = "shipping_method_name", nullable = false, unique = true)
    private String name;
    @Column(name = "shipping_method_base_fee", nullable = false)
    private int baseFee;
    @Column(name = "shipping_method_free_shipping_threshold")
    private Integer freeShippingThreshold;
    @Column(name = "shipping_method_estimated_days_min", nullable = false)
    private int estimatedDaysMin;
    @Column(name = "shipping_method_estimated_days_max", nullable = false)
    private int estimatedDaysMax;
    @Column(name = "shipping_method_active", nullable = false)
    private boolean active;
    @Column(name = "shipping_method_price_per_km")
    private Integer pricePerKm;

    /**
     * Copy-constructor.
     *
     * @param source The original ShippingMethod
     */
    public ShippingMethod(ShippingMethod source) {
        this.id = source.id;
        this.name = source.name;
        this.baseFee = source.baseFee;
        this.freeShippingThreshold = source.freeShippingThreshold;
        this.estimatedDaysMin = source.estimatedDaysMin;
        this.estimatedDaysMax = source.estimatedDaysMax;
        this.active = source.active;
        this.pricePerKm = source.pricePerKm;
    }
}
