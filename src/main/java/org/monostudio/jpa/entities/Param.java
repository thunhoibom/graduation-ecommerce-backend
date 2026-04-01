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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;

@Entity
@Table(
    name = "app_params",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"param_category", "param_name"})
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Param
    implements DBEntity {
    private static final long serialVersionUID = 6L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "param_id", nullable = false)
    private Long id;
    @Size(min = 1, max = 25)
    @Column(name = "param_category", nullable = false)
    private String category;
    @Size(min = 1, max = 50)
    @Column(name = "param_name", nullable = false)
    private String name;
    @Size(min = 1, max = 500)
    @Column(name = "param_value", nullable = false)
    private String value;

    public Param(Param source) {
        this.id = source.id;
        this.category = source.category;
        this.name = source.name;
        this.value = source.value;
    }
}
