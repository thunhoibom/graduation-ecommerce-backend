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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Represents a temporary stock hold placed when a customer adds a variant to their cart.
 *
 * <p>Lifecycle:</p>
 * <ol>
 *   <li>{@code RESERVED} — stock is held; stockReserved is incremented on the variant</li>
 *   <li>{@code CONFIRMED} — payment succeeded; stockCurrent is decremented, stockReserved is decremented</li>
 *   <li>{@code RELEASED} — cart abandoned or payment failed; stockReserved is decremented</li>
 * </ol>
 *
 * <p>Reservations expire after {@code RESERVATION_TTL_MINUTES} (default 30) if not confirmed or released.</p>
 */
@Entity
@Table(
    name = "stock_reservations",
    indexes = {
        @Index(columnList = "reservation_session_id"),
        @Index(columnList = "reservation_variant_id"),
        @Index(columnList = "reservation_status"),
        @Index(columnList = "reservation_expires_at")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class StockReservation
    implements DBEntity {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_RESERVED  = "RESERVED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_RELEASED  = "RELEASED";
    public static final int    DEFAULT_TTL_MINUTES = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id", nullable = false)
    private Long id;

    /**
     * The cart session that placed this reservation.
     * Identifies the owning cart — all reservations sharing the same sessionId belong to one cart.
     */
    @Column(name = "reservation_session_id", nullable = false, length = 64)
    private String sessionId;

    @JoinColumn(name = "reservation_variant_id", nullable = false,
        referencedColumnName = "variant_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private ProductVariant variant;

    @Column(name = "reservation_quantity", nullable = false)
    private int quantity;

    /**
     * Reservation status: RESERVED | CONFIRMED | RELEASED
     */
    @Column(name = "reservation_status", nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_RESERVED;

    /**
     * When this reservation expires and can be automatically released.
     */
    @Column(name = "reservation_expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "reservation_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "reservation_updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Copy-constructor — does NOT copy the variant relationship.
     *
     * @param source The original StockReservation
     */
    public StockReservation(StockReservation source) {
        this.id = source.id;
        this.sessionId = source.sessionId;
        this.variant = null;
        this.quantity = source.quantity;
        this.status = source.status;
        this.expiresAt = source.expiresAt;
        this.createdAt = source.createdAt;
        this.updatedAt = source.updatedAt;
    }
}
