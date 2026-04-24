package org.monostudio.jpa.services.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ImagePojo;
import org.monostudio.api.models.ProductCategoryPojo;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.api.services.ProductPricingSnapshotService;
import org.monostudio.jpa.entities.Image;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.entities.ProductImage;
import org.monostudio.jpa.entities.ProductStatus;
import org.monostudio.jpa.repositories.ProductImagesRepository;
import org.monostudio.jpa.repositories.ProductReviewsRepository;
import org.monostudio.jpa.repositories.ProductsCategoriesRepository;
import org.monostudio.jpa.services.conversion.ImagesConverterService;
import org.monostudio.jpa.services.conversion.ProductCategoriesConverterService;
import org.monostudio.jpa.services.conversion.ProductsConverterService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Service
public class ProductsConverterServiceImpl
    implements ProductsConverterService {
    private final ProductImagesRepository productImagesRepository;
    private final ImagesConverterService imagesConverterService;
    private final ProductsCategoriesRepository productsCategoriesRepository;
    private final ProductCategoriesConverterService productCategoriesConverterService;
    private final ProductReviewsRepository productReviewsRepository;
    private final ProductPricingSnapshotService productPricingSnapshotService;

    @Autowired
    public ProductsConverterServiceImpl(
        ProductImagesRepository productImagesRepository,
        ImagesConverterService imagesConverterService,
        ProductsCategoriesRepository productsCategoriesRepository,
        ProductCategoriesConverterService productCategoriesConverterService,
        ProductReviewsRepository productReviewsRepository,
        ProductPricingSnapshotService productPricingSnapshotService
    ) {
        this.productImagesRepository = productImagesRepository;
        this.imagesConverterService = imagesConverterService;
        this.productsCategoriesRepository = productsCategoriesRepository;
        this.productCategoriesConverterService = productCategoriesConverterService;
        this.productReviewsRepository = productReviewsRepository;
        this.productPricingSnapshotService = productPricingSnapshotService;
    }

    @Override
    public ProductPojo convertToPojo(Product source) {
        int currentStock = source.getStockCurrent();
        int reservedStock = 0;
        int criticalStock = source.getStockCritical();
        if (source.getVariants() != null && !source.getVariants().isEmpty()) {
            var activeVariants = source.getVariants().stream()
                .filter(org.monostudio.jpa.entities.ProductVariant::isActive)
                .toList();
            var stockSource = activeVariants.isEmpty() ? source.getVariants() : activeVariants;
            currentStock = stockSource.stream().mapToInt(org.monostudio.jpa.entities.ProductVariant::getStockCurrent).sum();
            reservedStock = stockSource.stream().mapToInt(org.monostudio.jpa.entities.ProductVariant::getStockReserved).sum();
            criticalStock = stockSource.stream().mapToInt(org.monostudio.jpa.entities.ProductVariant::getStockCritical).sum();
        }

        var pricing = productPricingSnapshotService.calculate(source);

        ProductPojo target = ProductPojo.builder()
            .id(source.getId())
            .name(source.getName())
            .barcode(source.getBarcode())
            .price(pricing.currentPrice())
            .originalPrice(pricing.originalPrice())
            .currentPrice(pricing.currentPrice())
            .discountPercent(pricing.discountPercent())
            .hasDiscount(pricing.hasDiscount())
            .discountActiveFrom(pricing.activeFrom())
            .discountActiveUntil(pricing.activeUntil())
            .description(source.getDescription())
            .currentStock(currentStock)
            .reservedStock(reservedStock)
            .criticalStock(criticalStock)
            .status(source.getStatus() != null ? source.getStatus().name() : ProductStatus.DRAFT.name())
            .build();

        ProductCategory category = source.getProductCategory();
        if (category!=null) {
            ProductCategoryPojo categoryPojo = productCategoriesConverterService.convertToPojo(category);
            target.setCategory(categoryPojo);
        }

        // Populate review statistics
        populateReviewStats(source.getId(), target);

        // Populate images
        List<ProductImage> images = productImagesRepository.deepFindProductImagesByProductIdOrdered(source.getId());
        if (images != null && !images.isEmpty()) {
            target.setImages(convertImagesToPojo(images));
            target.setPrimaryImageUrl(extractPrimaryImageUrl(images));
        }

        return target;
    }


    /**
     * Populate review statistics for a product.
     * Called internally by convertToPojo. Safe to call from admin contexts
     * to include stats when returning product data.
     */
    public void populateReviewStats(Long productId, ProductPojo target) {
        long totalApproved = productReviewsRepository.countByProductIdAndApprovedTrue(productId);
        if (totalApproved > 0) {
            target.setTotalReviews((int) totalApproved);
            Optional<Double> avgOpt = productReviewsRepository.findAverageRatingByProductId(productId);
            avgOpt.ifPresent(avg -> target.setAverageRating(Math.round(avg * 10.0) / 10.0));
        }
    }

    @Override
    @Deprecated(forRemoval = true, since = "0.2.0-SNAPSHOT")
    public Product applyChangesToExistingEntity(ProductPojo source, Product target) {
        return convertToNewEntity(source); // deprecated — use PatchService instead
    }

    @Override
    public Product convertToNewEntity(ProductPojo source) {
        Product target = Product.builder()
            .name(source.getName())
            .barcode(source.getBarcode())
            .price(source.getPrice())
            .description(source.getDescription())
            .build();

        if (source.getCurrentStock()!=null) {
            target.setStockCurrent(source.getCurrentStock());
        }

        if (source.getCriticalStock()!=null) {
            target.setStockCritical(source.getCriticalStock());
        }

        ProductCategoryPojo sourceCategory = source.getCategory();
        if (sourceCategory!=null && !StringUtils.isBlank(sourceCategory.getCode())) {
            productsCategoriesRepository.findByCode(sourceCategory.getCode()).ifPresent(target::setProductCategory);
        }

        // Set status — default to DRAFT if not specified
        if (!StringUtils.isBlank(source.getStatus())) {
            try {
                target.setStatus(ProductStatus.valueOf(source.getStatus()));
            } catch (IllegalArgumentException exc) {
                target.setStatus(ProductStatus.DRAFT);
            }
        } else {
            target.setStatus(ProductStatus.DRAFT);
        }

        return target;
    }

    @Override
    public Collection<ImagePojo> convertImagesToPojo(Collection<ProductImage> productImages) {
        return productImages.stream()
            .sorted(java.util.Comparator.comparingInt(pi -> pi.getSortOrder() != null ? pi.getSortOrder() : 0))
            .map(ProductImage::getImage)
            .map(imagesConverterService::convertToPojo)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public String extractPrimaryImageUrl(Collection<ProductImage> productImages) {
        return productImages.stream()
            .filter(pi -> Boolean.TRUE.equals(pi.getIsPrimary()))
            .map(ProductImage::getImage)
            .map(Image::getUrl)
            .findFirst()
            .orElse(
                // Fallback: first image by sort order
                productImages.stream()
                    .sorted(java.util.Comparator.comparingInt(pi -> pi.getSortOrder() != null ? pi.getSortOrder() : 0))
                    .map(ProductImage::getImage)
                    .map(Image::getUrl)
                    .findFirst()
                    .orElse(null)
            );
    }

}
