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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

@Entity
@Table(
    name = "app_users",
    indexes = {
        @Index(columnList = "user_name")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class User
    implements DBEntity {
    private static final long serialVersionUID = 19L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long id;
    @Size(min = 1, max = 50)
    @Column(name = "user_name", nullable = false, unique = true)
    private String name;
    @Size(min = 1, max = 100)
    @Column(name = "user_password", nullable = false)
    private String password;
    @JoinColumn(name = "person_id", referencedColumnName = "person_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Person person;
    @JoinColumn(name = "user_role_id", referencedColumnName = "user_role_id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private UserRole userRole;

    /**
     * Please note that this copy-constructor only preserves a User's relationship to a role
     *
     * @param source The original User
     */
    public User(User source) {
        this.id = source.id;
        this.name = source.name;
        this.userRole = source.userRole;
        this.password = null;
        this.person = null;
    }
}
