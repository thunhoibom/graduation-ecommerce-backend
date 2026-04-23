package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ShippingMethodPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.jpa.services.patch.ShippingMethodsPatchService;

import java.util.Map;

@Service
@NoArgsConstructor
public class ShippingMethodsPatchServiceImpl
    implements ShippingMethodsPatchService {

    @Override
    public ShippingMethod patchExistingEntity(Map<String, Object> changes, ShippingMethod existing) throws BadInputException {
        ShippingMethod target = new ShippingMethod(existing);

        if (changes.containsKey("name")) {
            String name = (String) changes.get("name");
            if (!StringUtils.isBlank(name)) {
                target.setName(name);
            }
        }
        if (changes.containsKey("baseFee")) {
            Object baseFee = changes.get("baseFee");
            if (baseFee instanceof Number) {
                target.setBaseFee(((Number) baseFee).intValue());
            }
        }
        if (changes.containsKey("freeShippingThreshold")) {
            Object threshold = changes.get("freeShippingThreshold");
            if (threshold == null) {
                target.setFreeShippingThreshold(null);
            } else if (threshold instanceof Number) {
                target.setFreeShippingThreshold(((Number) threshold).intValue());
            }
        }
        if (changes.containsKey("estimatedDaysMin")) {
            Object val = changes.get("estimatedDaysMin");
            if (val instanceof Number) {
                target.setEstimatedDaysMin(((Number) val).intValue());
            }
        }
        if (changes.containsKey("estimatedDaysMax")) {
            Object val = changes.get("estimatedDaysMax");
            if (val instanceof Number) {
                target.setEstimatedDaysMax(((Number) val).intValue());
            }
        }
        if (changes.containsKey("active")) {
            Object active = changes.get("active");
            if (active instanceof Boolean) {
                target.setActive((Boolean) active);
            } else if (active instanceof String) {
                target.setActive(Boolean.parseBoolean((String) active));
            }
        }
        if (changes.containsKey("pricePerKm")) {
            Object pricePerKm = changes.get("pricePerKm");
            if (pricePerKm == null) {
                target.setPricePerKm(null);
            } else if (pricePerKm instanceof Number) {
                target.setPricePerKm(((Number) pricePerKm).intValue());
            }
        }
        if (changes.containsKey("carrierCode")) {
            String carrierCode = (String) changes.get("carrierCode");
            if (!StringUtils.isBlank(carrierCode)) {
                target.setCarrierCode(carrierCode.trim().toUpperCase());
            }
        }
        if (changes.containsKey("rateMode")) {
            String rateMode = (String) changes.get("rateMode");
            if (!StringUtils.isBlank(rateMode)) {
                target.setRateMode(rateMode.trim().toUpperCase());
            }
        }
        if (changes.containsKey("carrierServiceCode")) {
            Object serviceCode = changes.get("carrierServiceCode");
            if (serviceCode == null) {
                target.setCarrierServiceCode(null);
            } else {
                String value = String.valueOf(serviceCode).trim();
                target.setCarrierServiceCode(value.isEmpty() ? null : value);
            }
        }
        if (changes.containsKey("carrierShopId")) {
            Object carrierShopId = changes.get("carrierShopId");
            if (carrierShopId == null) {
                target.setCarrierShopId(null);
            } else if (carrierShopId instanceof Number) {
                target.setCarrierShopId(((Number) carrierShopId).longValue());
            }
        }

        return target;
    }

    @Override
    public ShippingMethod patchExistingEntity(ShippingMethodPojo changes, ShippingMethod existing) throws BadInputException {
        throw new UnsupportedOperationException("This method has been deprecated");
    }
}
