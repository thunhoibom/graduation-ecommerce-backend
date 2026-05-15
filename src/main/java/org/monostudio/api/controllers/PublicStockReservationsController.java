package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.StockReservationPojo;
import org.monostudio.api.services.StockReservationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.repositories.ProductVariantsRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/cart/reservations")
@Tag(name = "Cart — Stock Reservations")
public class PublicStockReservationsController {

    private final StockReservationService stockReservationService;
    private final ProductVariantsRepository productVariantsRepository;

    @Autowired
    public PublicStockReservationsController(
        StockReservationService stockReservationService,
        ProductVariantsRepository productVariantsRepository
    ) {
        this.stockReservationService = stockReservationService;
        this.productVariantsRepository = productVariantsRepository;
    }

    /**
     * GET /public/cart/reservations?sessionId=xxx
     * List all active reservations for the given cart session.
     */
    @GetMapping
    @Operation(summary = "List active stock reservations for a cart session")
    public List<StockReservationPojo> listActive(
        @RequestHeader("X-Session-Token") String sessionToken
    ) {
        return stockReservationService.getActiveReservations(sessionToken);
    }

    /**
     * POST /public/cart/reservations
     * Reserve stock for a variant in the cart.
     * Body: { "variantSku": "...", "quantity": 1 }
     */
    @PostMapping
    @Operation(summary = "Reserve stock for a variant in the cart")
    public StockReservationPojo reserve(
        @RequestHeader("X-Session-Token") String sessionToken,
        @RequestBody Map<String, Object> body
    ) throws BadInputException {
        String variantSku = (String) body.get("variantSku");
        Integer quantity = (Integer) body.get("quantity");
        if (variantSku == null || variantSku.isBlank()) {
            throw new BadInputException("variantSku is required");
        }
        if (quantity == null || quantity <= 0) {
            throw new BadInputException("quantity must be a positive integer");
        }
        return stockReservationService.reserve(sessionToken, variantSku, quantity);
    }

    /**
     * PATCH /public/cart/reservations
     * Update the quantity of an existing reservation (e.g. cart item qty changed).
     * Body: { "variantSku": "...", "quantity": 2 }
     */
    @PatchMapping
    @Operation(summary = "Update the quantity of an existing reservation")
    public StockReservationPojo update(
        @RequestHeader("X-Session-Token") String sessionToken,
        @RequestBody Map<String, Object> body
    ) throws BadInputException {
        String variantSku = (String) body.get("variantSku");
        Integer quantity = (Integer) body.get("quantity");
        if (variantSku == null || variantSku.isBlank()) {
            throw new BadInputException("variantSku is required");
        }
        if (quantity == null || quantity <= 0) {
            throw new BadInputException("quantity must be a positive integer");
        }
        return stockReservationService.updateReservation(sessionToken, variantSku, quantity);
    }

    /**
     * DELETE /public/cart/reservations?sessionId=xxx
     * Release all active reservations for a cart (cart cleared / checkout abandoned).
     */
    @DeleteMapping
    @Operation(summary = "Release all reservations for a cart session (cart cleared)")
    public List<StockReservationPojo> releaseAll(
        @RequestHeader("X-Session-Token") String sessionToken
    ) {
        return stockReservationService.release(sessionToken);
    }

    /**
     * DELETE /public/cart/reservations/{variantSku}
     * Release a single item reservation.
     */
    @DeleteMapping("/{variantSku}")
    @Operation(summary = "Release a specific reservation item from the cart")
    public ResponseEntity<StockReservationPojo> releaseItem(
        @RequestHeader("X-Session-Token") String sessionToken,
        @PathVariable String variantSku
    ) {
        StockReservationPojo released = stockReservationService.releaseItem(sessionToken, variantSku);
        if (released == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(released);
    }

    /**
     * GET /public/cart/reservations/availability?variantSku=xxx
     * Check available (non-reserved) stock for a variant.
     */
    @GetMapping("/availability")
    @Operation(summary = "Check available stock for a variant")
    public ResponseEntity<Map<String, Object>> checkAvailability(
        @RequestParam String variantSku
    ) {
        ProductVariant variant = productVariantsRepository.findBySku(variantSku).orElse(null);
        if (variant == null) {
            return ResponseEntity.notFound().build();
        }
        int onHand = variant.getStockCurrent();
        int reserved = variant.getStockReserved();
        int available = Math.max(0, onHand - reserved);
        return ResponseEntity.ok(Map.of(
            "variantSku", variantSku,
            // Backward-compatible field name
            "availableStock", available,
            // Canonical inventory fields
            "onHand", onHand,
            "reserved", reserved,
            "availableToSell", available
        ));
    }

    /**
     * POST /public/cart/reservations/confirm
     * Confirm all active reservations — called when payment succeeds.
     * Body: { "sessionId": "xxx" }  (sessionId in body, sessionToken in header both accepted)
     */
    @PostMapping("/confirm")
    @Operation(summary = "Confirm all reservations (payment succeeded)")
    public List<StockReservationPojo> confirm(
        @RequestHeader(value = "X-Session-Token", required = false) String sessionToken,
        @RequestBody(required = false) Map<String, String> body
    ) {
        String sessionId = resolveSessionId(sessionToken, body);
        return stockReservationService.confirm(sessionId);
    }

    private String resolveSessionId(String headerToken, Map<String, String> body) {
        if (body != null && body.containsKey("sessionId") && !body.get("sessionId").isBlank()) {
            return body.get("sessionId");
        }
        return headerToken;
    }
}
