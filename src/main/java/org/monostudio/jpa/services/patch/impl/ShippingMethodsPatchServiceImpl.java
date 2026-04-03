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

        return target;
    }

    @Override
    public ShippingMethod patchExistingEntity(ShippingMethodPojo changes, ShippingMethod existing) throws BadInputException {
        throw new UnsupportedOperationException("This method has been deprecated");
    }
}
