package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;

@org.springframework.stereotype.Repository
public interface ParamsRepository
    extends Repository<org.monostudio.jpa.entities.Param> {

    @Query("SELECT p FROM Param p WHERE p.category = :category")
    Iterable<org.monostudio.jpa.entities.Param> findParamsByCategory(
        @Param("category") String category);
}
