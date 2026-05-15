package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * A saved address book entry belonging to a User (account).
 * Used for checkout shipping/billing address selection.
 */
@Entity
@Table(
    name = "address_book",
    indexes = {
        @Index(columnList = "user_id")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class AddressBook
    implements DBEntity {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_book_id", nullable = false)
    private Long id;

    /**
     * Short label chosen by the user, e.g. "Home", "Office", "Parents"
     */
    @Size(max = 50)
    @Column(name = "address_book_label", nullable = false)
    private String label;

    /**
     * Whether this is the user's default shipping address.
     */
    @Column(name = "address_book_default_shipping", nullable = false)
    @Builder.Default
    private boolean defaultShipping = false;

    /**
     * Whether this is the user's default billing address.
     */
    @Column(name = "address_book_default_billing", nullable = false)
    @Builder.Default
    private boolean defaultBilling = false;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @CreationTimestamp
    @Column(name = "address_book_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "address_book_updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Copy-constructor — does NOT copy the User or Address relationship (set to null).
     *
     * @param source The original AddressBook entry
     */
    public AddressBook(AddressBook source) {
        this.id = source.id;
        this.label = source.label;
        this.defaultShipping = source.defaultShipping;
        this.defaultBilling = source.defaultBilling;
        this.address = null;
        this.user = null;
        this.createdAt = source.createdAt;
        this.updatedAt = source.updatedAt;
    }
}
