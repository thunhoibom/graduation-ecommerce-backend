package org.monostudio.jpa.services.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.jpa.entities.ProductListItem;
import org.monostudio.jpa.services.conversion.ProductListItemsConverterService;
import org.monostudio.jpa.services.conversion.ProductsConverterService;

@Transactional
@Service
public class ProductListItemsConverterServiceImpl
    implements ProductListItemsConverterService {
    private final ProductsConverterService productsConverterService;

    @Autowired
    public ProductListItemsConverterServiceImpl(
        ProductsConverterService productsConverterService
    ) {
        this.productsConverterService = productsConverterService;
    }

    @Override
    public ProductPojo convertToPojo(ProductListItem source) {
        return productsConverterService.convertToPojo(source.getProduct());
    }

    @Override
    public ProductListItem convertToNewEntity(ProductPojo source) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ProductListItem applyChangesToExistingEntity(ProductPojo source, ProductListItem target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
