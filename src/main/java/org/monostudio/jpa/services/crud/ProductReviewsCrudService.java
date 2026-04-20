package org.monostudio.jpa.services.crud;

import org.monostudio.api.models.ProductReviewPojo;
import org.monostudio.api.models.ProductReviewReplyPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductReview;
import org.monostudio.jpa.services.CrudService;
import org.monostudio.jpa.services.crud.impl.ProductReviewsCrudServiceImpl;

import java.util.List;

public interface ProductReviewsCrudService
    extends CrudService<ProductReviewPojo, ProductReview> {

    /**
     * Submit a new product review for the given customer.
     */
    ProductReviewPojo createReview(ProductReviewPojo input, Long customerId) throws BadInputException;

    /**
     * Submit a reply to a review as a customer.
     */
    ProductReviewReplyPojo createReply(Long reviewId, String body, Long customerId) throws BadInputException;

    /**
     * Submit a reply to a review as a staff member (User).
     */
    ProductReviewReplyPojo createReplyAdmin(Long reviewId, String body, Long userId) throws BadInputException;

    /**
     * Public: list approved reviews for a product by product ID.
     */
    List<ProductReviewPojo> readApprovedByProduct(Long productId);

    /**
     * Public: list approved reviews for a product by barcode.
     */
    List<ProductReviewPojo> readApprovedByProductBarcode(String barcode);

    /**
     * Customer: list all reviews written by the given customer.
     */
    List<ProductReviewPojo> readByCustomer(Long customerId);

    /**
     * Admin: approve or reject a review.
     */
    ProductReviewPojo setApproval(Long reviewId, boolean approved);

    /**
     * Get aggregate review statistics for a product.
     */
    ProductReviewsCrudServiceImpl.ReviewStats getReviewStats(Long productId);
}
