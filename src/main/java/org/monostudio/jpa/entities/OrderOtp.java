package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
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
import java.time.Instant;

@Entity
@Table(
    name = "order_otp",
    indexes = {
        @Index(columnList = "otp_order_id"),
        @Index(columnList = "otp_expires_at"),
        @Index(columnList = "otp_created_at")
    }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class OrderOtp implements DBEntity {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "otp_order_id", nullable = false)
    private Order order;

    @Column(name = "otp_email", nullable = false, length = 254)
    private String email;

    @Column(name = "otp_hash", nullable = false, length = 120)
    private String hash;

    @Column(name = "otp_expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "otp_attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "otp_resend_count", nullable = false)
    @Builder.Default
    private int resendCount = 0;

    @Column(name = "otp_last_sent_at", nullable = false)
    private Instant lastSentAt;

    @Column(name = "otp_verified_at")
    private Instant verifiedAt;

    @Column(name = "otp_is_used", nullable = false)
    @Builder.Default
    private boolean used = false;

    @CreationTimestamp
    @Column(name = "otp_created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
