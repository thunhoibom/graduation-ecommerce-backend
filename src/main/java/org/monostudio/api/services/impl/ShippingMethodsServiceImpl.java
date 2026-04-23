package org.monostudio.api.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ShippingRatePojo;
import org.monostudio.api.models.ShippingRateRequestContext;
import org.monostudio.api.services.ShippingMethodsService;
import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.shipping.ShippingCarrierCodes;
import org.monostudio.shipping.ghn.GhnApiClient;
import org.monostudio.shipping.ghn.GhnConfig;
import org.monostudio.shipping.ghn.dto.GhnRateRequest;
import org.monostudio.shipping.ghn.dto.GhnRateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ShippingMethodsServiceImpl
    implements ShippingMethodsService {
    private static final Logger logger = LoggerFactory.getLogger(ShippingMethodsServiceImpl.class);
    @Value("${monostudio.store.location.latitude:10.762622}")
    private Double storeLat;

    @Value("${monostudio.store.location.longitude:106.660172}")
    private Double storeLng;

    private final GhnApiClient ghnApiClient;
    private final GhnConfig ghnConfig;

    public ShippingMethodsServiceImpl(GhnApiClient ghnApiClient, GhnConfig ghnConfig) {
        this.ghnApiClient = ghnApiClient;
        this.ghnConfig = ghnConfig;
    }

    @Override
    public ShippingRatePojo computeRate(ShippingMethod method, ShippingRateRequestContext context) {
        Integer subtotal = context != null ? context.getSubtotal() : null;
        boolean freeShipping = false;
        int fee;
        String providerFeeSource = "LOCAL";
        boolean estimated = false;

        String carrierCode = StringUtils.defaultIfBlank(method.getCarrierCode(), ShippingCarrierCodes.LOCAL)
            .trim()
            .toUpperCase();
        if (ShippingCarrierCodes.GHN.equals(carrierCode)) {
            fee = computeGhnFee(method, context);
            providerFeeSource = "GHN";
        } else {
            fee = computeLocalFee(method, context);
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
            .carrierCode(carrierCode)
            .providerFeeSource(providerFeeSource)
            .estimated(estimated)
            .build();
    }

    private int computeLocalFee(ShippingMethod method, ShippingRateRequestContext context) {
        int fee = method.getBaseFee();
        Double dstLat = context != null ? context.getLatitude() : null;
        Double dstLng = context != null ? context.getLongitude() : null;
        if (method.getPricePerKm() != null && method.getPricePerKm() > 0 && dstLat != null && dstLng != null) {
            double distanceKm = calculateHaversineDistance(storeLat, storeLng, dstLat, dstLng);
            fee += (int) (Math.round(distanceKm) * method.getPricePerKm());
        }
        return Math.max(0, fee);
    }

    private int computeGhnFee(ShippingMethod method, ShippingRateRequestContext context) {
        if (context == null || context.getToDistrictId() == null || StringUtils.isBlank(context.getToWardCode())) {
            throw new IllegalArgumentException("Missing GHN destination district/ward");
        }
        Integer configuredServiceId = parseNullableInt(method.getCarrierServiceCode());
        long shopId = method.getCarrierShopId() != null && method.getCarrierShopId() > 0
            ? method.getCarrierShopId()
            : ghnConfig.getDefaultShopId();
        if (shopId <= 0) {
            throw new IllegalArgumentException("GHN shop id is not configured");
        }
        int resolvedServiceId = ghnApiClient.resolveServiceId(
            shopId,
            ghnConfig.getFromDistrictId(),
            context.getToDistrictId(),
            configuredServiceId,
            ghnConfig.getDefaultServiceTypeId()
        );
        logger.info("GHN fee context: shippingMethodId={}, shopId={}, fromDistrict={}, fromWard={}, toDistrict={}, toWard={}, configuredServiceId={}, resolvedServiceId={}",
            method.getId(), shopId, ghnConfig.getFromDistrictId(), ghnConfig.getFromWardCode(),
            context.getToDistrictId(), context.getToWardCode(), configuredServiceId, resolvedServiceId);
        GhnRateRequest request = GhnRateRequest.builder()
            .shopId(shopId)
            .serviceId(resolvedServiceId)
            .serviceTypeId(ghnConfig.getDefaultServiceTypeId())
            .insuranceValue(context.getSubtotal() != null ? Math.max(context.getSubtotal(), 0) : 0)
            .fromDistrictId(ghnConfig.getFromDistrictId())
            .fromWardCode(ghnConfig.getFromWardCode())
            .toDistrictId(context.getToDistrictId())
            .toWardCode(context.getToWardCode())
            .weight(1000)
            .length(20)
            .width(20)
            .height(10)
            .build();
        GhnRateResult result = ghnApiClient.quoteFee(request);
        return Math.max(0, result.getTotalFee());
    }

    private Integer parseNullableInt(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
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
