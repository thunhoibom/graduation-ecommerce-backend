package org.monostudio.jpa.services.conversion.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.CartItemPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.CartItem;
import org.monostudio.jpa.services.conversion.CartItemsConverterService;

@Transactional
@Service
public class CartItemsConverterServiceImpl
    implements CartItemsConverterService {

    @Override
    public CartItemPojo convertToPojo(CartItem source) {
        var variant = source.getVariant();
        return CartItemPojo.builder()
            .id(source.getId())
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
