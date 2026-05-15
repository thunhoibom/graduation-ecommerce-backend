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

    @Query(value = "SELECT c FROM Customer c JOIN FETCH c.person p WHERE p.email = :email")
    java.util.List<Customer> findAllByPersonEmail(@Param("email") String email);

    @Query(value = "SELECT c FROM Customer c JOIN FETCH c.person p WHERE LOWER(TRIM(p.email)) = :email")
    java.util.List<Customer> findAllByPersonEmailIgnoreCase(@Param("email") String email);

    /**
     * Find a Customer linked to a User by the user's person ID.
     * A User and their corresponding Customer share the same Person record.
     */
    @Query(value = "SELECT c FROM Customer c JOIN c.person p WHERE p.id = :personId")
    Optional<Customer> findByPersonId(@Param("personId") Long personId);
}
