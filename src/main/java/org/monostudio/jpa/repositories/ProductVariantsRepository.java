package org.monostudio.jpa.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.ProductVariant;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface ProductVariantsRepository
    extends Repository<ProductVariant> {

    Optional<ProductVariant> findBySku(String sku);

    Optional<ProductVariant> findByBarcode(String barcode);

    List<ProductVariant> findByProductId(Long productId);

    List<ProductVariant> findByProductIdAndActive(Long productId, boolean active);

    Page<ProductVariant> findByProductId(Long productId, Pageable pageable);

    @Query(value = "SELECT v FROM ProductVariant v JOIN FETCH v.product", countQuery = "SELECT v FROM ProductVariant v")
    Page<ProductVariant> deepReadAll(Pageable pageable);

    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product WHERE v.product.id = :productId")
    List<ProductVariant> deepFindByProductId(@Param("productId") Long productId);

    @Query("SELECT v FROM ProductVariant v WHERE v.product.id IN :productIds")
    List<ProductVariant> findByProductIds(@Param("productIds") Collection<Long> productIds);

    @Modifying
    @Transactional
    @Query("UPDATE ProductVariant v SET v.stockCurrent = :stock WHERE v.id = :id")
    void setStockById(@Param("id") Long id, @Param("stock") int stock);

    @Modifying
    @Transactional
    @Query("UPDATE ProductVariant v SET v.active = :active WHERE v.id = :id")
    void setActiveById(@Param("id") Long id, @Param("active") boolean active);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariant> findByIdWithLock(@Param("id") Long id);

    // ─── Admin Dashboard Queries ───────────────────────────────────────────────

    /**
     * All active variants at or below their critical stock threshold.
     * Returns variant + product name via JOIN FETCH.
     */
    @Query("SELECT v FROM ProductVariant v "
        + "JOIN FETCH v.product p "
        + "WHERE v.stockCurrent <= v.stockCritical "
        + "AND v.active = true "
        + "ORDER BY v.stockCurrent ASC")
    List<ProductVariant> findLowStockAlerts();
}
