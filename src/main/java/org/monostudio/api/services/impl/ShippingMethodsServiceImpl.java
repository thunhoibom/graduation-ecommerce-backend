package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ShippingRatePojo;
import org.monostudio.api.services.ShippingMethodsService;
import org.monostudio.jpa.entities.ShippingMethod;

@Service
public class ShippingMethodsServiceImpl
    implements ShippingMethodsService {

    @Value("${monostudio.store.location.latitude:10.762622}")
    private Double storeLat;

    @Value("${monostudio.store.location.longitude:106.660172}")
    private Double storeLng;

    @Override
    public ShippingRatePojo computeRate(ShippingMethod method, Integer subtotal, Double dstLat, Double dstLng) {
        boolean freeShipping = false;
        int fee = method.getBaseFee();

        // Calculate distance fee if applicable
        if (method.getPricePerKm() != null && method.getPricePerKm() > 0 && dstLat != null && dstLng != null) {
            double distanceKm = calculateHaversineDistance(storeLat, storeLng, dstLat, dstLng);
            fee += (int) (Math.round(distanceKm) * method.getPricePerKm());
        }

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

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; 
    }
}
