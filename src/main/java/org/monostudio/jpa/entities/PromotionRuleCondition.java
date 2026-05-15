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

/**
 * Single predicate: fact_field operator target_value (AND-ed with sibling conditions).
 */
@Entity
@Table(name = "promotion_rule_conditions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "rule")
public class PromotionRuleCondition implements DBEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "condition_id", nullable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condition_rule_id", nullable = false)
    private PromotionRule rule;

    @Column(name = "condition_fact_field", nullable = false, length = 80)
    private String factField;

    @Column(name = "condition_operator", nullable = false, length = 10)
    private String operator;

    @Column(name = "condition_target_value", nullable = false, length = 500)
    private String targetValue;
}
