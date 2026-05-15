package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.CartItem;
import org.monostudio.jpa.entities.QCartItem;
import org.monostudio.jpa.services.PredicateService;

public interface CartItemsPredicateService
    extends PredicateService<CartItem> {
    QCartItem basePath = QCartItem.cartItem;
}
