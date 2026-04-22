package org.monostudio.api.services;

import org.monostudio.api.models.StockAdjustmentPojo;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.StockAdjustment;

import java.time.Instant;
import java.util.List;

/**
 * Central audit-log service for stock adjustments.
 * All stock mutations in the system must go through this service
 * to ensure a complete and tamper-evident audit trail.
 */
public interface StockAdjustmentService {

    /**
     * Records a stock adjustment for a variant.
     * Reads the current stock level, calculates before/after, persists the entry.
     *
     * @param variantId     The affected variant ID
     * @param reason        The reason for this adjustment
     * @param quantityDelta Positive = add, Negative = deduct
     * @return The created audit log entry
     */
    StockAdjustmentPojo record(Long variantId, StockAdjustment.StockAdjustmentReason reason, int quantityDelta);

    /**
     * Records a stock adjustment with full context.
     *
     * @param variantId     The affected variant ID
     * @param reason        The reason for this adjustment
     * @param quantityDelta Positive = add, Negative = deduct
     * @param description   Human-readable description
     * @param sessionId     Cart session ID (if applicable)
     * @param orderId       Order ID (if applicable)
     * @param returnRequestId Return request ID (if applicable)
     * @param performedBy    Admin user ID (if applicable)
     * @return The created audit log entry
     */
    StockAdjustmentPojo recordFull(
        Long variantId,
        StockAdjustment.StockAdjustmentReason reason,
        int quantityDelta,
        String description,
        String sessionId,
        Long orderId,
        Long returnRequestId,
        Long performedBy
    );

    /**
     * Records a stock adjustment using an already-loaded ProductVariant entity.
     * Prefer this overload when the entity is already in scope to avoid extra DB lookups.
     *
     * @param variant        The already-loaded ProductVariant
     * @param reason         The reason for this adjustment
     * @param quantityDelta  Positive = add, Negative = deduct
     * @param description    Human-readable description
     * @param sessionId      Cart session ID (if applicable)
     * @param orderId        Order ID (if applicable)
     * @param returnRequestId Return request ID (if applicable)
     * @param performedBy     Admin user ID (if applicable)
     * @return The created audit log entry
     */
    StockAdjustmentPojo recordForVariant(
        ProductVariant variant,
        StockAdjustment.StockAdjustmentReason reason,
        int quantityDelta,
        String description,
        String sessionId,
        Long orderId,
        Long returnRequestId,
        Long performedBy
    );

    /**
     * Retrieves all adjustments for a specific variant, ordered by most recent first.
     *
     * @param variantId The variant ID
     * @return List of adjustments
     */
    List<StockAdjustmentPojo> getByVariant(Long variantId);

    /**
     * Retrieves adjustments for a variant with pagination.
     *
     * @param variantId The variant ID
     * @param page      Page number (0-based)
     * @param size      Page size
     * @return Page of adjustments
     */
    List<StockAdjustmentPojo> getByVariant(Long variantId, int page, int size);

    /**
     * Retrieves adjustments for a variant identified by SKU with pagination.
     *
     * @param sku  Variant SKU
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of adjustments
     */
    List<StockAdjustmentPojo> getBySku(String sku, int page, int size);

    /**
     * Retrieves all adjustments for an order.
     *
     * @param orderId The order ID
     * @return List of adjustments
     */
    List<StockAdjustmentPojo> getByOrder(Long orderId);

    /**
     * Retrieves all adjustments for a return request.
     *
     * @param returnRequestId The return request ID
     * @return List of adjustments
     */
    List<StockAdjustmentPojo> getByReturnRequest(Long returnRequestId);

    /**
     * Retrieves adjustments within a date range with pagination.
     *
     * @param start Start of range
     * @param end   End of range
     * @param page  Page number (0-based)
     * @param size  Page size
     * @return Page of adjustments
     */
    List<StockAdjustmentPojo> getByDateRange(Instant start, Instant end, int page, int size);

    /**
     * Retrieves all adjustments with pagination (most recent first).
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of adjustments
     */
    List<StockAdjustmentPojo> getAll(int page, int size);

    /**
     * Applies a manual stock delta on a variant and records the audit entry.
     * This method updates variant stockCurrent and writes to stock_adjustments atomically.
     *
     * @param variantId      Variant ID
     * @param quantityDelta  Positive (inbound), negative (outbound)
     * @param reason         Stock adjustment reason enum
     * @param description    Required business reason text for traceability
     * @param orderId        Optional related order id
     * @param performedBy    Optional user id
     * @return Created adjustment entry
     */
    StockAdjustmentPojo applyManualDelta(
        Long variantId,
        int quantityDelta,
        StockAdjustment.StockAdjustmentReason reason,
        String description,
        Long orderId,
        Long performedBy
    );

    /**
     * Sets a manual target stock level and records the implied delta.
     *
     * @param variantId      Variant ID
     * @param targetStock    Absolute stock target
     * @param reason         Stock adjustment reason enum
     * @param description    Required business reason text for traceability
     * @param orderId        Optional related order id
     * @param performedBy    Optional user id
     * @return Created adjustment entry
     */
    StockAdjustmentPojo applyManualTargetStock(
        Long variantId,
        int targetStock,
        StockAdjustment.StockAdjustmentReason reason,
        String description,
        Long orderId,
        Long performedBy
    );
}
