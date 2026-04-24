package org.monostudio.jpa.services.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.monostudio.jpa.repositories.ImagesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ImagePojo;
import org.monostudio.api.models.ProductVariantPojo;
import org.monostudio.api.services.ProductPricingSnapshotService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Image;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.VariantImage;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.services.conversion.ImagesConverterService;
import org.monostudio.jpa.services.conversion.ProductVariantsConverterService;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

@Transactional
@Service
public class ProductVariantsConverterServiceImpl
    implements ProductVariantsConverterService {

    private final ProductsRepository productsRepository;
    private final ImagesRepository imagesRepository;
    private final ImagesConverterService imagesConverterService;
    private final ProductPricingSnapshotService productPricingSnapshotService;

    @Autowired
    public ProductVariantsConverterServiceImpl(
        ProductsRepository productsRepository,
        ImagesRepository imagesRepository,
        ImagesConverterService imagesConverterService,
        ProductPricingSnapshotService productPricingSnapshotService
    ) {
        this.productsRepository = productsRepository;
        this.imagesRepository = imagesRepository;
        this.imagesConverterService = imagesConverterService;
        this.productPricingSnapshotService = productPricingSnapshotService;
    }


    @Override
    public ProductVariantPojo convertToPojo(ProductVariant source) {
        ProductVariantPojo target = ProductVariantPojo.builder()
            .id(source.getId())
            .sku(source.getSku())
            .size(source.getSize())
            .color(source.getColor())
            .attributes(source.getAttributes())
            .priceModifier(source.getPriceModifier())
            .currentStock(source.getStockCurrent())
            .criticalStock(source.getStockCritical())
            .reservedStock(source.getStockReserved())
            .availableStock(source.getStockCurrent() - source.getStockReserved())
            .active(source.isActive())
            .barcode(source.getBarcode())
            .createdAt(source.getCreatedAt())
            .build();


        Product product = source.getProduct();
        if (product != null) {
            var pricing = productPricingSnapshotService.calculate(product);
            target.setProductBarcode(product.getBarcode());
            target.setProductName(product.getName());
            target.setProductBasePrice(pricing.currentPrice());
            target.setFinalPrice(pricing.currentPrice() + source.getPriceModifier());
        }

        // Map images
        if (source.getImages() != null && !source.getImages().isEmpty()) {
            target.setImages(new java.util.ArrayList<>(
                convertVariantImagesToPojo(source.getImages())
            ));

            target.setPrimaryImageUrl(extractPrimaryImageUrl(source.getImages()));
        }



        return target;
    }

    @Override
    public ProductVariant convertToNewEntity(ProductVariantPojo source) throws BadInputException {
        ProductVariant target = ProductVariant.builder()
            .sku(source.getSku())
            .size(source.getSize())
            .color(source.getColor())
            .attributes(source.getAttributes())
            .priceModifier(source.getPriceModifier() != null ? source.getPriceModifier() : 0)
            .active(source.getActive() != null ? source.getActive() : true)
            .barcode(source.getBarcode())
            .build();

        if (source.getCurrentStock() != null) {
            target.setStockCurrent(source.getCurrentStock());
        }

        if (source.getCriticalStock() != null) {
            target.setStockCritical(source.getCriticalStock());
        }

        if (!StringUtils.isBlank(source.getProductBarcode())) {
            productsRepository.findByBarcode(source.getProductBarcode())
                .ifPresent(target::setProduct);
        } else {
            throw new BadInputException("Product barcode is required to create a variant");
        }

        // Initialize images collection (Limit to 1 as requested)
        if (source.getImages() != null && !source.getImages().isEmpty()) {
            java.util.List<VariantImage> variantImages = source.getImages().stream()
                .limit(1)
                .map(imgPojo -> imagesRepository.findByCode(imgPojo.getCode()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(imgEntity -> VariantImage.builder()
                        .variant(target)
                        .image(imgEntity)
                        .isPrimary(true)
                        .sortOrder(0)
                        .build())
                .collect(Collectors.toList());
            target.setImages(variantImages);
        }



        return target;
    }

    @Override
    public ProductVariant applyChangesToExistingEntity(ProductVariantPojo source, ProductVariant target) {
        target.setSku(source.getSku());
        target.setSize(source.getSize());
        target.setColor(source.getColor());
        target.setAttributes(source.getAttributes());
        target.setPriceModifier(source.getPriceModifier() != null ? source.getPriceModifier() : 0);
        target.setActive(source.getActive() != null ? source.getActive() : true);
        target.setBarcode(source.getBarcode());

        if (source.getCurrentStock() != null) {
            target.setStockCurrent(source.getCurrentStock());
        }
        if (source.getCriticalStock() != null) {
            target.setStockCritical(source.getCriticalStock());
        }

        // Update images (Limit to 1 as requested)
        if (source.getImages() != null) {
            target.getImages().clear();
            java.util.List<VariantImage> newImages = source.getImages().stream()
                .limit(1)
                .map(imgPojo -> imagesRepository.findByCode(imgPojo.getCode()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(imgEntity -> VariantImage.builder()
                        .variant(target)
                        .image(imgEntity)
                        .isPrimary(true)
                        .sortOrder(0)
                        .build())
                .collect(Collectors.toList());
            target.getImages().addAll(newImages);
        }



        return target;
    }


    @Override
    public Collection<ImagePojo> convertVariantImagesToPojo(Collection<VariantImage> variantImages) {
        return variantImages.stream()
            .sorted(Comparator.comparingInt(vi -> vi.getSortOrder() != null ? vi.getSortOrder() : 0))
            .map(VariantImage::getImage)
            .map(imagesConverterService::convertToPojo)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public String extractPrimaryImageUrl(Collection<VariantImage> variantImages) {
        return variantImages.stream()
            .filter(vi -> Boolean.TRUE.equals(vi.getIsPrimary()))
            .map(VariantImage::getImage)
            .map(Image::getUrl)
            .findFirst()
            .orElse(
                variantImages.stream()
                    .sorted(Comparator.comparingInt(vi -> vi.getSortOrder() != null ? vi.getSortOrder() : 0))
                    .map(VariantImage::getImage)
                    .map(Image::getUrl)
                    .findFirst()
                    .orElse(null)
            );
    }
}
