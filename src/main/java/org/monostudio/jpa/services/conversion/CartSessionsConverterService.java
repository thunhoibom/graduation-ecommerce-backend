package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.CartSessionPojo;
import org.monostudio.jpa.entities.CartSession;
import org.monostudio.jpa.services.ConverterService;

public interface CartSessionsConverterService
    extends ConverterService<CartSessionPojo, CartSession> {
}
