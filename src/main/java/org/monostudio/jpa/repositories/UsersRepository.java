package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.User;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface UsersRepository
    extends Repository<User> {

    Optional<User> findByName(String name);

    @Query("SELECT u FROM User u JOIN FETCH u.userRole WHERE u.name = :name")
    Optional<User> findByNameWithRole(@Param("name") String name);

    @Query("SELECT u FROM User u JOIN FETCH u.person WHERE u.name = :name")
    Optional<User> findByNameWithProfile(@Param("name") String name);

    @Query("SELECT u FROM User u JOIN FETCH u.person WHERE u.id = :id")
    Optional<User> findByIdWithProfile(@Param("id") Long id);

    @Query("SELECT u FROM User u JOIN FETCH u.person p WHERE p.idNumber = :idNumber")
    Optional<User> findByPersonIdNumber(@Param("idNumber") String idNumber);

    @Query("SELECT u FROM User u JOIN FETCH u.person p WHERE p.email = :email")
    Optional<User> findByPersonEmail(@Param("email") String email);

    @Query("SELECT u FROM User u JOIN FETCH u.person p WHERE LOWER(p.email) = LOWER(:email)")
    Optional<User> findByPersonEmailIgnoreCase(@Param("email") String email);
}
