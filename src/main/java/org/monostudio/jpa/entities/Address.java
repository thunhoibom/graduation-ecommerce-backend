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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;

@Entity
@Table(
    name = "addresses",
    indexes = {
        @Index(columnList = "address_first_line"),
        @Index(columnList = "address_second_line"),
        @Index(columnList = "address_postal_code")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {
            "address_city", "address_municipality", "address_first_line",
            "address_second_line", "address_postal_code", "address_notes"
        })
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Address
    implements DBEntity {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id", nullable = false)
    private Long id;
    @Size(max = 50)
    @Column(name = "address_city", nullable = false)
    private String city;
    @Size(max = 50)
    @Column(name = "address_municipality", nullable = false)
    private String municipality;
    @Size(max = 100)
    @Column(name = "address_first_line", nullable = false)
    private String firstLine;
    @Size(max = 50)
    @Column(name = "address_second_line")
    private String secondLine;
    @Column(name = "address_postal_code")
    private String postalCode;
    @Size(max = 50)
    @Column(name = "address_notes")
    private String notes;
    @Column(name = "latitude")
    private Double latitude;
    @Column(name = "longitude")
    private Double longitude;

    public Address(Address source) {
        this.id = source.id;
        this.city = source.city;
        this.municipality = source.municipality;
        this.firstLine = source.firstLine;
        this.secondLine = source.secondLine;
        this.postalCode = source.postalCode;
        this.notes = source.notes;
        this.latitude = source.latitude;
        this.longitude = source.longitude;
    }
}
