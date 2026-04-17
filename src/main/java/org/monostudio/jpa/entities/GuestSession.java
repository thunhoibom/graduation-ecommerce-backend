package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Tracks each unique guest session for JWT isolation.
 * Each guest gets a unique guest_session_id used as JWT subject.
 * If a guest is banned, only that specific session is revoked —
 * other guests are unaffected.
 */
@Entity
@Table(
    name = "guest_sessions",
    indexes = {
        @Index(columnList = "session_uuid", unique = true),
        @Index(columnList = "customer_id")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class GuestSession
    implements DBEntity {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guest_session_id", nullable = false)
    private Long id;

    /**
     * Unique UUID for this guest session — used as JWT subject.
     * Frontend stores this in localStorage and sends as guest identifier.
     */
    @Column(name = "session_uuid", nullable = false, unique = true, length = 64)
    private String sessionUuid;

    /**
     * The customer associated with this guest session.
     * Nullable initially — created on first guest profile submission.
     */
    @Column(name = "customer_id", nullable = true)
    private Long customerId;

    /**
     * Whether this guest session is revoked (banned).
     * Revoked sessions result in 401 — affects ONLY this session.
     */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this guest session token expires.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public static String generateSessionUuid() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}