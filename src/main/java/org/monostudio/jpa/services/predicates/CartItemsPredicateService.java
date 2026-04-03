package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QCartItem;
import org.monostudio.jpa.services.PredicateService;

public interface CartItemsPredicateService
    extends PredicateService {
    QCartItem basePath = QCartItem.cartItem;
}
