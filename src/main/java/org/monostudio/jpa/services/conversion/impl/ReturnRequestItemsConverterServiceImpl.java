package org.monostudio.jpa.services.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.api.models.ReturnRequestItemPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ReturnRequestItem;
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

    @Autowired
    public ReturnRequestItemsConverterServiceImpl(
        ProductsConverterService productConverterService,
        ProductsRepository productsRepository
    ) {
        this.productConverterService = productConverterService;
        this.productsRepository = productsRepository;
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
            .quantity(source.getQuantity())
            .reason(source.getReason())
            .isActive(source.isActive());

        if (source.getProduct() != null) {
            ProductPojo product = productConverterService.convertToPojo(source.getProduct());
            builder.product(product);
        }

        return builder.build();
    }
}
