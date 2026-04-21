package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.CartSessionPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.CartSession;
import org.monostudio.jpa.services.patch.CartSessionsPatchService;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@NoArgsConstructor
public class CartSessionsPatchServiceImpl
    implements CartSessionsPatchService {

    @Override
    public CartSession patchExistingEntity(Map<String, Object> changes, CartSession existing)
        throws BadInputException {
        CartSession target = new CartSession(existing);
        target.setCustomer(existing.getCustomer());
        target.setItems(existing.getItems());

        if (changes.containsKey("expiresAt")) {
            Object val = changes.get("expiresAt");
            if (val instanceof LocalDateTime) {
                target.setExpiresAt((LocalDateTime) val);
            } else if (val instanceof String) {
                target.setExpiresAt(LocalDateTime.parse((String) val));
            }
        }

        // Refresh expiry to extend TTL
        if (changes.containsKey("refreshExpiry")) {
            Boolean refresh = (Boolean) changes.get("refreshExpiry");
            if (Boolean.TRUE.equals(refresh)) {
                target.refreshExpiry();
            }
        }

        return target;
    }

    @Override
    public CartSession patchExistingEntity(CartSessionPojo changes, CartSession existing)
        throws BadInputException {
        throw new UnsupportedOperationException("This method signature has been deprecated");
    }
}
