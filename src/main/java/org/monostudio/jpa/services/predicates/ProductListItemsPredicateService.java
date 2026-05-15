package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.ProductListItem;
import org.monostudio.jpa.entities.QProductListItem;
import org.monostudio.jpa.services.PredicateService;

public interface ProductListItemsPredicateService
    extends PredicateService<ProductListItem> {
    QProductListItem basePath = QProductListItem.productListItem;
}
