package org.monostudio.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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

    @Autowired
    public StockReservationScheduler(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    /**
     * Runs every 5 minutes (cron: "0 */5 * * * *").
     * Releases all reservations whose expiresAt timestamp is in the past.
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void expireStaleReservations() {
        logger.debug("Running stale reservation expiry job...");
        int released = stockReservationService.expireStaleReservations();
        if (released > 0) {
            logger.info("StockReservationScheduler: released {} stale reservation(s)", released);
        }
    }
}
