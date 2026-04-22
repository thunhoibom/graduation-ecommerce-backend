package org.monostudio.jpa.repositories;

import java.util.List;
import java.util.Optional;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.Supplier;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@org.springframework.stereotype.Repository
public interface SuppliersRepository
    extends Repository<Supplier> {

    Optional<Supplier> findByCode(String code);

    @Query("SELECT s FROM Supplier s WHERE s.deleted = false ORDER BY s.name ASC")
    List<Supplier> findAllActive();

    @Query("""
        SELECT s FROM Supplier s
        WHERE s.deleted = false
          AND (:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY s.name ASC
        """)
    List<Supplier> search(@Param("keyword") String keyword);
}
