package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.Person;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface PeopleRepository
    extends Repository<Person> {

    Optional<Person> findByIdNumber(String idNumber);

    Optional<Person> findByEmail(String email);

    @Query("SELECT p FROM Person p WHERE LOWER(TRIM(p.email)) = :email")
    Optional<Person> findByEmailIgnoreCase(@Param("email") String email);

    boolean existsByEmail(String email);
}
