package org.monostudio.jpa.services.conversion.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.CartItemPojo;
import org.monostudio.api.models.CartSessionPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.CartItem;
import org.monostudio.jpa.entities.CartSession;
import org.monostudio.jpa.services.conversion.CartSessionsConverterService;

import java.util.ArrayList;
import java.util.List;

@Transactional
@Service
public class CartSessionsConverterServiceImpl
    implements CartSessionsConverterService {

    @Override
    public CartSessionPojo convertToPojo(CartSession source) {
        List<CartItemPojo> itemPojos = new ArrayList<>();
        if (source.getItems() != null) {
            for (CartItem item : source.getItems()) {
                itemPojos.add(toItemPojo(item));
            }
        }

        int subtotal = itemPojos.stream()
            .mapToInt(pojo -> pojo.getLineTotal() != null ? pojo.getLineTotal() : 0)
            .sum();

        int totalUnits = itemPojos.stream()
            .mapToInt(pojo -> pojo.getQuantity())
            .sum();

        return CartSessionPojo.builder()
            .id(source.getId())
            .token(source.getToken())
            .items(itemPojos)
            .itemCount(itemPojos.size())
            .totalUnits(totalUnits)
            .subtotal(subtotal)
            .createdAt(source.getCreatedAt())
            .updatedAt(source.getUpdatedAt())
            .expiresAt(source.getExpiresAt())
            .expired(source.isExpired())
            .build();
    }

    @Override
    public CartSession convertToNewEntity(CartSessionPojo source) throws BadInputException {
        CartSession target = CartSession.builder()
            .token(source.getToken())
            .build();
        return target;
    }

    @Override
    public CartSession applyChangesToExistingEntity(CartSessionPojo source, CartSession target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }

    private CartItemPojo toItemPojo(CartItem item) {
        var variant = item.getVariant();
        return CartItemPojo.builder()
            .id(item.getId())
            .variantSkuResolved(variant != null ? variant.getSku() : null)
            .variantSize(variant != null ? variant.getSize() : null)
            .variantColor(variant != null ? variant.getColor() : null)
            .productId(variant != null && variant.getProduct() != null
                ? variant.getProduct().getId() : null)
            .productName(variant != null && variant.getProduct() != null
                ? variant.getProduct().getName() : null)
            .productBarcode(variant != null && variant.getProduct() != null
                ? variant.getProduct().getBarcode() : null)
            .quantity(item.getQuantity())
            .addedAt(item.getAddedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }
}
