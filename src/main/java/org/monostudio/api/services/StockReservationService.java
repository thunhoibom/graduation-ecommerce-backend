package org.monostudio.api.services;

import org.monostudio.api.models.StockReservationPojo;
import org.monostudio.common.exceptions.BadInputException;

import java.util.List;

/**
 * Stock reservation service for managing temporary inventory holds during cart/checkout.
 *
 * <p>Lifecycle:</p>
 * <ol>
 *   <li>Customer adds item to cart → {@link #reserve(String, String, int)}</li>
 *   <li>Customer updates quantity → {@link #updateReservation(String, String, int)}</li>
 *   <li>Customer removes item / cart abandoned → {@link #releaseItem(String, String)}</li>
 *   <li>Payment confirmed → {@link #confirm(String)}</li>
 *   <li>Payment failed / cart abandoned → {@link #release(String)}</li>
 *   <li>TTL expired → {@link #expireStaleReservations()}</li>
 * </ol>
 */
public interface StockReservationService {

    // ─── Core reservation operations ────────────────────────────────────────────

    /**
     * Reserve stock for one variant within a cart session.
     *
     * @param sessionId Cart session token
     * @param variantSku SKU of the product variant to reserve
     * @param quantity   Number of units to hold
     * @return The created reservation
     * @throws BadInputException if quantity is invalid or variant not found
     */
    StockReservationPojo reserve(String sessionId, String variantSku, int quantity) throws BadInputException;

    /**
     * Update the quantity of an existing active reservation for the same variant in the same session.
     * Releases the old reservation and creates a new one with the new quantity.
     *
     * @param sessionId Cart session token
     * @param variantSku SKU of the variant
     * @param newQuantity New total quantity (replaces old quantity)
     * @return Updated reservation
     * @throws BadInputException if variant not found
     */
    StockReservationPojo updateReservation(String sessionId, String variantSku, int newQuantity) throws BadInputException;

    // ─── Release ────────────────────────────────────────────────────────────────

    /**
     * Release all active reservations for a cart session (cart cleared / checkout abandoned).
     *
     * @param sessionId Cart session token
     * @return List of released reservations
     */
    List<StockReservationPojo> release(String sessionId);

    /**
     * Release a specific reservation item.
     *
     * @param sessionId Cart session token
     * @param variantSku SKU of the variant to release
     * @return The released reservation, or null if none existed
     */
    StockReservationPojo releaseItem(String sessionId, String variantSku);

    // ─── Confirm ────────────────────────────────────────────────────────────────

    /**
     * Confirm all active reservations for a session — called when payment succeeds.
     * Deducts stockCurrent and decrements stockReserved for each reservation.
     *
     * @param sessionId Cart session token
     * @return List of confirmed reservations
     */
    List<StockReservationPojo> confirm(String sessionId);

    /**
     * Confirm a single reservation item — called for partial order confirmations.
     *
     * @param sessionId Cart session token
     * @param variantSku SKU of the variant to confirm
     * @return The confirmed reservation, or null if none existed
     */
    StockReservationPojo confirmItem(String sessionId, String variantSku);

    // ─── Query ─────────────────────────────────────────────────────────────────

    /**
     * List all active (RESERVED) reservations for a cart session.
     *
     * @param sessionId Cart session token
     * @return List of active reservations
     */
    List<StockReservationPojo> getActiveReservations(String sessionId);

    /**
     * Check available stock for a variant (stockCurrent - stockReserved).
     *
     * @param variantSku SKU of the variant
     * @return Available quantity, or null if variant not found
     */
    Integer getAvailableStock(String variantSku);

    // ─── Maintenance ───────────────────────────────────────────────────────────

    /**
     * Release all reservations that have passed their expiry time.
     * Should be called by a scheduled job (e.g. every 5 minutes via cron).
     *
     * @return Number of reservations released
     */
    int expireStaleReservations();
}
