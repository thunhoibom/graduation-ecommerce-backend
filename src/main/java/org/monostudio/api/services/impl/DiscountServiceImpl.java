package org.monostudio.api.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.DiscountValidationResult;
import org.monostudio.api.services.DiscountService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.DiscountCode;
import org.monostudio.jpa.entities.DiscountUsage;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.jpa.repositories.DiscountCodesRepository;
import org.monostudio.jpa.repositories.DiscountUsagesRepository;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.config.cache.CacheNames;

import java.time.LocalDateTime;

@Service
public class DiscountServiceImpl
    implements DiscountService {

    private static final Logger logger = LoggerFactory.getLogger(DiscountServiceImpl.class);

    private final DiscountCodesRepository discountCodesRepository;
    private final DiscountUsagesRepository discountUsagesRepository;
    private final CustomersRepository customersRepository;
    private final OrdersRepository ordersRepository;

    @Autowired
    public DiscountServiceImpl(
        DiscountCodesRepository discountCodesRepository,
        DiscountUsagesRepository discountUsagesRepository,
        CustomersRepository customersRepository,
        OrdersRepository ordersRepository
    ) {
        this.discountCodesRepository = discountCodesRepository;
        this.discountUsagesRepository = discountUsagesRepository;
        this.customersRepository = customersRepository;
        this.ordersRepository = ordersRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountValidationResult validateDiscount(String code, int subtotal, Long customerId) {
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

        // Check per-customer usage limit using DiscountUsage table.
        if (discount.getMaxUsesPerCustomer() != null && customerId != null) {
            int customerUseCount = discountUsagesRepository
                .countByDiscountIdAndCustomerId(discount.getId(), customerId);
            if (customerUseCount >= discount.getMaxUsesPerCustomer()) {
                return DiscountValidationResult.invalid(
                    "You have already used this discount code " + customerUseCount
                        + " time(s). Maximum allowed: " + discount.getMaxUsesPerCustomer());
            }
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
    @CacheEvict(cacheNames = CacheNames.PUBLIC_DISCOUNT_VALIDATION, allEntries = true)
    public void redeemDiscount(String code, int subtotal, Long customerId, Long orderId) throws BadInputException {
        if (StringUtils.isBlank(code)) {
            throw new BadInputException("Discount code is required");
        }

        DiscountValidationResult validation = validateDiscount(code, subtotal, customerId);
        if (!validation.isValid()) {
            throw new BadInputException(validation.getMessage());
        }

        DiscountCode discount = discountCodesRepository.findByCodeIgnoreCase(code)
            .orElseThrow(() -> new BadInputException("Invalid discount code"));

        // Atomic increment of global use count
        int rows = discountCodesRepository.incrementUseCount(discount.getId());
        if (rows == 0) {
            throw new BadInputException("Failed to apply discount — code may have reached its limit");
        }

        // Record per-customer usage for per-customer limit enforcement on future orders
        if (customerId != null) {
            var customer = customersRepository.findById(customerId).orElse(null);
            var order    = orderId != null ? ordersRepository.getReferenceById(orderId) : null;
            DiscountUsage usage = DiscountUsage.builder()
                .discountCode(discount)
                .customer(customer)
                .order(order)
                .build();
            discountUsagesRepository.saveAndFlush(usage);
        }

        logger.info("Redeemed discount code '{}' for customer {}, orderId={}", code, customerId, orderId);
    }

    private int calculateDiscountAmount(DiscountCode discount, int subtotal) {
        String normalizedType = discount.getType() == null
            ? ""
            : discount.getType().trim().toUpperCase();
        return switch (normalizedType) {
            case "PERCENT", DiscountCode.TYPE_PERCENTAGE -> {
                // value is a percentage (1–100)
                int percentage = Math.min(100, Math.max(1, discount.getValue()));
                yield (int) Math.round((long) subtotal * percentage / 100.0);
            }
            case "FIXED", DiscountCode.TYPE_FIXED_AMOUNT -> {
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
