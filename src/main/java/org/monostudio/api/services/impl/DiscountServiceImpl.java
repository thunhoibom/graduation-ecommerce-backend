package org.monostudio.api.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.DiscountValidationResult;
import org.monostudio.api.services.DiscountService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.DiscountCode;
import org.monostudio.jpa.repositories.DiscountCodesRepository;

import java.time.LocalDateTime;

@Service
public class DiscountServiceImpl
    implements DiscountService {

    private static final Logger logger = LoggerFactory.getLogger(DiscountServiceImpl.class);

    private final DiscountCodesRepository discountCodesRepository;

    @Autowired
    public DiscountServiceImpl(DiscountCodesRepository discountCodesRepository) {
        this.discountCodesRepository = discountCodesRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountValidationResult validateDiscount(String code, int subtotal) {
        if (StringUtils.isBlank(code)) {
            return DiscountValidationResult.noDiscount();
        }

        LocalDateTime now = LocalDateTime.now();
        DiscountCode discount = discountCodesRepository.findValidByCode(code, now)
            .orElse(null);

        if (discount == null) {
            // Try to give a helpful message — code might exist but be expired/inactive
            var existing = discountCodesRepository.findByCodeIgnoreCase(code);
            if (existing.isPresent()) {
                DiscountCode d = existing.get();
                if (!d.isActive()) {
                    return DiscountValidationResult.invalid("This discount code is no longer active");
                }
                if (d.getValidUntil() != null && d.getValidUntil().isBefore(now)) {
                    return DiscountValidationResult.invalid("This discount code has expired");
                }
                if (d.getValidFrom() != null && d.getValidFrom().isAfter(now)) {
                    return DiscountValidationResult.invalid("This discount code is not yet valid");
                }
                if (d.getMaxUses() != null && d.getUseCount() >= d.getMaxUses()) {
                    return DiscountValidationResult.invalid("This discount code has reached its usage limit");
                }
            }
            return DiscountValidationResult.invalid("Invalid discount code");
        }

        // Check max uses (global)
        if (discount.getMaxUses() != null && discount.getUseCount() >= discount.getMaxUses()) {
            return DiscountValidationResult.invalid("This discount code has reached its usage limit");
        }

        // Check per-customer usage limit.
        // NOTE: Full enforcement requires a DiscountUsage tracking table
        // (discount_code_id, customer_id, use_count). Without it, this check
        // is a soft guard only — the global useCount above is the authoritative limit.
        // TODO: Create DiscountUsage entity to track (discount_code, customer_id) usage
        //       and add a DiscountUsagesRepository.countByCodeAndCustomer(code, customerId) query.
        // Until DiscountUsage exists, per-customer limits cannot be fully enforced.
        // The global useCount provides a hard ceiling in the meantime.
        if (discount.getMaxUsesPerCustomer() != null) {
            logger.debug("maxUsesPerCustomer={} set for code '{}', but per-customer "
                + "usage tracking is not yet implemented — relying on global useCount",
                discount.getMaxUsesPerCustomer(), discount.getCode());
        }

        // Check minimum cart value
        if (discount.getMinCartValue() != null && subtotal < discount.getMinCartValue()) {
            int required = discount.getMinCartValue();
            return DiscountValidationResult.invalid(
                "Minimum cart value of " + required + " cents required for this code");
        }

        // Calculate discount amount
        int discountAmount = calculateDiscountAmount(discount, subtotal);

        return DiscountValidationResult.builder()
            .valid(true)
            .code(discount.getCode())
            .type(discount.getType())
            .value(discount.getValue())
            .discountAmount(discountAmount)
            .message(discount.getDescription())
            .build();
    }

    @Override
    @Transactional
    public void redeemDiscount(String code, int subtotal, Long customerId) throws BadInputException {
        if (StringUtils.isBlank(code)) {
            throw new BadInputException("Discount code is required");
        }

        DiscountValidationResult validation = validateDiscount(code, subtotal);
        if (!validation.isValid()) {
            throw new BadInputException(validation.getMessage());
        }

        DiscountCode discount = discountCodesRepository.findByCodeIgnoreCase(code)
            .orElseThrow(() -> new BadInputException("Invalid discount code"));

        // Atomic increment of use count
        int rows = discountCodesRepository.incrementUseCount(discount.getId());
        if (rows == 0) {
            throw new BadInputException("Failed to apply discount — code may have reached its limit");
        }

        logger.info("Redeemed discount code '{}' for customer {}", code, customerId);
    }

    private int calculateDiscountAmount(DiscountCode discount, int subtotal) {
        return switch (discount.getType()) {
            case DiscountCode.TYPE_PERCENTAGE -> {
                // value is a percentage (1–100)
                int percentage = Math.min(100, Math.max(1, discount.getValue()));
                yield (int) Math.round((long) subtotal * percentage / 100.0);
            }
            case DiscountCode.TYPE_FIXED_AMOUNT -> {
                // value is in cents — cannot exceed subtotal
                yield Math.min(discount.getValue(), subtotal);
            }
            case DiscountCode.TYPE_FREE_SHIPPING -> {
                // Free shipping — discount amount is determined by shipping cost at checkout
                // We return 0 here; the checkout flow handles shipping discount separately
                yield 0;
            }
            default -> 0;
        };
    }
}
