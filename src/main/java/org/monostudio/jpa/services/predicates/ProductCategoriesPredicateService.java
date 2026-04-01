package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QProductCategory;
import org.monostudio.jpa.services.PredicateService;

public interface ProductCategoriesPredicateService
    extends PredicateService {
    QProductCategory basePath = QProductCategory.productCategory;
}
