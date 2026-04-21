package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.StockReservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface StockReservationsRepository
    extends Repository<StockReservation> {

    List<StockReservation> findBySessionIdAndStatus(String sessionId, String status);

    List<StockReservation> findBySessionId(String sessionId);

    List<StockReservation> findByStatus(String status);

    @Query("SELECT r FROM StockReservation r "
        + "JOIN FETCH r.variant v "
        + "JOIN FETCH v.product p "
        + "WHERE r.sessionId = :sessionId "
        + "AND r.status = :status")
    List<StockReservation> findBySessionIdAndStatusDeep(
        @Param("sessionId") String sessionId,
        @Param("status") String status);

    @Query("SELECT r FROM StockReservation r "
        + "JOIN FETCH r.variant v "
        + "JOIN FETCH v.product p "
        + "WHERE r.sessionId = :sessionId")
    List<StockReservation> findBySessionIdDeep(@Param("sessionId") String sessionId);

    @Modifying
    @Transactional
    @Query("UPDATE StockReservation r SET r.status = :newStatus "
        + "WHERE r.id = :id AND r.status = :currentStatus")
    int updateStatus(@Param("id") Long id,
                     @Param("currentStatus") String currentStatus,
                     @Param("newStatus") String newStatus);

    @Modifying
    @Transactional
    @Query("UPDATE StockReservation r SET r.status = :newStatus "
        + "WHERE r.sessionId = :sessionId AND r.status = :currentStatus")
    int updateStatusBySessionId(@Param("sessionId") String sessionId,
                                @Param("currentStatus") String currentStatus,
                                @Param("newStatus") String newStatus);

    /**
     * Atomically increment stockReserved on a variant.
     * Returns the new reserved quantity, or empty if insufficient stock.
     */
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE product_variants
        SET variant_stock_reserved = variant_stock_reserved + :delta
        WHERE variant_id = :variantId
          AND (variant_stock_current - variant_stock_reserved) >= :delta
          AND variant_active = true
        """, nativeQuery = true)
    int tryIncrementReserved(@Param("variantId") Long variantId, @Param("delta") int delta);

    /**
     * Atomically decrement stockReserved on a variant (release reservation).
     */
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE product_variants
        SET variant_stock_reserved = GREATEST(variant_stock_reserved - :delta, 0)
        WHERE variant_id = :variantId
        """, nativeQuery = true)
    void decrementReserved(@Param("variantId") Long variantId, @Param("delta") int delta);

    /**
     * Atomically deduct stockCurrent and decrement stockReserved on payment confirmation.
     * The full quantity (reserved amount) is deducted from current stock.
     */
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE product_variants
        SET variant_stock_current = variant_stock_current - :quantity,
            variant_stock_reserved = GREATEST(variant_stock_reserved - :quantity, 0)
        WHERE variant_id = :variantId
          AND variant_stock_current >= :quantity
        """, nativeQuery = true)
    int confirmDeduct(@Param("variantId") Long variantId, @Param("quantity") int quantity);

    /**
     * Restore stockCurrent AND decrement stockReserved when a paid order is cancelled or rejected.
     * This reverses the deduction made at payment confirmation (confirmDeduct).
     *
     * correct invariant after cancel/reject:
     *   stockCurrent    += quantity   (return sold units to inventory)
     *   stockReserved  -= quantity   (clear the phantom reservation from the original cart session)
     *
     * @see #confirmDeduct(Long, int)  the inverse operation
     */
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE product_variants
        SET variant_stock_current  = variant_stock_current  + :quantity,
            variant_stock_reserved = GREATEST(variant_stock_reserved - :quantity, 0)
        WHERE variant_id = :variantId
        """, nativeQuery = true)
    void restoreStock(@Param("variantId") Long variantId, @Param("quantity") int quantity);

    /**
     * Find all expired reservations that are still in RESERVED status.
     */
    @Query("SELECT r FROM StockReservation r "
        + "WHERE r.status = 'RESERVED' "
        + "AND r.expiresAt < :now")
    List<StockReservation> findExpiredReservations(@Param("now") LocalDateTime now);

    /**
     * Total reserved quantity for a variant across all active sessions.
     */
    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM StockReservation r "
        + "WHERE r.variant.id = :variantId AND r.status = 'RESERVED'")
    int sumReservedByVariantId(@Param("variantId") Long variantId);

    Optional<StockReservation> findByIdAndStatus(Long id, String status);

    @Query("SELECT r.variant.id, SUM(r.quantity) FROM StockReservation r "
        + "WHERE r.sessionId = :sessionId AND r.status = 'RESERVED' "
        + "GROUP BY r.variant.id")
    List<Object[]> sumReservedBySessionId(@Param("sessionId") String sessionId);
}
