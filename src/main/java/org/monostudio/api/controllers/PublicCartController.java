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
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.CartItemPojo;
import org.monostudio.api.models.CartSessionPojo;
import org.monostudio.api.services.CartService;
import org.monostudio.common.exceptions.BadInputException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/cart")
@Tag(name = "Cart")
public class PublicCartController {

    private final CartService cartService;

    @Autowired
    public PublicCartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * GET /public/cart
     * Get current cart state. Creates a new session if none exists.
     */
    @GetMapping
    @Operation(summary = "Get current cart state (creates session if none)")
    public CartSessionPojo getCart(
        @RequestHeader(name = "X-Session-Token", required = false) String sessionToken
    ) {
        return cartService.getCart(sessionToken);
    }

    /**
     * POST /public/cart/items
     * Add an item to the cart.
     * Body: { "variantSku": "SKU-RED-S", "quantity": 1 }
     */
    @PostMapping("/items")
    @Operation(summary = "Add an item to the cart")
    public CartSessionPojo addItem(
        @RequestHeader(name = "X-Session-Token", required = false) String sessionToken,
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
        return cartService.addItem(sessionToken, variantSku, quantity);
    }

    /**
     * PATCH /public/cart/items/{variantSku}
     * Update item quantity.
     * Body: { "quantity": 2 }
     */
    @PatchMapping("/items/{variantSku}")
    @Operation(summary = "Update item quantity in the cart")
    public CartSessionPojo updateItem(
        @RequestHeader(name = "X-Session-Token", required = false) String sessionToken,
        @PathVariable String variantSku,
        @RequestBody Map<String, Integer> body
    ) throws BadInputException {
        Integer quantity = body.get("quantity");
        if (quantity == null || quantity <= 0) {
            throw new BadInputException("quantity must be a positive integer");
        }
        return cartService.updateItem(sessionToken, variantSku, quantity);
    }

    /**
     * DELETE /public/cart/items/{variantSku}
     * Remove a specific item from the cart.
     */
    @DeleteMapping("/items/{variantSku}")
    @Operation(summary = "Remove a specific item from the cart")
    public CartSessionPojo removeItem(
        @RequestHeader(name = "X-Session-Token", required = false) String sessionToken,
        @PathVariable String variantSku
    ) throws BadInputException {
        return cartService.removeItem(sessionToken, variantSku);
    }

    /**
     * DELETE /public/cart
     * Clear all items from the cart.
     */
    @DeleteMapping
    @Operation(summary = "Clear the entire cart")
    public ResponseEntity<Void> clearCart(
        @RequestHeader(name = "X-Session-Token", required = false) String sessionToken
    ) {
        cartService.clearCart(sessionToken);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /public/cart/validate
     * Validate that all cart items are still in stock and available.
     */
    @GetMapping("/validate")
    @Operation(summary = "Validate cart stock availability before checkout")
    public ResponseEntity<Map<String, Object>> validateCart(
        @RequestHeader(name = "X-Session-Token", required = false) String sessionToken
    ) {
        List<CartItemPojo> unavailable = cartService.validateCartStock(sessionToken);
        if (unavailable.isEmpty()) {
            return ResponseEntity.ok(Map.of("valid", true, "message", "All items available"));
        }
        return ResponseEntity.ok(Map.of(
            "valid", false,
            "message", "Some items are unavailable",
            "unavailableItems", unavailable
        ));
    }
}
