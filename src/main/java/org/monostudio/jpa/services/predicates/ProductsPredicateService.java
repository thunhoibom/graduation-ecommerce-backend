package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.QProduct;
import org.monostudio.jpa.services.PredicateService;

public interface ProductsPredicateService
    extends PredicateService<Product> {
    QProduct basePath = QProduct.product;
}
