package org.monostudio.jpa.services.crud.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ProductCategoryPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.repositories.ProductsCategoriesRepository;
import org.monostudio.jpa.services.conversion.ProductCategoriesConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.ProductCategoriesCrudService;
import org.monostudio.jpa.services.patch.ProductCategoriesPatchService;

import java.util.Map;
import java.util.Optional;

@Transactional
@Service
public class ProductCategoriesCrudServiceImpl
    extends CrudGenericService<ProductCategoryPojo, ProductCategory>
    implements ProductCategoriesCrudService {
    private final ProductsCategoriesRepository categoriesRepository;
    private final ProductCategoriesPatchService categoriesPatchService;

    @Autowired
    public ProductCategoriesCrudServiceImpl(
        ProductsCategoriesRepository categoriesRepository,
        ProductCategoriesConverterService categoriesConverterService,
        ProductCategoriesPatchService categoriesPatchService
    ) {
        super(categoriesRepository, categoriesConverterService, categoriesPatchService);
        this.categoriesRepository = categoriesRepository;
        this.categoriesPatchService = categoriesPatchService;
    }

    @Override
    public Optional<ProductCategory> getExisting(ProductCategoryPojo input) throws BadInputException {
        String code = input.getCode();
        if (StringUtils.isBlank(code)) {
            throw new BadInputException("Invalid category code");
        } else {
            return this.categoriesRepository.findByCode(code);
        }
    }

    @Override
    protected final ProductCategory flushPartialChanges(Map<String, Object> changes, ProductCategory existingEntity) throws BadInputException {
        ProductCategory preparedEntity = categoriesPatchService.patchExistingEntity(changes, existingEntity);
        if (existingEntity.getParent()!=null) {
            preparedEntity.setParent(existingEntity.getParent());
        }
        if (existingEntity.equals(preparedEntity)) {
            return existingEntity;
        }
        return categoriesRepository.saveAndFlush(preparedEntity);
    }
}
