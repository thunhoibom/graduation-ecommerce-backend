package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

@Entity
@Table(
    name = "app_user_roles",
    indexes = {
        @Index(columnList = "user_role_name")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class UserRole
    implements DBEntity {
    private static final long serialVersionUID = 20L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_role_id", nullable = false)
    private Long id;
    @Size(min = 1, max = 50)
    @Column(name = "user_role_name", nullable = false, unique = true)
    private String name;

    public UserRole(UserRole source) {
        this.id = source.id;
        this.name = source.name;
    }
}
