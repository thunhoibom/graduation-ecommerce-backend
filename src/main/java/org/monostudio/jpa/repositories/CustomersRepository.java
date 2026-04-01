package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.Customer;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface CustomersRepository
    extends Repository<Customer> {

    @Query(value = "SELECT c FROM Customer c JOIN FETCH c.person p WHERE p.idNumber = :idNumber")
    Optional<Customer> findByPersonIdNumber(@Param("idNumber") String idNumber);
}
