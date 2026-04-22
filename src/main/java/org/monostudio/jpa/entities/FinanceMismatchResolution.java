package org.monostudio.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.monostudio.jpa.DBEntity;

import java.time.Instant;

@Entity
@Table(
    name = "finance_mismatch_resolutions",
    indexes = {
        @Index(columnList = "finance_mismatch_key", unique = true),
        @Index(columnList = "finance_mismatch_resolved_at")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class FinanceMismatchResolution
    implements DBEntity {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "finance_mismatch_resolution_id", nullable = false)
    private Long id;

    @Column(name = "finance_mismatch_key", nullable = false, unique = true, length = 128)
    private String mismatchKey;

    @Column(name = "finance_mismatch_note", length = 2000)
    private String note;

    @Column(name = "finance_mismatch_resolved_by", length = 255)
    private String resolvedBy;

    @CreationTimestamp
    @Column(name = "finance_mismatch_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "finance_mismatch_resolved_at", nullable = false)
    private Instant resolvedAt;
}
