package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QProductVariant;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ProductVariantsSortSpec {
    private static final QProductVariant BASE_PATH = QProductVariant.productVariant;

    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "sku", BASE_PATH.sku.asc(),
        "size", BASE_PATH.size.asc(),
        "color", BASE_PATH.color.asc(),
        "priceModifier", BASE_PATH.priceModifier.asc(),
        "stockCurrent", BASE_PATH.stockCurrent.asc(),
        "active", BASE_PATH.active.asc(),
        "createdAt", BASE_PATH.createdAt.desc()
    );
}
