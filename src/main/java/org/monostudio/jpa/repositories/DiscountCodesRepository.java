package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.DiscountCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface DiscountCodesRepository
    extends Repository<DiscountCode> {

    Optional<DiscountCode> findByCodeIgnoreCase(String code);

    List<DiscountCode> findByActiveTrue();

    @Query("SELECT d FROM DiscountCode d WHERE d.active = true "
        + "AND (d.validFrom IS NULL OR d.validFrom <= :now) "
        + "AND (d.validUntil IS NULL OR d.validUntil >= :now)")
    List<DiscountCode> findCurrentlyValid(@Param("now") LocalDateTime now);

    @Query("SELECT d FROM DiscountCode d WHERE d.active = true "
        + "AND d.code = :code "
        + "AND (d.validFrom IS NULL OR d.validFrom <= :now) "
        + "AND (d.validUntil IS NULL OR d.validUntil >= :now)")
    Optional<DiscountCode> findValidByCode(@Param("code") String code, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE DiscountCode d SET d.useCount = d.useCount + 1 WHERE d.id = :id AND d.active = true")
    int incrementUseCount(@Param("id") Long id);
}
