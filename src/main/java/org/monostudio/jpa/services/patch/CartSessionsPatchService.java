package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.CartSessionPojo;
import org.monostudio.jpa.entities.CartSession;
import org.monostudio.jpa.services.PatchService;

public interface CartSessionsPatchService
    extends PatchService<CartSessionPojo, CartSession> {
}
