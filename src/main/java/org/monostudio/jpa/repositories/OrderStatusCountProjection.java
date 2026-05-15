package org.monostudio.jpa.repositories;

import java.time.LocalDate;

/**
 * Projection interface for native revenue-by-period queries.
 * Must match the column alias names used in the native SQL.
 */
public interface OrderStatusCountProjection {
    String getStatus();
    long getCount();
}
