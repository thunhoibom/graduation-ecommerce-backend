package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.PromotionRule;

import java.time.LocalDateTime;
import java.util.List;

@org.springframework.stereotype.Repository
public interface PromotionRulesRepository extends Repository<PromotionRule> {

    @Query(
        "SELECT r FROM PromotionRule r WHERE r.active = true "
            + "AND (r.activeFrom IS NULL OR r.activeFrom <= :now) "
            + "AND (r.activeUntil IS NULL OR r.activeUntil >= :now) "
            + "ORDER BY r.priority DESC"
    )
    List<PromotionRule> findAllActive(@Param("now") LocalDateTime now);
}
