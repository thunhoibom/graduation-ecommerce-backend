package org.monostudio.jpa.repositories;

import org.monostudio.jpa.entities.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewImagesRepository extends JpaRepository<ReviewImage, Long> {
    List<ReviewImage> findByReviewId(Long reviewId);
}
