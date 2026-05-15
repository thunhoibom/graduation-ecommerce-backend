package org.monostudio.jpa.services.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ProductListPojo;
import org.monostudio.jpa.entities.ProductList;
import org.monostudio.jpa.entities.QProductListItem;
import org.monostudio.jpa.repositories.ProductListItemsRepository;
import org.monostudio.jpa.services.conversion.ProductListsConverterService;

@Service
public class ProductListConverterServiceImpl
    implements ProductListsConverterService {
    private final ProductListItemsRepository productListItemRepository;

    @Autowired
    public ProductListConverterServiceImpl(
        ProductListItemsRepository productListItemRepository
    ) {
        this.productListItemRepository = productListItemRepository;
    }

    @Override
    public ProductListPojo convertToPojo(ProductList source) {
        Long sourceListId = source.getId();
        long itemCount = productListItemRepository.count(QProductListItem.productListItem.list.id.eq(sourceListId));
        return ProductListPojo.builder()
            .name(source.getName())
            .code(source.getCode())
            .totalCount(itemCount)
            .build();
    }

    @Override
    public ProductList convertToNewEntity(ProductListPojo source) {
        return ProductList.builder()
            .name(source.getName())
            .code(source.getCode())
            .build();
    }

    @Override
    public ProductList applyChangesToExistingEntity(ProductListPojo source, ProductList target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
