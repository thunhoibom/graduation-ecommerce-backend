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
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "order_statuses")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class OrderStatus
    implements DBEntity {
    private static final long serialVersionUID = 16L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_status_id", nullable = false)
    private Long id;
    @Column(name = "order_status_code", nullable = false, unique = true)
    private Integer code;
    @Size(min = 1, max = 100)
    @Column(name = "order_status_name", nullable = false, unique = true)
    private String name;

    public OrderStatus(OrderStatus source) {
        this.id = source.id;
        this.code = source.code;
        this.name = source.name;
    }
}
