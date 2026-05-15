package org.monostudio.api.services;

import org.monostudio.jpa.entities.Product;

import java.time.LocalDateTime;

public interface ProductPricingSnapshotService {

    ProductPricingSnapshot calculate(Product product);

    record ProductPricingSnapshot(
        int originalPrice,
        int currentPrice,
        int discountPercent,
        boolean hasDiscount,
        LocalDateTime activeFrom,
        LocalDateTime activeUntil
    ) {
    }
}
