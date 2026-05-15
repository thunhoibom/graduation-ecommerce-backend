package org.monostudio.jpa.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.monostudio.jpa.DBEntity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A promotion program composed of conditions (IF) and actions (THEN).
 */
@Entity
@Table(
    name = "promotion_rules",
    indexes = {
        @Index(columnList = "rule_active"),
        @Index(columnList = "rule_priority")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "conditions", "actions", "linkedDiscountCodes" })
public class PromotionRule implements DBEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id", nullable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "rule_name", nullable = false, length = 200)
    private String name;

    /**
     * Higher number = selected before lower when resolving conflicts.
     */
    @Column(name = "rule_priority", nullable = false)
    @Builder.Default
    private int priority = 0;

    @Column(name = "rule_combinable", nullable = false)
    @Builder.Default
    private boolean combinable = true;

    @Column(name = "rule_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_scope", nullable = false, length = 20)
    @Builder.Default
    private PromotionScope scope = PromotionScope.CART;

    @Column(name = "rule_active_from")
    private LocalDateTime activeFrom;

    @Column(name = "rule_active_until")
    private LocalDateTime activeUntil;

    /**
     * Rules sharing the same non-null group cannot both apply (e.g. FLASH_SALE).
     */
    @Column(name = "rule_mutual_exclusion_group", length = 64)
    private String mutualExclusionGroup;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 32)
    @Builder.Default
    private Set<PromotionRuleCondition> conditions = new HashSet<>();

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 32)
    @Builder.Default
    private Set<PromotionRuleAction> actions = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "promotionRule", fetch = FetchType.LAZY)
    @BatchSize(size = 16)
    @Builder.Default
    private Set<DiscountCode> linkedDiscountCodes = new HashSet<>();

    @CreationTimestamp
    @Column(name = "rule_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "rule_updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
