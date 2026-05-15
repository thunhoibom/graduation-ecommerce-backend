package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.ShippingMethodPojo;
import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.jpa.services.ConverterService;

public interface ShippingMethodsConverterService
    extends ConverterService<ShippingMethodPojo, ShippingMethod> {

    /**
     * Builds a full entity for PUT-style updates when the JSON body may omit fields (null = leave existing).
     */
    ShippingMethod mergePojoOntoExisting(ShippingMethodPojo source, ShippingMethod existing);
}
