package org.monostudio.jpa.services.conversion.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ShippingMethodPojo;
import org.monostudio.common.exceptions.BadInputException;
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
    public ShippingMethod convertToNewEntity(ShippingMethodPojo source) throws BadInputException {
        String name = StringUtils.trimToNull(source.getName());
        if (name == null) {
            name = "Shipping-" + System.currentTimeMillis();
        }
        int baseFee = Math.max(0, source.getBaseFee() != null ? source.getBaseFee() : 0);
        int daysMin = Math.max(0, source.getEstimatedDaysMin() != null ? source.getEstimatedDaysMin() : 0);
        int daysMax = Math.max(0, source.getEstimatedDaysMax() != null ? source.getEstimatedDaysMax() : 0);
        if (daysMax < daysMin) {
            daysMax = daysMin;
        }
        return ShippingMethod.builder()
            .name(name)
            .baseFee(baseFee)
            .freeShippingThreshold(source.getFreeShippingThreshold())
            .estimatedDaysMin(daysMin)
            .estimatedDaysMax(daysMax)
            .active(source.getActive() != null ? source.getActive() : true)
            .pricePerKm(source.getPricePerKm())
            .carrierCode(normalizeOrDefault(source.getCarrierCode(), "LOCAL"))
            .rateMode(normalizeOrDefault(source.getRateMode(), "DISTANCE"))
            .carrierServiceCode(source.getCarrierServiceCode())
            .carrierShopId(source.getCarrierShopId())
            .build();
    }

    @Override
    public ShippingMethod mergePojoOntoExisting(ShippingMethodPojo source, ShippingMethod existing) {
        String mergedName = StringUtils.isNotBlank(source.getName())
            ? StringUtils.trim(source.getName())
            : existing.getName();
        int mergedBaseFee = Math.max(0, source.getBaseFee() != null ? source.getBaseFee() : existing.getBaseFee());
        int mergedDaysMin = Math.max(0, source.getEstimatedDaysMin() != null ? source.getEstimatedDaysMin() : existing.getEstimatedDaysMin());
        int mergedDaysMax = Math.max(0, source.getEstimatedDaysMax() != null ? source.getEstimatedDaysMax() : existing.getEstimatedDaysMax());
        if (mergedDaysMax < mergedDaysMin) {
            mergedDaysMax = mergedDaysMin;
        }
        boolean mergedActive = source.getActive() != null ? source.getActive() : existing.isActive();

        Integer mergedThreshold = source.getFreeShippingThreshold() != null
            ? source.getFreeShippingThreshold()
            : existing.getFreeShippingThreshold();
        Integer mergedPricePerKm = source.getPricePerKm() != null
            ? source.getPricePerKm()
            : existing.getPricePerKm();

        String mergedCarrier = StringUtils.isBlank(source.getCarrierCode())
            ? normalizeOrDefault(existing.getCarrierCode(), "LOCAL")
            : normalizeOrDefault(source.getCarrierCode(), "LOCAL");
        String mergedRateMode = StringUtils.isBlank(source.getRateMode())
            ? normalizeOrDefault(existing.getRateMode(), "DISTANCE")
            : normalizeOrDefault(source.getRateMode(), "DISTANCE");

        String mergedServiceCode = source.getCarrierServiceCode() != null
            ? source.getCarrierServiceCode()
            : existing.getCarrierServiceCode();
        Long mergedShopId = source.getCarrierShopId() != null
            ? source.getCarrierShopId()
            : existing.getCarrierShopId();

        return ShippingMethod.builder()
            .id(existing.getId())
            .name(mergedName)
            .baseFee(mergedBaseFee)
            .freeShippingThreshold(mergedThreshold)
            .estimatedDaysMin(mergedDaysMin)
            .estimatedDaysMax(mergedDaysMax)
            .active(mergedActive)
            .pricePerKm(mergedPricePerKm)
            .carrierCode(mergedCarrier)
            .rateMode(mergedRateMode)
            .carrierServiceCode(mergedServiceCode)
            .carrierShopId(mergedShopId)
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
