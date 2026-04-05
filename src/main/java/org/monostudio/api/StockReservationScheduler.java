package org.monostudio.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.monostudio.api.services.OrdersProcessService;
import org.monostudio.api.services.StockReservationService;

/**
 * Scheduled jobs for the stock reservation system.
 *
 * <p>Runs every 5 minutes to release any stale (expired) reservations
 * that were never confirmed or released.</p>
 */
@Component
public class StockReservationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(StockReservationScheduler.class);

    private final StockReservationService stockReservationService;
    private final OrdersProcessService ordersProcessService;

    @Autowired
    public StockReservationScheduler(
        StockReservationService stockReservationService,
        OrdersProcessService ordersProcessService
    ) {
        this.stockReservationService = stockReservationService;
        this.ordersProcessService = ordersProcessService;
    }

    /**
     * Releases all reservations whose expiresAt timestamp is in the past.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRateString = "300000")
    public void expireStaleReservations() {
        logger.debug("Running stale reservation expiry job...");
        int released = stockReservationService.expireStaleReservations();
        if (released > 0) {
            logger.info("StockReservationScheduler: released {} stale reservation(s)", released);
        }
    }

    /**
     * Cancels orders that have been stuck in "Payment Started" for more than 30 minutes.
     * Releases their stock reservations and marks them as Payment Cancelled.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRateString = "300000")
    public void expireStalePaymentSessions() {
        logger.debug("Running stale payment session expiry job...");
        int expired = ordersProcessService.expireStalePaymentSessions();
        if (expired > 0) {
            logger.info("StockReservationScheduler: expired {} stale payment session(s)", expired);
        }
    }
}
