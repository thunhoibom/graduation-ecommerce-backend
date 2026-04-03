package org.monostudio.jpa.repositories;

/**
 * Projection interface for native top-products queries.
 * Must match the column alias names used in the native SQL.
 */
public interface TopProductProjection {
    Long getProductId();
    String getProductName();
    long getUnitsSold();
    long getRevenue();
}
