package org.monostudio.api.services.impl;

import org.springframework.stereotype.Service;
import org.monostudio.api.models.ShippingRatePojo;
import org.monostudio.api.services.ShippingMethodsService;
import org.monostudio.jpa.entities.ShippingMethod;

@Service
public class ShippingMethodsServiceImpl
    implements ShippingMethodsService {

    @Override
    public ShippingRatePojo computeRate(ShippingMethod method, Integer subtotal) {
        boolean freeShipping = false;
        int fee = method.getBaseFee();

        if (subtotal != null && method.getFreeShippingThreshold() != null) {
            if (subtotal >= method.getFreeShippingThreshold()) {
                fee = 0;
                freeShipping = true;
            }
        }

        return ShippingRatePojo.builder()
            .id(method.getId())
            .name(method.getName())
            .fee(fee)
            .estimatedDaysMin(method.getEstimatedDaysMin())
            .estimatedDaysMax(method.getEstimatedDaysMax())
            .freeShipping(freeShipping)
            .build();
    }
}
