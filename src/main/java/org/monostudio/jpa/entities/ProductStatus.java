package org.monostudio.jpa.entities;

/**
 * Product visibility lifecycle status.
 *
 * <ul>
 *   <li>DRAFT — product is being prepared, not visible to customers</li>
 *   <li>PUBLISHED — product is live and visible to customers</li>
 *   <li>UNLISTED — product was available but is now hidden from customers
 *        (discontinued, out of season, etc.)</li>
 * </ul>
 */
public enum ProductStatus {
    DRAFT,
    PUBLISHED,
    UNLISTED
}
