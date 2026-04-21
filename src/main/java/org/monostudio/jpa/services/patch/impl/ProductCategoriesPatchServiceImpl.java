package org.monostudio.jpa.services.patch.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.monostudio.api.models.ImagePojo;
import org.monostudio.api.models.ProductCategoryPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.repositories.ProductsCategoriesRepository;
import org.monostudio.jpa.services.conversion.ImagesConverterService;
import org.monostudio.jpa.services.patch.ProductCategoriesPatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Service
public class ProductCategoriesPatchServiceImpl
    implements ProductCategoriesPatchService {

    private final ObjectMapper objectMapper;
    private final ImagesConverterService imagesConverterService;
    private final ProductsCategoriesRepository categoriesRepository;

    @Autowired
    public ProductCategoriesPatchServiceImpl(
        ObjectMapper objectMapper,
        ImagesConverterService imagesConverterService,
        ProductsCategoriesRepository categoriesRepository
    ) {
        this.objectMapper = objectMapper;
        this.imagesConverterService = imagesConverterService;
        this.categoriesRepository = categoriesRepository;
    }

    @Override
    public ProductCategory patchExistingEntity(Map<String, Object> changes, ProductCategory existing) throws BadInputException {
        ProductCategory target = new ProductCategory(existing);
        // By default, copy existing relations that the copy constructor might miss or nullify
        target.setParent(existing.getParent());

        if (changes.containsKey("code")) {
            String code = (String) changes.get("code");
            if (!StringUtils.isBlank(code)) {
                target.setCode(code);
            }
        }

        if (changes.containsKey("name")) {
            String name = (String) changes.get("name");
            if (!StringUtils.isBlank(name)) {
                target.setName(name);
            }
        }

        if (changes.containsKey("image")) {
            Object imgValue = changes.get("image");
            if (imgValue == null) {
                target.setImage(null);
            } else {
                ImagePojo imgPojo = objectMapper.convertValue(imgValue, ImagePojo.class);
                target.setImage(imagesConverterService.convertToNewEntity(imgPojo));
            }
        }

        if (changes.containsKey("parent")) {
            Object parentValue = changes.get("parent");
            if (parentValue == null) {
                target.setParent(null);
            } else {
                ProductCategoryPojo parentPojo = objectMapper.convertValue(parentValue, ProductCategoryPojo.class);
                if (parentPojo.getCode() != null) {
                    categoriesRepository.findByCode(parentPojo.getCode()).ifPresent(target::setParent);
                }
            }
        }

        return target;
    }

    @Override
    public ProductCategory patchExistingEntity(ProductCategoryPojo changes, ProductCategory existing) throws BadInputException {
        throw new UnsupportedOperationException("This method signature has been deprecated");
    }
}
