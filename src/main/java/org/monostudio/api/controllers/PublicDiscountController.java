package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.DiscountValidationResult;
import org.monostudio.api.services.DiscountService;
import org.monostudio.config.cache.CacheNames;

@RestController
@RequestMapping("/api/public/discount")
@Tag(name = "Discount codes (public)")
public class PublicDiscountController {

    private final DiscountService discountService;

    @Autowired
    public PublicDiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    /**
     * GET /public/discount/validate?code=SUMMER20&subtotal=200000
     * Validates a discount code against a cart subtotal and returns the computed discount.
     * Note: customerId is null here — per-customer limit check runs at markAsPaid
     * when the actual customer is resolved from the order.
     */
    @GetMapping("/validate")
    @Operation(summary = "Validate a discount code against a cart subtotal")
    @Cacheable(cacheNames = CacheNames.PUBLIC_DISCOUNT_VALIDATION, key = "@cacheKeyBuilder.discountValidation(#code, #subtotal)")
    public DiscountValidationResult validateDiscount(
        @RequestParam("code") String code,
        @RequestParam("subtotal") int subtotal
    ) {
        return discountService.validateDiscount(code, subtotal, null);
    }
}
