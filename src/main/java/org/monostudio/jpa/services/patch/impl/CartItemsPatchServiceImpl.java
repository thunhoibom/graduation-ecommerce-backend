package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.CartItemPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.CartItem;
import org.monostudio.jpa.services.patch.CartItemsPatchService;

import java.util.Map;

@Service
@NoArgsConstructor
public class CartItemsPatchServiceImpl
    implements CartItemsPatchService {

    @Override
    public CartItem patchExistingEntity(Map<String, Object> changes, CartItem existing)
        throws BadInputException {
        CartItem target = new CartItem(existing);

        if (changes.containsKey("quantity")) {
            Integer quantity = (Integer) changes.get("quantity");
            if (quantity != null && quantity > 0) {
                target.setQuantity(quantity);
            }
        }

        return target;
    }

    @Override
    public CartItem patchExistingEntity(CartItemPojo changes, CartItem existing)
        throws BadInputException {
        throw new UnsupportedOperationException("This method signature has been deprecated");
    }
}
