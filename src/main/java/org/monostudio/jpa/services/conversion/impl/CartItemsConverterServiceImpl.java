package org.monostudio.jpa.services.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.CartItemPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.CartItem;
import org.monostudio.jpa.repositories.ProductImagesRepository;
import org.monostudio.jpa.repositories.VariantImagesRepository;
import org.monostudio.jpa.services.conversion.CartItemsConverterService;

@Transactional
@Service
public class CartItemsConverterServiceImpl
    implements CartItemsConverterService {

    private final VariantImagesRepository variantImagesRepository;
    private final ProductImagesRepository productImagesRepository;

    @Autowired
    public CartItemsConverterServiceImpl(
        VariantImagesRepository variantImagesRepository,
        ProductImagesRepository productImagesRepository
    ) {
        this.variantImagesRepository = variantImagesRepository;
        this.productImagesRepository = productImagesRepository;
    }

    @Override
    public CartItemPojo convertToPojo(CartItem source) {
        var variant = source.getVariant();
        CartItemPojo target = CartItemPojo.builder()
            .id(source.getId())
            .variantSku(variant != null ? variant.getSku() : null)
            .variantSkuResolved(variant != null ? variant.getSku() : null)
            .variantSize(variant != null ? variant.getSize() : null)
            .variantColor(variant != null ? variant.getColor() : null)
            .productName(variant != null && variant.getProduct() != null
                ? variant.getProduct().getName() : null)
            .productBarcode(variant != null && variant.getProduct() != null
                ? variant.getProduct().getBarcode() : null)
            .priceModifier(variant != null ? variant.getPriceModifier() : null)
            .unitPrice(variant != null && variant.getProduct() != null
                ? variant.getProduct().getPrice() + variant.getPriceModifier() : 0)
            .lineTotal(variant != null && variant.getProduct() != null
                ? (variant.getProduct().getPrice() + variant.getPriceModifier()) * source.getQuantity() : 0)
            .quantity(source.getQuantity())
            .addedAt(source.getAddedAt())
            .updatedAt(source.getUpdatedAt())
            .build();

        // Resolve image URL: variant primary -> variant first -> product primary -> product first -> null
        String productImageUrl = null;
        if (variant != null) {
            // 1. Try variant primary image
            var variantPrimary = variantImagesRepository.findPrimaryByVariantId(variant.getId());
            if (variantPrimary.isPresent()) {
                productImageUrl = variantPrimary.get().getImage().getUrl();
            } else {
                // 2. Try any variant image (first by sort order)
                var variantImages = variantImagesRepository.deepFindByVariantId(variant.getId());
                if (!variantImages.isEmpty()) {
                    productImageUrl = variantImages.get(0).getImage().getUrl();
                } else if (variant.getProduct() != null) {
                    // 3. Fallback to product primary image
                    var productPrimary = productImagesRepository.findPrimaryByProductId(variant.getProduct().getId());
                    if (productPrimary.isPresent()) {
                        productImageUrl = productPrimary.get().getImage().getUrl();
                    } else {
                        // 4. Last resort: any product image (first by sort order)
                        var productImages = productImagesRepository.deepFindProductImagesByProductIdOrdered(variant.getProduct().getId());
                        if (!productImages.isEmpty()) {
                            productImageUrl = productImages.get(0).getImage().getUrl();
                        }
                    }
                }
            }
        }
        target.setPrimaryImageUrl(productImageUrl);

        return target;
    }

    @Override
    public CartItem convertToNewEntity(CartItemPojo source) throws BadInputException {
        throw new UnsupportedOperationException(
            "CartItem entities must be created through CartService, not directly via CRUD");
    }

    @Override
    public CartItem applyChangesToExistingEntity(CartItemPojo source, CartItem target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
