package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.ProductCategoryPojo;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.services.ConverterService;

public interface ProductCategoriesConverterService
    extends ConverterService<ProductCategoryPojo, ProductCategory> {
}
