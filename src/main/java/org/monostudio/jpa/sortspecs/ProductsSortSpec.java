package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QProduct;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ProductsSortSpec {
    private static final QProduct BASE_PATH = QProduct.product;
    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "name", BASE_PATH.name.asc(),
        "barcode", BASE_PATH.barcode.asc(),
        "price", BASE_PATH.price.asc(),
        "category", BASE_PATH.productCategory.name.asc()
    );
}
