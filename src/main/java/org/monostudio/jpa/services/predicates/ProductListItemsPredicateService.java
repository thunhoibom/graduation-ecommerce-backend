package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QProductListItem;
import org.monostudio.jpa.services.PredicateService;

public interface ProductListItemsPredicateService
    extends PredicateService {
    QProductListItem basePath = QProductListItem.productListItem;
}
