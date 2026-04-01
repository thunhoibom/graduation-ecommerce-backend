package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QProductList;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ProductListsSortSpec {
    private static final QProductList BASE_PATH = QProductList.productList;
    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "name", BASE_PATH.name.asc(),
        "code", BASE_PATH.code.asc(),
        "totalCount", BASE_PATH.items.size().asc()
    );
}
