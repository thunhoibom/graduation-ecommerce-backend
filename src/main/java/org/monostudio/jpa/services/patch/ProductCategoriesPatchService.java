package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.ProductCategoryPojo;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.services.PatchService;

public interface ProductCategoriesPatchService
    extends PatchService<ProductCategoryPojo, ProductCategory> {
}
