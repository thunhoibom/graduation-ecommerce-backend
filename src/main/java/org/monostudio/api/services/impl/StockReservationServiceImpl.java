package org.monostudio.api.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.StockReservationPojo;
import org.monostudio.api.services.StockReservationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.StockReservation;
import org.monostudio.jpa.entities.StockAdjustment;
import org.monostudio.jpa.repositories.ProductVariantsRepository;
import org.monostudio.jpa.repositories.StockReservationsRepository;
import org.monostudio.api.services.StockAdjustmentService;
import org.monostudio.mailing.kafka.KafkaMailProducer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StockReservationServiceImpl
    implements StockReservationService {

    private static final Logger logger = LoggerFactory.getLogger(StockReservationServiceImpl.class);
    private static final int DEFAULT_TTL_MINUTES = StockReservation.DEFAULT_TTL_MINUTES;

    private final StockReservationsRepository stockReservationsRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final StockAdjustmentService stockAdjustmentService;
    private final KafkaMailProducer kafkaMailProducer;

    @Autowired
    public StockReservationServiceImpl(
        StockReservationsRepository stockReservationsRepository,
        ProductVariantsRepository productVariantsRepository,
        StockAdjustmentService stockAdjustmentService,
        KafkaMailProducer kafkaMailProducer
    ) {
        this.stockReservationsRepository = stockReservationsRepository;
        this.productVariantsRepository = productVariantsRepository;
        this.stockAdjustmentService = stockAdjustmentService;
        this.kafkaMailProducer = kafkaMailProducer;
    }

    // ─── Legacy Long-based methods (used by OrdersProcessServiceImpl) ─────────────

    /**
     * @deprecated Use {@link #getAvailableStock(String)} with SKU instead.
     */

    @Transactional(readOnly = true)
    public int getAvailableStock(Long variantId) {
        ProductVariant v = productVariantsRepository.getById(variantId);
        return Math.max(0, v.getStockCurrent() - v.getStockReserved());
    }

    /**
     * @deprecated Use {@link #reserve(String, String, int)} with session ID instead.
     */
    @Transactional
    public void reserveStock(Long variantId, int quantity) {
        int updated = stockReservationsRepository.tryIncrementReserved(variantId, quantity);
        if (updated == 0) {
            ProductVariant v = productVariantsRepository.getById(variantId);
            throw new IllegalArgumentException(
                "Insufficient stock for variant " + variantId
                    + ": available=" + (v.getStockCurrent() - v.getStockReserved()));
        }
    }

    /**
     * @deprecated Use {@link #releaseItem(String, String)} with SKU instead.
     */

    @Transactional
    public void releaseStock(Long variantId, int quantity) {
        stockReservationsRepository.decrementReserved(variantId, quantity);
    }

    /**
     * @deprecated Use {@link #confirmItem(String, String)} with SKU instead.
     */
    @Transactional
    public void commitReservation(Long variantId, int quantity) {
        stockReservationsRepository.confirmDeduct(variantId, quantity);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    private ProductVariant findVariantBySku(String sku) throws BadInputException {
        ProductVariant variant = productVariantsRepository.findBySku(sku)
            .orElseThrow(() -> new BadInputException("Variant not found with SKU: " + sku));
        return productVariantsRepository.findByIdWithLock(variant.getId())
            .orElseThrow(() -> new BadInputException("Variant not found with ID: " + variant.getId()));
    }

    private StockReservationPojo toPojo(StockReservation reservation) {
        ProductVariant variant = reservation.getVariant();
        StockReservationPojo.StockReservationPojoBuilder builder = StockReservationPojo.builder()
            .id(reservation.getId())
            .sessionId(reservation.getSessionId())
            .variantSkuResolved(reservation.getVariant().getSku())
            .quantity(reservation.getQuantity())
            .status(reservation.getStatus())
            .expiresAt(reservation.getExpiresAt())
            .createdAt(reservation.getCreatedAt())
            .updatedAt(reservation.getUpdatedAt());

        if (variant != null) {
            int onHand = variant.getStockCurrent();
            int reserved = variant.getStockReserved();
            builder.variantSize(variant.getSize())
                .variantColor(variant.getColor())
                .onHand(onHand)
                .reserved(reserved)
                .availableToSell(Math.max(0, onHand - reserved));
            if (variant.getProduct() != null) {
                builder.productName(variant.getProduct().getName())
                    .productBarcode(variant.getProduct().getBarcode());
            }
        }
        return builder.build();
    }

    /**
     * Atomically checks availability and increments stockReserved.
     * Returns true if the reservation succeeded, false if insufficient stock.
     */
    private boolean tryIncrementReserved(Long variantId, int delta) {
        int rowsUpdated = stockReservationsRepository.tryIncrementReserved(variantId, delta);
        return rowsUpdated > 0;
    }

    // ─── Core operations ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public StockReservationPojo reserve(String sessionId, String variantSku, int quantity)
        throws BadInputException {
        if (quantity <= 0) {
            throw new BadInputException("Quantity must be greater than zero");
        }

        ProductVariant variant = findVariantBySku(variantSku);
        Long variantId = variant.getId();

        // Atomic check-and-reserve: only succeeds if enough stock is available
        if (!tryIncrementReserved(variantId, quantity)) {
            int available = variant.getStockCurrent() - variant.getStockReserved();
            throw new BadInputException(
                "Insufficient stock for variant '" + variantSku + "': requested " + quantity
                    + ", available " + Math.max(0, available));
        }

        // Also persist the reservation record for session tracking
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(DEFAULT_TTL_MINUTES);
        StockReservation reservation = StockReservation.builder()
            .sessionId(sessionId)
            .variant(variant)
            .quantity(quantity)
            .status(StockReservation.STATUS_RESERVED)
            .expiresAt(expiresAt)
            .build();

        StockReservation saved = stockReservationsRepository.saveAndFlush(reservation);
        logger.info("Reserved {} units of variant {} (session={}, reservationId={})",
            quantity, variantSku, sessionId, saved.getId());

        // Low-stock alert
        ProductVariant refreshed = productVariantsRepository.getById(variantId);
        int newAvailable = refreshed.getStockCurrent() - refreshed.getStockReserved();
        if (newAvailable <= refreshed.getStockCritical()) {
            logger.warn("Variant '{}' (id={}) reached low-stock threshold: available={}, critical={}",
                variantSku, variantId, newAvailable, refreshed.getStockCritical());
            String productName = refreshed.getProduct() != null
                ? refreshed.getProduct().getName()
                : "Variant #" + variantId;
            kafkaMailProducer.sendLowStockAlert(productName, newAvailable);
        }

        return toPojo(saved);
    }

    @Override
    @Transactional
    public StockReservationPojo updateReservation(String sessionId, String variantSku, int newQuantity)
        throws BadInputException {
        if (newQuantity <= 0) {
            // Treat zero/negative as a release
            return releaseItem(sessionId, variantSku);
        }

        ProductVariant variant = findVariantBySku(variantSku);
        Long variantId = variant.getId();

        List<StockReservation> active = stockReservationsRepository
            .findBySessionIdAndStatus(sessionId, StockReservation.STATUS_RESERVED);

        Optional<StockReservation> existing = active.stream()
            .filter(r -> r.getVariant().getId().equals(variantId))
            .findFirst();

        if (existing.isEmpty()) {
            // No existing reservation — create a new one
            return reserve(sessionId, variantSku, newQuantity);
        }

        int oldQuantity = existing.get().getQuantity();
        int delta = newQuantity - oldQuantity;

        if (delta > 0) {
            // Need more stock — check availability
            if (!tryIncrementReserved(variantId, delta)) {
                int available = variant.getStockCurrent() - variant.getStockReserved();
                throw new BadInputException(
                    "Insufficient stock for variant '" + variantSku + "': need " + delta
                        + " more, only " + Math.max(0, available) + " available");
            }
        } else if (delta < 0) {
            // Releasing some reserved stock back
            stockReservationsRepository.decrementReserved(variantId, Math.abs(delta));
        }

        // Update expiry and persist
        StockReservation reservation = existing.get();
        reservation.setQuantity(newQuantity);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(DEFAULT_TTL_MINUTES));
        StockReservation saved = stockReservationsRepository.saveAndFlush(reservation);

        logger.info("Updated reservation {} for variant {} (session={}): {} -> {}",
            saved.getId(), variantSku, sessionId, oldQuantity, newQuantity);
        return toPojo(saved);
    }

    // ─── Release ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<StockReservationPojo> release(String sessionId) {
        List<StockReservation> active = stockReservationsRepository
            .findBySessionIdAndStatus(sessionId, StockReservation.STATUS_RESERVED);

        for (StockReservation reservation : active) {
            stockReservationsRepository.updateStatus(
                reservation.getId(),
                StockReservation.STATUS_RESERVED,
                StockReservation.STATUS_RELEASED);
            stockReservationsRepository.decrementReserved(
                reservation.getVariant().getId(),
                reservation.getQuantity());

            // Log stock adjustment for reservation release
            ProductVariant refreshedVariant = productVariantsRepository.getById(reservation.getVariant().getId());
            stockAdjustmentService.recordForVariant(
                refreshedVariant,
                StockAdjustment.StockAdjustmentReason.RESERVATION_RELEASED,
                0,
                "Cart session released, reserved stock freed",
                sessionId,
                null,
                null,
                null
            );
        }

        logger.info("Released {} reservation(s) for session {}", active.size(), sessionId);
        return active.stream().map(this::toPojo).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StockReservationPojo releaseItem(String sessionId, String variantSku) {
        ProductVariant variant = productVariantsRepository.findBySku(variantSku).orElse(null);
        if (variant == null) {
            return null;
        }

        List<StockReservation> active = stockReservationsRepository
            .findBySessionIdAndStatus(sessionId, StockReservation.STATUS_RESERVED);

        Optional<StockReservation> existing = active.stream()
            .filter(r -> r.getVariant().getId().equals(variant.getId()))
            .findFirst();

        if (existing.isEmpty()) {
            return null;
        }

        StockReservation reservation = existing.get();
        stockReservationsRepository.updateStatus(
            reservation.getId(),
            StockReservation.STATUS_RESERVED,
            StockReservation.STATUS_RELEASED);
        stockReservationsRepository.decrementReserved(variant.getId(), reservation.getQuantity());

        // Log stock adjustment for item release
        ProductVariant refreshedVariant = productVariantsRepository.getById(variant.getId());
        stockAdjustmentService.recordForVariant(
            refreshedVariant,
            StockAdjustment.StockAdjustmentReason.RESERVATION_RELEASED,
            0,
            "Cart item removed, reserved stock freed",
            sessionId,
            null,
            null,
            null
        );

        reservation.setStatus(StockReservation.STATUS_RELEASED);
        logger.info("Released reservation {} (variant={}, session={})",
            reservation.getId(), variantSku, sessionId);
        return toPojo(reservation);
    }

    // ─── Confirm ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<StockReservationPojo> confirm(String sessionId) {
        List<StockReservation> active = stockReservationsRepository
            .findBySessionIdAndStatus(sessionId, StockReservation.STATUS_RESERVED);

        for (StockReservation reservation : active) {
            Long variantId = reservation.getVariant().getId();
            int quantity = reservation.getQuantity();

            int rows = stockReservationsRepository.confirmDeduct(variantId, quantity);
            if (rows == 0) {
                // CRITICAL FIX (P0.1): Throwing instead of silent-continuing.
                // Before: order was marked PAID but stock was NOT deducted — customer charged
                //          but inventory unchanged. Order stuck at PAID_UNCONFIRMED forever.
                // After:  IllegalStateException propagates → Spring rolls back entire transaction
                //          → customer is NOT charged (payment gateway rolled back).
                // This is consistent with confirmItem() behavior already in markAsPaid().
                throw new IllegalStateException(
                    "Stock deduction failed for variant " + variantId
                        + " (sku=" + reservation.getVariant().getSku() + ")"
                        + ": available stock may be insufficient or reserved by another checkout."
                        + " Order will NOT be marked as paid. Customer should retry.");
            }

            stockReservationsRepository.updateStatus(
                reservation.getId(),
                StockReservation.STATUS_RESERVED,
                StockReservation.STATUS_CONFIRMED);

            // Log stock adjustment — reload variant from DB to capture post-deduct stockCurrent
            // NOTE: confirmDeduct uses a native UPDATE query so JPA entity is stale.
            // getById() with Open Session in View or entity refresh gives us the real value.
            ProductVariant refreshedVariant = productVariantsRepository.findById(variantId).orElse(null);
            if (refreshedVariant != null) {
                stockAdjustmentService.recordForVariant(
                    refreshedVariant,
                    StockAdjustment.StockAdjustmentReason.PAYMENT_CONFIRMED,
                    -quantity,
                    "Payment confirmed, stock deducted",
                    sessionId,
                    null,
                    null,
                    null
                );
            }

            logger.info("Confirmed reservation {} (variant={}, qty={}, session={})",
                reservation.getId(), variantId, quantity, sessionId);
        }

        return active.stream().map(this::toPojo).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StockReservationPojo confirmItem(String sessionId, String variantSku) {
        ProductVariant variant = productVariantsRepository.findBySku(variantSku).orElse(null);
        if (variant == null) {
            return null;
        }

        List<StockReservation> active = stockReservationsRepository
            .findBySessionIdAndStatus(sessionId, StockReservation.STATUS_RESERVED);

        Optional<StockReservation> existing = active.stream()
            .filter(r -> r.getVariant().getId().equals(variant.getId()))
            .findFirst();

        if (existing.isEmpty()) {
            return null;
        }

        StockReservation reservation = existing.get();
        int rows = stockReservationsRepository.confirmDeduct(variant.getId(), reservation.getQuantity());

        // If deduct failed (e.g. insufficient stock), fail hard — do NOT mark as confirmed.
        // The caller (markAsPaid) will propagate this exception and rollback the transaction,
        // preventing the customer from being charged for stock that isn't available.
        if (rows == 0) {
            throw new IllegalStateException(
                "Stock deduction failed for variant '" + variantSku
                    + "': available stock may be insufficient. "
                    + "Order will not be marked as paid.");
        }

        stockReservationsRepository.updateStatus(
            reservation.getId(),
            StockReservation.STATUS_RESERVED,
            StockReservation.STATUS_CONFIRMED);

        // Log stock adjustment
        ProductVariant refreshedVariant = productVariantsRepository.getById(variant.getId());
        stockAdjustmentService.recordForVariant(
            refreshedVariant,
            StockAdjustment.StockAdjustmentReason.PAYMENT_CONFIRMED,
            -reservation.getQuantity(),
            "Payment confirmed (item-level), stock deducted",
            sessionId,
            null,
            null,
            null
        );

        reservation.setStatus(StockReservation.STATUS_CONFIRMED);
        return toPojo(reservation);
    }

    // ─── Restore ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void restoreStockCurrent(
        String sessionId,
        String variantSku,
        int quantity,
        Long orderId,
        StockAdjustment.StockAdjustmentReason reason
    ) {
        ProductVariant variant = productVariantsRepository.findBySku(variantSku).orElse(null);
        if (variant == null) {
            logger.warn("Cannot restore stock — variant not found: {}", variantSku);
            return;
        }

        // restoreStock reverses confirmDeduct:
        //   stockCurrent  += quantity  (return sold units to inventory)
        //   stockReserved -= quantity  (clear the phantom reservation from the original cart session)
        stockReservationsRepository.restoreStock(variant.getId(), quantity);

        // Log audit trail
        stockAdjustmentService.recordForVariant(
            variant,
            reason,
            quantity,
            "Stock restored after " + reason.name().toLowerCase().replace("_", " "),
            sessionId,
            orderId,
            null,
            null
        );

        logger.info("Restored {} units to variant {} (orderId={}, reason={})",
            quantity, variantSku, orderId, reason);
    }

    // ─── Confirm ────────────────────────────────────────────────────────────────

    /**
     * Atomically deducts stockCurrent and decrements stockReserved for the given variant.
     * Called during payment success to permanently deduct reserved stock.
     *
     * @param variantId The variant ID to deduct from.
     * @param quantity  The quantity to deduct.
     * @return Number of rows updated (0 = no-op, e.g. insufficient stock).
     */
    @Transactional
    public int confirmDeduct(Long variantId, int quantity) {
        return stockReservationsRepository.confirmDeduct(variantId, quantity);
    }

    // ─── Query ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<StockReservationPojo> getActiveReservations(String sessionId) {
        return stockReservationsRepository
            .findBySessionIdAndStatusDeep(sessionId, StockReservation.STATUS_RESERVED)
            .stream()
            .map(this::toPojo)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getAvailableStock(String variantSku) {
        return productVariantsRepository.findBySku(variantSku)
            .map(v -> v.getStockCurrent() - v.getStockReserved())
            .map(available -> Math.max(0, available))
            .orElse(null);
    }

    // ─── Maintenance ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public int expireStaleReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<StockReservation> expired = stockReservationsRepository.findExpiredReservations(now);

        for (StockReservation reservation : expired) {
            stockReservationsRepository.updateStatus(
                reservation.getId(),
                StockReservation.STATUS_RESERVED,
                StockReservation.STATUS_RELEASED);
            stockReservationsRepository.decrementReserved(
                reservation.getVariant().getId(),
                reservation.getQuantity());

            // Log stock adjustment for expired reservation
            ProductVariant refreshedVariant = productVariantsRepository.getById(reservation.getVariant().getId());
            stockAdjustmentService.recordForVariant(
                refreshedVariant,
                StockAdjustment.StockAdjustmentReason.RESERVATION_RELEASED,
                0,
                "Reservation expired, reserved stock freed",
                reservation.getSessionId(),
                null,
                null,
                null
            );
        }

        if (!expired.isEmpty()) {
            logger.info("Expired and released {} stale reservation(s)", expired.size());
        }
        return expired.size();
    }
}
