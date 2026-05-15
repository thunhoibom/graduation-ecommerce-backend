package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QProduct;
import org.monostudio.jpa.entities.QProductListItem;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ProductListItemsSortSpec {
    private static final QProductListItem BASE_PATH = QProductListItem.productListItem;
    private static final QProduct PRODUCT_PATH = BASE_PATH.product;
    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "name", PRODUCT_PATH.name.asc(),
        "barcode", PRODUCT_PATH.barcode.asc()
    );
}
