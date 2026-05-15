package org.monostudio.jpa.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.monostudio.jpa.entities.StockAdjustment;

import java.time.Instant;
import java.util.List;

@Repository
public interface StockAdjustmentsRepository
    extends JpaRepository<StockAdjustment, Long> {
    List<StockAdjustment> findByVariantId(Long variantId);
    Page<StockAdjustment> findByVariantId(Long variantId, Pageable pageable);
    List<StockAdjustment> findByOrderId(Long orderId);
    List<StockAdjustment> findByReturnRequestId(Long returnRequestId);
    List<StockAdjustment> findByReason(StockAdjustment.StockAdjustmentReason reason);
    List<StockAdjustment> findByDateBetween(Instant start, Instant end);

    @Query("SELECT sa FROM StockAdjustment sa "
        + "JOIN FETCH sa.variant v "
        + "JOIN FETCH v.product p "
        + "WHERE sa.variant.id = :variantId "
        + "ORDER BY sa.date DESC")
    List<StockAdjustment> findByVariantIdDeep(@Param("variantId") Long variantId);

    @Query("SELECT sa FROM StockAdjustment sa "
        + "JOIN FETCH sa.variant v "
        + "JOIN FETCH v.product p "
        + "WHERE sa.date BETWEEN :start AND :end "
        + "ORDER BY sa.date DESC")
    Page<StockAdjustment> findByDateRangeDeep(
        @Param("start") Instant start,
        @Param("end") Instant end,
        Pageable pageable);

    @Query("SELECT sa FROM StockAdjustment sa "
        + "JOIN FETCH sa.variant v "
        + "JOIN FETCH v.product p "
        + "ORDER BY sa.date DESC")
    Page<StockAdjustment> findAllDeep(Pageable pageable);
}
