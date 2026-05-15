package org.monostudio.jpa.repositories;

import org.monostudio.jpa.entities.ProductReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductReviewReplyRepository extends JpaRepository<ProductReviewReply, Long> {
    List<ProductReviewReply> findByReviewId(Long reviewId);
}
