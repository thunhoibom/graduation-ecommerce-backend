package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.Person;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface PeopleRepository
    extends Repository<Person> {

    Optional<Person> findByIdNumber(String idNumber);

    Optional<Person> findByEmail(String email);

    boolean existsByEmail(String email);
}
