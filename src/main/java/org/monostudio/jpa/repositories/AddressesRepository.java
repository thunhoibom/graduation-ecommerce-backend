package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.Address;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface AddressesRepository
    extends Repository<Address> {

    @Query(value = "SELECT a FROM Address a WHERE a.city = :city "
        + "AND a.municipality = :municipality "
        + "AND a.firstLine = :firstLine "
        + "AND a.secondLine = :secondLine "
        + "AND a.postalCode = :postalCode "
        + "AND a.notes = :notes")
    Optional<Address> findByFields(
        @Param("city") String city,
        @Param("municipality") String municipality,
        @Param("firstLine") String firstLine,
        @Param("secondLine") String secondLine,
        @Param("postalCode") String postalCode,
        @Param("notes") String notes);
}
