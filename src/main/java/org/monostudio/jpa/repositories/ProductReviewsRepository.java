package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.entities.ProductReview;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface ProductReviewsRepository
    extends org.monostudio.jpa.Repository<ProductReview> {

    List<ProductReview> findByProductId(Long productId);

    List<ProductReview> findByProductIdAndApprovedTrue(Long productId);

    List<ProductReview> findByCustomerId(Long customerId);

    Optional<ProductReview> findByProductIdAndCustomerId(Long productId, Long customerId);

    long countByProductId(Long productId);

    long countByProductIdAndApprovedTrue(Long productId);

    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.product.id = :productId AND r.approved = true")
    Optional<Double> findAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM ProductReview r WHERE r.product.id = :productId AND r.approved = true AND r.rating = :rating")
    long countByProductIdAndRatingAndApproved(@Param("productId") Long productId, @Param("rating") int rating);

    boolean existsByProductIdAndCustomerId(Long productId, Long customerId);
}
