package org.monostudio.api.services;

import org.monostudio.api.models.StockReservationPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.StockAdjustment;

import java.util.List;

/**
 * Stock reservation service for managing temporary inventory holds during cart/checkout.
 */
public interface StockReservationService {

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

    /**
     * Permanently deduct stock for one order line. Uses the order quantity as source of
     * truth and still works when cart reservations were released after checkout.
     *
     * @param sessionId  Cart session token linked to the order (optional)
     * @param orderId    Order id for audit logging and idempotency
     * @param variantSku SKU of the variant sold
     * @param units      Units to deduct from on-hand stock
     */
    void commitOrderLine(String sessionId, Long orderId, String variantSku, int units);

    /**
     * Restore on-hand stock for one order line after goods return to warehouse.
     * Only runs when the line was previously committed and not already restored.
     */
    void restoreOrderLine(
        String sessionId,
        Long orderId,
        String variantSku,
        int units,
        StockAdjustment.StockAdjustmentReason reason
    );

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

    /**
     * Restore stockCurrent after a paid order is cancelled or rejected.
     * This adds the quantity back to stockCurrent AND decrements stockReserved
     * (the reserved amount has been previously deducted from current at payment confirmation).
     * Used only for orders that were PAID before being cancelled/rejected.
     *
     * @param sessionId   Cart session token
     * @param variantSku  SKU of the variant to restore
     * @param quantity    Number of units to return to available stock
     * @param orderId     Order ID for audit logging
     * @param reason      The adjustment reason — ORDER_CANCELLED or ORDER_REJECTED
     */
    void restoreStockCurrent(String sessionId, String variantSku, int quantity, Long orderId,
        StockAdjustment.StockAdjustmentReason reason);

    /**
     * Release all reservations that have passed their expiry time.
     * Should be called by a scheduled job (e.g. every 5 minutes via cron).
     *
     * @return Number of reservations released
     */
    int expireStaleReservations();
}
