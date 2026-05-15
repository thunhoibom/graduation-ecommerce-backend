package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QProductCategory;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ProductCategoriesSortSpec {
    private static final QProductCategory BASE_PATH = QProductCategory.productCategory;
    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "displayOrder", BASE_PATH.displayOrder.asc(),
        "displayOrder_desc", BASE_PATH.displayOrder.desc(),
        "name",      BASE_PATH.name.asc(),
        "name_desc", BASE_PATH.name.desc(),
        "code",      BASE_PATH.code.asc(),
        "code_desc", BASE_PATH.code.desc()
    );
}
