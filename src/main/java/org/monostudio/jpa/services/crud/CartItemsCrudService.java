package org.monostudio.jpa.services.crud;

import org.monostudio.api.models.CartItemPojo;
import org.monostudio.jpa.entities.CartItem;
import org.monostudio.jpa.services.CrudService;

public interface CartItemsCrudService
    extends CrudService<CartItemPojo, CartItem> {
}
