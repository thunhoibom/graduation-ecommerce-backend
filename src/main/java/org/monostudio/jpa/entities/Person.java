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
    name = "persons",
    indexes = {
        @Index(columnList = "person_id_number"),
        @Index(columnList = "person_first_name"),
        @Index(columnList = "person_last_name"),
        @Index(columnList = "person_email")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Person
    implements DBEntity {
    private static final long serialVersionUID = 9L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "person_id", nullable = false)
    private Long id;
    @Size(min = 1, max = 200)
    @Column(name = "person_first_name", nullable = false)
    private String firstName;
    @Size(min = 1, max = 200)
    @Column(name = "person_last_name", nullable = false)
    private String lastName;
    @Size(min = 1, max = 20)
    @Column(name = "person_id_number", nullable = true, unique = false)
    private String idNumber;
    @Size(min = 5, max = 100)
    @Column(name = "person_email", nullable = false)
    private String email;
    @Column(name = "person_phone1", nullable = false)
    @Builder.Default
    private String phone1 = "";
    @Column(name = "person_phone2", nullable = false)
    @Builder.Default
    private String phone2 = "";

    public Person(Person source) {
        this.id = source.id;
        this.firstName = source.firstName;
        this.lastName = source.lastName;
        this.idNumber = source.idNumber;
        this.email = source.email;
        this.phone1 = source.phone1;
        this.phone2 = source.phone2;
    }
}
