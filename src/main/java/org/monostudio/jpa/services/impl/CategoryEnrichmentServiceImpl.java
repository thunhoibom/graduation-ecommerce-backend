package org.monostudio.jpa.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ProductCategoryPojo;
import org.monostudio.jpa.repositories.ProductsCategoriesRepository;
import org.monostudio.jpa.services.CategoryEnrichmentService;

@Service
public class CategoryEnrichmentServiceImpl implements CategoryEnrichmentService {

    private final ProductsCategoriesRepository categoriesRepository;

    @Autowired
    public CategoryEnrichmentServiceImpl(ProductsCategoriesRepository categoriesRepository) {
        this.categoriesRepository = categoriesRepository;
    }

    @Override
    public void enrichWithProductCount(ProductCategoryPojo pojo) {
        if (pojo.getId() != null) {
            pojo.setProductCount(
                (int) categoriesRepository.countProductsByCategoryId(pojo.getId()));
        }
    }
}
