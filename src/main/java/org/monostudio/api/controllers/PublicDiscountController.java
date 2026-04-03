package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.DiscountValidationResult;
import org.monostudio.api.services.DiscountService;

@RestController
@RequestMapping("/public/discount")
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
     */
    @GetMapping("/validate")
    @Operation(summary = "Validate a discount code against a cart subtotal")
    public DiscountValidationResult validateDiscount(
        @RequestParam("code") String code,
        @RequestParam("subtotal") int subtotal
    ) {
        return discountService.validateDiscount(code, subtotal);
    }
}
