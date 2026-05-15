package org.monostudio.api.services;

import org.monostudio.api.models.ShippingRatePojo;
import org.monostudio.api.models.ShippingRateRequestContext;
import org.monostudio.jpa.entities.ShippingMethod;

/**
 * Domain service for shipping method operations.
 */
public interface ShippingMethodsService {

    /**
     * Computes the shipping rate for a given method and subtotal.
     * Fee is 0 if subtotal >= freeShippingThreshold.
     *
     * @param method   The shipping method
     * @param subtotal The cart subtotal in VND (may be null)
     * @return A ShippingRatePojo with the computed fee
     */
    ShippingRatePojo computeRate(ShippingMethod method, ShippingRateRequestContext context);
}
