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
@Table(name = "billing_companies")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class BillingCompany
    implements DBEntity {
    private static final long serialVersionUID = 2L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_company_id", nullable = false)
    private Long id;
    @Size(min = 1, max = 20)
    @Column(name = "billing_company_id_number", nullable = false, unique = true)
    private String idNumber;
    @Size(min = 1, max = 100)
    @Column(name = "billing_company_name", nullable = false, unique = true)
    private String name;

    public BillingCompany(BillingCompany source) {
        this.id = source.id;
        this.idNumber = source.idNumber;
        this.name = source.name;
    }
}
