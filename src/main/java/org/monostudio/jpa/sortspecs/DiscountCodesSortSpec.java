package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QDiscountCode;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class DiscountCodesSortSpec {
    private static final QDiscountCode BASE_PATH = QDiscountCode.discountCode;

    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "code",        BASE_PATH.code.asc(),
        "type",        BASE_PATH.type.asc(),
        "value",       BASE_PATH.value.asc(),
        "active",      BASE_PATH.active.asc(),
        "maxUses",    BASE_PATH.maxUses.asc(),
        "useCount",   BASE_PATH.useCount.desc(),
        "validUntil", BASE_PATH.validUntil.asc(),
        "createdAt",   BASE_PATH.createdAt.desc()
    );
}
