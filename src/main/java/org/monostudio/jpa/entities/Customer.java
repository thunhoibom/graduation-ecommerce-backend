package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Customer
    implements DBEntity {
    private static final long serialVersionUID = 4L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id", nullable = false)
    private Long id;
    @JoinColumn(name = "person_id", referencedColumnName = "person_id")
    @OneToOne(optional = false, cascade = CascadeType.ALL)
    private Person person;

    @Column(name = "customer_loyalty_tier", length = 32)
    private String loyaltyTier;

    @Column(name = "customer_monthly_spend_cents", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int monthlySpendCents = 0;

    @Column(name = "customer_loyalty_points_balance", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int loyaltyPointsBalance = 0;

    @Column(name = "customer_lifetime_points_earned", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int lifetimePointsEarned = 0;

    /**
     * Please note: this copy-constructor DOES include a Customer's relationship to its own profile data
     *
     * @param source The original UserRolePermission
     */
    public Customer(Customer source) {
        this.id = source.id;
        this.person = new Person(source.person);
        this.loyaltyTier = source.loyaltyTier;
        this.monthlySpendCents = source.monthlySpendCents;
        this.loyaltyPointsBalance = source.loyaltyPointsBalance;
        this.lifetimePointsEarned = source.lifetimePointsEarned;
    }
}
