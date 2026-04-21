package org.monostudio.jpa.services.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ProductCategoryPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.repositories.ProductsCategoriesRepository;
import org.monostudio.jpa.services.conversion.ImagesConverterService;
import org.monostudio.jpa.services.conversion.ProductCategoriesConverterService;

@Service
public class ProductCategoriesConverterServiceImpl
    implements ProductCategoriesConverterService {

    private final ProductsCategoriesRepository categoriesRepository;
    private final ImagesConverterService imagesConverterService;

    @Autowired
    public ProductCategoriesConverterServiceImpl(
        ProductsCategoriesRepository categoriesRepository,
        ImagesConverterService imagesConverterService
    ) {
        this.categoriesRepository = categoriesRepository;
        this.imagesConverterService = imagesConverterService;
    }

    @Override
    public ProductCategoryPojo convertToPojo(ProductCategory source) {
        if (source == null) return null;

        ProductCategoryPojo pojo = ProductCategoryPojo.builder()
            .id(source.getId())
            .code(source.getCode())
            .name(source.getName())
            .imageUrl(source.getImage() != null ? source.getImage().getUrl() : null)
            .image(source.getImage() != null ? imagesConverterService.convertToPojo(source.getImage()) : null)
            .build();

        if (source.getParent() != null) {
            ProductCategory p = source.getParent();
            pojo.setParent(ProductCategoryPojo.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .imageUrl(p.getImage() != null ? p.getImage().getUrl() : null)
                .image(p.getImage() != null ? imagesConverterService.convertToPojo(p.getImage()) : null)
                .build());
        }

        return pojo;
    }

    @Override
    public ProductCategory convertToNewEntity(ProductCategoryPojo source) throws BadInputException {
        ProductCategory target = ProductCategory.builder()
            .code(source.getCode())
            .name(source.getName())
            .image(source.getImage() != null ? imagesConverterService.convertToNewEntity(source.getImage()) : null)
            .build();

        if (source.getParent() != null) {
            String parentCode = source.getParent().getCode();
            if (StringUtils.isNotBlank(parentCode)) {
                categoriesRepository.findByCode(parentCode).ifPresent(target::setParent);
            }
        }
        return target;
    }

    @Override
    public ProductCategory applyChangesToExistingEntity(ProductCategoryPojo source, ProductCategory target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
