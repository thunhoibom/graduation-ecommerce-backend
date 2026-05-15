package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.ProductReviewPojo;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.services.crud.ProductReviewsCrudService;
import org.monostudio.jpa.services.crud.impl.ProductReviewsCrudServiceImpl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/public/products")
@Tag(name = "Product Reviews")
public class PublicProductReviewsController {

    private final ProductReviewsCrudService productReviewsCrudService;
    private final ProductsRepository productsRepository;

    @Autowired
    public PublicProductReviewsController(
        ProductReviewsCrudService productReviewsCrudService,
        ProductsRepository productsRepository
    ) {
        this.productReviewsCrudService = productReviewsCrudService;
        this.productsRepository = productsRepository;
    }

    /**
     * Public: list all approved reviews for a product by barcode.
     * No authentication required.
     */
    @GetMapping("/{barcode}/reviews")
    @Operation(summary = "List all approved reviews for a product")
    public List<ProductReviewPojo> listByProduct(@PathVariable String barcode) {
        return productReviewsCrudService.readApprovedByProductBarcode(barcode);
    }

    /**
     * Public: get review statistics for a product by barcode.
     */
    @GetMapping("/{barcode}/reviews/stats")
    @Operation(summary = "Get review statistics (average rating, total count, distribution) for a product")
    public ProductReviewsCrudServiceImpl.ReviewStats getStats(@PathVariable String barcode) {
        Long productId = productsRepository.findByBarcode(barcode)
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + barcode))
            .getId();
        return productReviewsCrudService.getReviewStats(productId);
    }
}
