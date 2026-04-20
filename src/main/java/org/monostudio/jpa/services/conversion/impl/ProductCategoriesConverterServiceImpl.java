package org.monostudio.jpa.services.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ProductCategoryPojo;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.repositories.ProductsCategoriesRepository;
import org.monostudio.jpa.services.conversion.ProductCategoriesConverterService;

@Service
public class ProductCategoriesConverterServiceImpl
    implements ProductCategoriesConverterService {

    private final ProductsCategoriesRepository categoriesRepository;

    @Autowired
    public ProductCategoriesConverterServiceImpl(
        ProductsCategoriesRepository categoriesRepository
    ) {
        this.categoriesRepository = categoriesRepository;
    }

    @Override
    public ProductCategoryPojo convertToPojo(ProductCategory source) {
        ProductCategoryPojo target = ProductCategoryPojo.builder()
            .id(source.getId())
            .code(source.getCode())
            .name(source.getName())
            .build();
        if (source.getParent() != null) {
            ProductCategoryPojo parent = ProductCategoryPojo.builder()
                .id(source.getParent().getId())
                .code(source.getParent().getCode())
                .name(source.getParent().getName())
                .build();
            target.setParent(parent);
        }
        return target;
    }

    @Override
    public ProductCategory convertToNewEntity(ProductCategoryPojo source) {
        ProductCategory target = ProductCategory.builder()
            .code(source.getCode())
            .name(source.getName())
            .build();

        if (source.getParent()!=null) {
            String parentCode = source.getParent().getCode();
            categoriesRepository.findByCode(parentCode).ifPresent(target::setParent);
        }
        return target;
    }

    @Override
    public ProductCategory applyChangesToExistingEntity(ProductCategoryPojo source, ProductCategory target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
