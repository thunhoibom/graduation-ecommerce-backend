package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QProductList;
import org.monostudio.jpa.services.PredicateService;

public interface ProductListsPredicateService
    extends PredicateService {
    QProductList basePath = QProductList.productList;
}
