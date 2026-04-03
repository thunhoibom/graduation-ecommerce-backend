package org.monostudio.api.services;

import org.monostudio.api.models.CartItemPojo;
import org.monostudio.api.models.CartSessionPojo;
import org.monostudio.common.exceptions.BadInputException;

import java.util.List;

/**
 * Shopping cart service.
 *
 * <p>Manages the full cart lifecycle including stock reservations.</p>
 *
 * <p>Each cart is identified by a session token (X-Session-Token header).
 * The session is created on first add-item call if it doesn't exist.</p>
 */
public interface CartService {

    // ─── Cart operations ───────────────────────────────────────────────────────

    /**
     * Get or create a cart session and return its full state.
     */
    CartSessionPojo getOrCreateSession(String sessionToken);

    /**
     * Add an item to the cart. Creates the cart session if it doesn't exist.
     * Reserves stock for the variant.
     *
     * @param sessionToken Cart session token
     * @param variantSku   SKU of the variant to add
     * @param quantity     Number of units
     * @return Updated cart session with all items
     */
    CartSessionPojo addItem(String sessionToken, String variantSku, int quantity) throws BadInputException;

    /**
     * Update the quantity of an existing item in the cart.
     * Adjusts the stock reservation accordingly.
     *
     * @param sessionToken Cart session token
     * @param variantSku   SKU of the variant to update
     * @param quantity     New quantity
     * @return Updated cart session
     */
    CartSessionPojo updateItem(String sessionToken, String variantSku, int quantity) throws BadInputException;

    /**
     * Remove an item from the cart. Releases the stock reservation.
     *
     * @param sessionToken Cart session token
     * @param variantSku   SKU of the variant to remove
     * @return Updated cart session
     */
    CartSessionPojo removeItem(String sessionToken, String variantSku) throws BadInputException;

    /**
     * Clear all items from the cart. Releases all stock reservations.
     *
     * @param sessionToken Cart session token
     */
    void clearCart(String sessionToken);

    /**
     * Get full cart state including resolved variant info and subtotals.
     *
     * @param sessionToken Cart session token
     * @return Cart session with all items
     */
    CartSessionPojo getCart(String sessionToken);

    // ─── Validation ───────────────────────────────────────────────────────────

    /**
     * Validate that all items in the cart are still in stock and variants are active.
     *
     * @param sessionToken Cart session token
     * @return List of items that are no longer available
     */
    List<CartItemPojo> validateCartStock(String sessionToken);

    // ─── Checkout preparation ─────────────────────────────────────────────────

    /**
     * Called before checkout to ensure all reservations are still valid.
     * Throws BadInputException listing any unavailable items.
     */
    void prepareForCheckout(String sessionToken) throws BadInputException;
}
