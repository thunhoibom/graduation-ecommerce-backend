package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.ShippingMethodPojo;
import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.jpa.services.PatchService;

public interface ShippingMethodsPatchService
    extends PatchService<ShippingMethodPojo, ShippingMethod> {
}
