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
import org.monostudio.jpa.entities.Customer;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents a customer's shopping cart session.
 *
 * <p>Each session is identified by a unique token (stored in frontend localStorage).
 * A session may optionally be linked to a Customer once they log in.</p>
 *
 * <p>Lifecycle:</p>
 * <ul>
 *   <li>Created on first cart interaction (anonymous guest)</li>
 *   <li>Linked to Customer on login/register</li>
 *   <li>Cleared on checkout completion or explicit clear action</li>
 * </ul>
 */
@Entity
@Table(
    name = "cart_sessions",
    indexes = {
        @Index(columnList = "session_token", unique = true),
        @Index(columnList = "session_customer_id")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class CartSession
    implements DBEntity {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_TTL_MINUTES = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id", nullable = false)
    private Long id;

    /**
     * Unique session token — stored in frontend (localStorage).
     * Used as the primary identifier for unauthenticated carts.
     */
    @Column(name = "session_token", nullable = false, unique = true, length = 64)
    private String token;

    /**
     * Optional link to a registered Customer.
     * If set, the cart persists across devices for the same customer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_customer_id")
    private Customer customer;

    @OneToMany(mappedBy = "cartSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Collection<CartItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "session_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "session_updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * When this cart expires and should be cleaned up.
     */
    @Column(name = "session_expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Copy-constructor — does NOT copy items collection.
     *
     * @param source The original CartSession
     */
    public CartSession(CartSession source) {
        this.id = source.id;
        this.token = source.token;
        this.customer = null; // never copy the customer relationship
        this.createdAt = source.createdAt;
        this.updatedAt = source.updatedAt;
        this.expiresAt = source.expiresAt;
    }

    /**
     * Generates a new unique session token.
     */
    public static String generateToken() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Refreshes the expiry time to now + TTL.
     */
    public void refreshExpiry() {
        this.expiresAt = LocalDateTime.now().plusMinutes(DEFAULT_TTL_MINUTES);
    }

    /**
     * Checks if this cart has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
