package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.ProductImage;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface ProductImagesRepository
    extends Repository<ProductImage> {

    List<ProductImage> findByProductId(long productId);

    @Query("SELECT pi FROM ProductImage pi JOIN FETCH pi.image WHERE pi.product.id = :id")
    List<ProductImage> deepFindProductImagesByProductId(@Param("id") long id);

    @Query("SELECT pi FROM ProductImage pi JOIN FETCH pi.image WHERE pi.product.id = :id ORDER BY pi.sortOrder ASC")
    List<ProductImage> deepFindProductImagesByProductIdOrdered(@Param("id") long id);

    @Query("SELECT pi FROM ProductImage pi JOIN FETCH pi.image WHERE pi.product.id = :id AND pi.isPrimary = true")
    Optional<ProductImage> findPrimaryByProductId(@Param("id") long id);

    @Query("SELECT pi FROM ProductImage pi JOIN FETCH pi.image WHERE pi.product.id IN :productIds AND pi.isPrimary = true")
    List<ProductImage> findPrimaryByProductIds(@Param("productIds") java.util.Collection<Long> productIds);

    @Modifying
    @Query("DELETE FROM ProductImage pi WHERE pi.product.id = :id")
    int deleteByProductId(@Param("id") long id);
}
