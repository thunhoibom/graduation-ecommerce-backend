package org.monostudio.jpa.services.conversion.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.DiscountCodePojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.DiscountCode;
import org.monostudio.jpa.services.conversion.DiscountCodesConverterService;

import java.time.LocalDateTime;

@Transactional
@Service
public class DiscountCodesConverterServiceImpl
    implements DiscountCodesConverterService {

    @Override
    public DiscountCodePojo convertToPojo(DiscountCode source) {
        LocalDateTime now = LocalDateTime.now();
        boolean currentlyValid = source.isActive()
            && (source.getValidFrom() == null || !source.getValidFrom().isAfter(now))
            && (source.getValidUntil() == null || !source.getValidUntil().isBefore(now));

        Integer remainingUses = null;
        if (source.getMaxUses() != null) {
            remainingUses = Math.max(0, source.getMaxUses() - source.getUseCount());
        }

        return DiscountCodePojo.builder()
            .code(source.getCode())
            .description(source.getDescription())
            .type(source.getType())
            .value(source.getValue())
            .maxUses(source.getMaxUses())
            .useCount(source.getUseCount())
            .maxUsesPerCustomer(source.getMaxUsesPerCustomer())
            .minCartValue(source.getMinCartValue())
            .validFrom(source.getValidFrom())
            .validUntil(source.getValidUntil())
            .active(source.isActive())
            .currentlyValid(currentlyValid)
            .remainingUses(remainingUses)
            .createdAt(source.getCreatedAt())
            .updatedAt(source.getUpdatedAt())
            .build();
    }

    @Override
    public DiscountCode convertToNewEntity(DiscountCodePojo source) throws BadInputException {
        DiscountCode target = DiscountCode.builder()
            .code(source.getCode().toUpperCase().trim())
            .description(source.getDescription())
            .type(source.getType())
            .value(source.getValue())
            .maxUses(source.getMaxUses())
            .maxUsesPerCustomer(source.getMaxUsesPerCustomer())
            .minCartValue(source.getMinCartValue())
            .validFrom(source.getValidFrom())
            .validUntil(source.getValidUntil())
            .active(source.getActive() != null ? source.getActive() : true)
            .build();
        return target;
    }

    @Override
    public DiscountCode applyChangesToExistingEntity(DiscountCodePojo source, DiscountCode target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
