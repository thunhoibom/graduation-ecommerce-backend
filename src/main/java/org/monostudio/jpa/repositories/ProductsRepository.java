package org.monostudio.jpa.repositories;

import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductCategory;

import java.util.Collection;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface ProductsRepository
    extends Repository<Product> {

    @Query(value = "SELECT p FROM Product p JOIN FETCH p.productCategory", countQuery = "SELECT p FROM Product p")
    Page<Product> deepReadAll(Pageable pageable);

    @Query(value = "SELECT p FROM Product p JOIN FETCH p.productCategory", countQuery = "SELECT p FROM Product p")
    Page<Product> deepReadAll(Predicate filters, Pageable pageable);

    Optional<Product> findByBarcode(String barcode);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.productCategory.id = :categoryId WHERE p.id = :id")
    void setProductCategoryById(@Param("id") Long productId, @Param("categoryId") Long categoryId);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.productCategory = null WHERE p.productCategory IN (:categories)")
    void orphanizeByCategories(@Param("categories") Collection<ProductCategory> categories);

    /**
     * Atomically decrements the stock of a product.
     * Used when an order is paid and there is no ProductVariant.
     *
     * @param id  the product ID
     * @param qty the quantity to deduct (must be positive)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stockCurrent = p.stockCurrent - :qty WHERE p.id = :id AND p.stockCurrent >= :qty")
    void decrementStock(@Param("id") Long id, @Param("qty") int qty);
}
