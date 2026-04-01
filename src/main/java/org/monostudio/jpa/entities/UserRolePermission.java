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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user_role_permissions")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class UserRolePermission
    implements DBEntity {
    private static final long serialVersionUID = 21L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_role_permission_id", nullable = false)
    private Long id;
    @JoinColumn(name = "permission_id", referencedColumnName = "permission_id", updatable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Permission permission;
    @JoinColumn(name = "user_role_id", referencedColumnName = "user_role_id", updatable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private UserRole userRole;

    /**
     * Please note: this copy-constructor does not include a UserRolePermission's relationships
     *
     * @param source The original UserRolePermission
     */
    public UserRolePermission(UserRolePermission source) {
        this.id = source.id;
        this.permission = null;
        this.userRole = null;
    }
}
