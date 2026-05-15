package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.ShippingRatePojo;
import org.monostudio.api.models.ShippingRateRequestContext;
import org.monostudio.api.services.ShippingMethodsService;
import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.jpa.repositories.ShippingMethodsRepository;
import org.monostudio.config.cache.CacheNames;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/shipping")
@Tag(name = "Public shipping")
public class PublicShippingMethodsController {
    private final ShippingMethodsRepository shippingMethodsRepository;
    private final ShippingMethodsService shippingMethodsService;

    @Autowired
    public PublicShippingMethodsController(
        ShippingMethodsRepository shippingMethodsRepository,
        ShippingMethodsService shippingMethodsService
    ) {
        this.shippingMethodsRepository = shippingMethodsRepository;
        this.shippingMethodsService = shippingMethodsService;
    }

    /**
     * Returns all active shipping methods with their computed fee based on the given subtotal.
     * Fee is 0 if the subtotal meets or exceeds the method's freeShippingThreshold (if set).
     *
     * @param subtotal The cart subtotal in VND (optional — if omitted, base fees are returned)
     * @return List of shipping rate responses
     */
    @GetMapping("/methods")
    @Operation(summary = "List active shipping methods with computed fees")
    @Cacheable(
        cacheNames = CacheNames.PUBLIC_SHIPPING_METHODS,
        key = "@cacheKeyBuilder.shipping(#subtotal, #latitude, #longitude, #toDistrictId, #toWardCode)"
    )
    public List<ShippingRatePojo> getShippingMethods(
        @RequestParam(required = false) Integer subtotal,
        @RequestParam(required = false) Double latitude,
        @RequestParam(required = false) Double longitude,
        @RequestParam(required = false) Integer toDistrictId,
        @RequestParam(required = false) String toWardCode
    ) {
        List<ShippingMethod> activeMethods = shippingMethodsRepository.findByActiveTrue();
        ShippingRateRequestContext context = ShippingRateRequestContext.builder()
            .subtotal(subtotal)
            .latitude(latitude)
            .longitude(longitude)
            .toDistrictId(toDistrictId)
            .toWardCode(toWardCode)
            .build();
        return activeMethods.stream()
            .map(method -> shippingMethodsService.computeRate(method, context))
            .collect(Collectors.toList());
    }
}
