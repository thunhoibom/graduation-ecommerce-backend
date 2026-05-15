package org.monostudio.jpa.entities;

/**
 * Scope of a promotion rule.
 * PRODUCT/CATEGORY affect catalog price rendering.
 * CART/SHIPPING apply at checkout pricing stage.
 */
public enum PromotionScope {
    PRODUCT,
    CATEGORY,
    CART,
    SHIPPING
}
