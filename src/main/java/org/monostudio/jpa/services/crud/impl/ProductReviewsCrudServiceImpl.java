package org.monostudio.jpa.services.crud.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ProductReviewPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductReview;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.ProductReviewsRepository;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.services.conversion.ProductReviewsConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.ProductReviewsCrudService;
import org.monostudio.jpa.services.patch.ProductReviewsPatchService;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductReviewsCrudServiceImpl
    extends CrudGenericService<ProductReviewPojo, ProductReview>
    implements ProductReviewsCrudService {

    private final ProductReviewsRepository productReviewsRepository;
    private final ProductReviewsConverterService productReviewsConverterService;
    private final ProductsRepository productsRepository;
    private final CustomersRepository customersRepository;
    private final OrdersRepository ordersRepository;

    @Autowired
    public ProductReviewsCrudServiceImpl(
        ProductReviewsRepository productReviewsRepository,
        ProductReviewsConverterService productReviewsConverterService,
        ProductReviewsPatchService productReviewsPatchService,
        ProductsRepository productsRepository,
        CustomersRepository customersRepository,
        OrdersRepository ordersRepository
    ) {
        super(productReviewsRepository, productReviewsConverterService, productReviewsPatchService);
        this.productReviewsRepository = productReviewsRepository;
        this.productReviewsConverterService = productReviewsConverterService;
        this.productsRepository = productsRepository;
        this.customersRepository = customersRepository;
        this.ordersRepository = ordersRepository;
    }

    /**
     * Not supported — product review creation requires a customerId.
     * Use {@link #createReview(ProductReviewPojo, Long)} instead.
     */
    @Override
    public ProductReviewPojo create(ProductReviewPojo input) throws BadInputException {
        throw new UnsupportedOperationException(
            "Product review creation requires a customerId. Use createReview(input, customerId) instead.");
    }

    /**
     * Submit a new product review.
     * verifiedPurchase is set to true only if the customer has a completed order containing the product.
     */
    @Transactional
    public ProductReviewPojo createReview(ProductReviewPojo input, Long customerId) throws BadInputException {
        this.validateInputPojoBeforeCreation(input);
        validateNoDuplicateReview(input, customerId);

        Customer customer = customersRepository.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        ProductReview prepared = productReviewsConverterService.convertToNewEntity(input);
        prepared.setCustomer(customer);

        // Check for verified purchase
        Long productId = prepared.getProduct().getId();
        boolean verified = hasCompletedOrderContainingProduct(customerId, productId);
        prepared.setVerifiedPurchase(verified);

        ProductReview saved = productReviewsRepository.saveAndFlush(prepared);
        return productReviewsConverterService.convertToPojo(saved);
    }

    /**
     * Public: list approved reviews for a product.
     */
    @Transactional(readOnly = true)
    public List<ProductReviewPojo> readApprovedByProduct(Long productId) {
        return productReviewsRepository.findByProductIdAndApprovedTrue(productId).stream()
            .map(productReviewsConverterService::convertToPojo)
            .collect(Collectors.toList());
    }

    /**
     * Public: list approved reviews for a product by barcode.
     */
    @Transactional(readOnly = true)
    public List<ProductReviewPojo> readApprovedByProductBarcode(String barcode) {
        Product product = productsRepository.findByBarcode(barcode)
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + barcode));
        return readApprovedByProduct(product.getId());
    }

    /**
     * Customer: list all their reviews (including unapproved).
     */
    @Transactional(readOnly = true)
    public List<ProductReviewPojo> readByCustomer(Long customerId) {
        return productReviewsRepository.findByCustomerId(customerId).stream()
            .map(productReviewsConverterService::convertToPojo)
            .collect(Collectors.toList());
    }

    /**
     * Admin: approve or reject a review.
     */
    @Transactional
    public ProductReviewPojo setApproval(Long reviewId, boolean approved) {
        ProductReview review = productReviewsRepository.findById(reviewId)
            .orElseThrow(() -> new EntityNotFoundException("Review not found"));
        review.setApproved(approved);
        ProductReview saved = productReviewsRepository.saveAndFlush(review);
        return productReviewsConverterService.convertToPojo(saved);
    }

    /**
     * Get review statistics for a product.
     */
    @Transactional(readOnly = true)
    public ReviewStats getReviewStats(Long productId) {
        long total = productReviewsRepository.countByProductIdAndApprovedTrue(productId);
        Optional<Double> avgOpt = productReviewsRepository.findAverageRatingByProductId(productId);
        double avgRating = avgOpt.orElse(0.0);

        int[] distribution = new int[5]; // rating 1-5
        for (int r = 1; r <= 5; r++) {
            distribution[r - 1] = (int) productReviewsRepository
                .countByProductIdAndRatingAndApproved(productId, r);
        }

        return new ReviewStats(total, avgRating, distribution);
    }

    @Override
    protected void validateInputPojoBeforeCreation(ProductReviewPojo inputPojo) throws BadInputException {
        if (inputPojo.getRating() == null) {
            throw new BadInputException("Rating is required");
        }
        if (inputPojo.getRating() < 1 || inputPojo.getRating() > 5) {
            throw new BadInputException("Rating must be between 1 and 5");
        }
        if (inputPojo.getProductBarcode() == null || inputPojo.getProductBarcode().isBlank()) {
            throw new BadInputException("Product barcode is required");
        }
    }

    /**
     * Prevent duplicate reviews: a customer can only submit one review per product.
     * Note: this is validated in createReview() after resolving customerId.
     */
    @Override
    public Optional<ProductReview> getExisting(ProductReviewPojo input) throws BadInputException {
        // Duplicate prevention is handled in createReview after we know the customerId
        return Optional.empty();
    }

    /**
     * Validates that the customer hasn't already reviewed this product.
     */
    public void validateNoDuplicateReview(ProductReviewPojo input, Long customerId) throws BadInputException {
        String barcode = input.getProductBarcode();
        if (barcode == null || barcode.isBlank()) return; // validated elsewhere
        Long productId = productsRepository.findByBarcode(barcode)
            .map(Product::getId)
            .orElse(null);
        if (productId != null && productReviewsRepository.existsByProductIdAndCustomerId(productId, customerId)) {
            throw new BadInputException("You have already submitted a review for this product");
        }
    }

    private boolean hasCompletedOrderContainingProduct(Long customerId, Long productId) {
        // Check if customer has a completed (paid/confirmed) order with this product
        return ordersRepository.hasCompletedOrderWithProduct(customerId, productId);
    }

    // Inner DTO for review statistics
    public static class ReviewStats {
        private final long totalReviews;
        private final double averageRating;
        private final int[] ratingDistribution; // index 0 = count of 1-star, etc.

        public ReviewStats(long totalReviews, double averageRating, int[] ratingDistribution) {
            this.totalReviews = totalReviews;
            this.averageRating = Math.round(averageRating * 10.0) / 10.0; // round to 1dp
            this.ratingDistribution = ratingDistribution;
        }

        public long getTotalReviews() { return totalReviews; }
        public double getAverageRating() { return averageRating; }
        public int[] getRatingDistribution() { return ratingDistribution; }
    }
}
