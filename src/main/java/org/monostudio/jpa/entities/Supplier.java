package org.monostudio.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.Instant;
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

@Entity
@Table(
    name = "suppliers",
    indexes = {
        @Index(columnList = "supplier_code", unique = true),
        @Index(columnList = "supplier_name"),
        @Index(columnList = "supplier_active"),
        @Index(columnList = "supplier_deleted")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Supplier
    implements DBEntity {
    private static final long serialVersionUID = 31L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id", nullable = false)
    private Long id;

    @Size(max = 50)
    @Column(name = "supplier_code", nullable = false, unique = true, length = 50)
    private String code;

    @Size(max = 255)
    @Column(name = "supplier_name", nullable = false, length = 255)
    private String name;

    @Size(max = 255)
    @Column(name = "supplier_contact_name", length = 255)
    private String contactName;

    @Size(max = 100)
    @Column(name = "supplier_phone", length = 100)
    private String phone;

    @Size(max = 255)
    @Column(name = "supplier_email", length = 255)
    private String email;

    @Size(max = 500)
    @Column(name = "supplier_address", length = 500)
    private String address;

    @Builder.Default
    @Column(name = "supplier_active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "supplier_deleted", nullable = false)
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "supplier_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "supplier_updated_at")
    private Instant updatedAt;
}
