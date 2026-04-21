package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.entities.QProductCategory;
import org.monostudio.jpa.services.PredicateService;

public interface ProductCategoriesPredicateService
    extends PredicateService<ProductCategory> {
    QProductCategory basePath = QProductCategory.productCategory;
}
