package org.monostudio.jpa.services;

import org.monostudio.api.models.ProductCategoryPojo;

/**
 * Enriches category Pojos with supplementary data not stored directly
 * on the ProductCategory entity (e.g. product counts from the Product table).
 */
public interface CategoryEnrichmentService {

    /**
     * Populates the productCount field by querying the Product table.
     * Safe to call on any Pojo that has a non-null id.
     */
    void enrichWithProductCount(ProductCategoryPojo pojo);
}
