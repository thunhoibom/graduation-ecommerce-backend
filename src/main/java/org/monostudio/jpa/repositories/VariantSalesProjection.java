package org.monostudio.jpa.repositories;

/**
 * Projection interface for variant sales query.
 */
public interface VariantSalesProjection {
    Long getVariantId();
    long getUnitsSold();
}
