package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.UserRole;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface UserRolesRepository
    extends Repository<UserRole> {

    @Query
    Optional<UserRole> findByName(String name);
}
