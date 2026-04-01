package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "salespeople")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Salesperson
    implements DBEntity {
    private static final long serialVersionUID = 13L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salesperson_id", nullable = false)
    private Long id;
    @JoinColumn(name = "person_id", referencedColumnName = "person_id")
    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    private Person person;

    /**
     * Please note: this copy-constructor DOES include a Salesperson's relationship to its own profile data
     *
     * @param source The original Sell
     */
    public Salesperson(Salesperson source) {
        this.id = source.id;
        this.person = new Person(source.person);
    }
}
