package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.Salesperson;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface SalespeopleRepository
    extends Repository<Salesperson> {

    @Query(value = "SELECT s FROM Salesperson s JOIN FETCH s.person p WHERE p.idNumber = :idNumber")
    Optional<Salesperson> findByPersonIdNumber(@Param("idNumber") String idNumber);
}
