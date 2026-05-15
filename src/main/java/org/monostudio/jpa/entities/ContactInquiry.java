package org.monostudio.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.monostudio.jpa.DBEntity;

import java.time.LocalDateTime;

/**
 * Append-only contact submissions. Index on {@code contact_inquiry_created_at} is defined in
 * {@code database-migrations.sql} — not on the entity, so Hibernate does not emit CREATE INDEX when the table is owned by another DB role.
 */
@Entity
@Table(name = "contact_inquiries")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "message", "userAgent" })
public class ContactInquiry implements DBEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_inquiry_id", nullable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @CreationTimestamp
    @Column(name = "contact_inquiry_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Size(max = 200)
    @Column(name = "contact_inquiry_name", nullable = false, length = 200)
    private String name;

    @Size(max = 254)
    @Column(name = "contact_inquiry_email", nullable = false, length = 254)
    private String email;

    @Size(max = 40)
    @Column(name = "contact_inquiry_phone", length = 40)
    private String phone;

    @Size(max = 64)
    @Column(name = "contact_inquiry_subject", nullable = false, length = 64)
    private String subject;

    @Column(name = "contact_inquiry_message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Size(max = 64)
    @Column(name = "contact_inquiry_submitter_ip", length = 64)
    private String submitterIp;

    @Size(max = 512)
    @Column(name = "contact_inquiry_user_agent", length = 512)
    private String userAgent;
}
