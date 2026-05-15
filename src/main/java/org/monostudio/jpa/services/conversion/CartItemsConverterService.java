package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.CartItemPojo;
import org.monostudio.jpa.entities.CartItem;
import org.monostudio.jpa.services.ConverterService;

public interface CartItemsConverterService
    extends ConverterService<CartItemPojo, CartItem> {
}
