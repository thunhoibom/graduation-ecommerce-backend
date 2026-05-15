package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.CartItemPojo;
import org.monostudio.jpa.entities.CartItem;
import org.monostudio.jpa.services.PatchService;

public interface CartItemsPatchService
    extends PatchService<CartItemPojo, CartItem> {
}
