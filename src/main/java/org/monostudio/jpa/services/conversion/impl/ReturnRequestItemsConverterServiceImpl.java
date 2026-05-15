package org.monostudio.jpa.services.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ReturnRequestItemPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ReturnRequestItem;
import org.monostudio.jpa.repositories.ProductVariantsRepository;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.services.conversion.ProductsConverterService;
import org.monostudio.jpa.services.conversion.ReturnRequestItemsConverterService;

import jakarta.validation.constraints.NotNull;

@Transactional
@Service
public class ReturnRequestItemsConverterServiceImpl
    implements ReturnRequestItemsConverterService {
    private final ProductsConverterService productConverterService;
    private final ProductsRepository productsRepository;
    private final ProductVariantsRepository productVariantsRepository;

    @Autowired
    public ReturnRequestItemsConverterServiceImpl(
        ProductsConverterService productConverterService,
        ProductsRepository productsRepository,
        ProductVariantsRepository productVariantsRepository
    ) {
        this.productConverterService = productConverterService;
        this.productsRepository = productsRepository;
        this.productVariantsRepository = productVariantsRepository;
    }

    @Override
    public ReturnRequestItem convertToNewEntity(ReturnRequestItemPojo source) throws BadInputException {
        ReturnRequestItem.ReturnRequestItemBuilder builder = ReturnRequestItem.builder()
            .quantity(source.getQuantity())
            .reason(source.getReason())
            .isActive(true);

        if (source.getProductId() != null) {
            Product product = productsRepository.findById(source.getProductId())
                .orElseThrow(() -> new BadInputException("Product not found with id: " + source.getProductId()));
            builder.product(product);
        }

        return builder.build();
    }

    @Override
    public ReturnRequestItem applyChangesToExistingEntity(ReturnRequestItemPojo source, ReturnRequestItem target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }

    @Override
    public ReturnRequestItemPojo convertToPojo(@NotNull ReturnRequestItem source) {
        ReturnRequestItemPojo.ReturnRequestItemPojoBuilder builder = ReturnRequestItemPojo.builder()
            .id(source.getId())
            .quantity(source.getQuantity())
            .reason(source.getReason())
            .isActive(source.isActive());

        Long variantId = source.getVariant() != null ? source.getVariant().getId() : null;
        if (variantId != null) {
            builder.variantId(variantId);
        }

        Long productId = source.getProduct() != null ? source.getProduct().getId() : null;
        if (productId == null && variantId != null) {
            productId = productVariantsRepository.findProductIdByVariantId(variantId).orElse(null);
        }
        if (productId != null) {
            builder.productId(productId);
            productsRepository.findById(productId)
                .map(productConverterService::convertToPojo)
                .ifPresent(builder::product);
        }

        return builder.build();
    }
}
