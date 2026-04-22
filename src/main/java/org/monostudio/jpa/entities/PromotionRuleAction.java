package org.monostudio.jpa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "promotion_rule_actions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "rule")
public class PromotionRuleAction implements DBEntity {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_PERCENTAGE_DISCOUNT = "PERCENTAGE_DISCOUNT";
    public static final String TYPE_FIXED_DISCOUNT = "FIXED_DISCOUNT";
    public static final String TYPE_FREE_SHIPPING = "FREE_SHIPPING";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id", nullable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action_rule_id", nullable = false)
    private PromotionRule rule;

    @Column(name = "action_type", nullable = false, length = 40)
    private String actionType;

    /**
     * PERCENTAGE_DISCOUNT: 1–100. FIXED_DISCOUNT: amount in same currency unit as cart (VND cents).
     * FREE_SHIPPING: ignored.
     */
    @Column(name = "action_value", nullable = false)
    @Builder.Default
    private int value = 0;
}
