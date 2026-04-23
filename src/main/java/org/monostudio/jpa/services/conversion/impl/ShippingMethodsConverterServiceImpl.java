package org.monostudio.jpa.services.conversion.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ShippingMethodPojo;
import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.jpa.services.conversion.ShippingMethodsConverterService;

@Service
@NoArgsConstructor
public class ShippingMethodsConverterServiceImpl
    implements ShippingMethodsConverterService {

    @Override
    public ShippingMethodPojo convertToPojo(ShippingMethod source) {
        return ShippingMethodPojo.builder()
            .id(source.getId())
            .name(source.getName())
            .baseFee(source.getBaseFee())
            .freeShippingThreshold(source.getFreeShippingThreshold())
            .estimatedDaysMin(source.getEstimatedDaysMin())
            .estimatedDaysMax(source.getEstimatedDaysMax())
            .active(source.isActive())
            .pricePerKm(source.getPricePerKm())
            .carrierCode(source.getCarrierCode())
            .rateMode(source.getRateMode())
            .carrierServiceCode(source.getCarrierServiceCode())
            .carrierShopId(source.getCarrierShopId())
            .build();
    }

    @Override
    public ShippingMethod convertToNewEntity(ShippingMethodPojo source) {
        return ShippingMethod.builder()
            .name(source.getName())
            .baseFee(source.getBaseFee())
            .freeShippingThreshold(source.getFreeShippingThreshold())
            .estimatedDaysMin(source.getEstimatedDaysMin())
            .estimatedDaysMax(source.getEstimatedDaysMax())
            .active(source.getActive() != null ? source.getActive() : true)
            .pricePerKm(source.getPricePerKm())
            .carrierCode(normalizeOrDefault(source.getCarrierCode(), "LOCAL"))
            .rateMode(normalizeOrDefault(source.getRateMode(), "DISTANCE"))
            .carrierServiceCode(source.getCarrierServiceCode())
            .carrierShopId(source.getCarrierShopId())
            .build();
    }

    @Override
    public ShippingMethod applyChangesToExistingEntity(ShippingMethodPojo source, ShippingMethod target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }

    private String normalizeOrDefault(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim().toUpperCase();
    }
}
