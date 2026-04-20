package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ParamsSortSpec {
    private static final org.monostudio.jpa.entities.QParam BASE_PATH = org.monostudio.jpa.entities.QParam.param;
    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "id", BASE_PATH.id.asc(),
        "category", BASE_PATH.category.asc(),
        "name", BASE_PATH.name.asc()
    );
}