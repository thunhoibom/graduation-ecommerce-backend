package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.ShippingMethodPojo;
import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.jpa.services.ConverterService;

public interface ShippingMethodsConverterService
    extends ConverterService<ShippingMethodPojo, ShippingMethod> {
}
