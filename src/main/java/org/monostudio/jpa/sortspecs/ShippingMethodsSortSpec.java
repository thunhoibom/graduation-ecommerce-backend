package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QShippingMethod;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ShippingMethodsSortSpec {
    private static final QShippingMethod BASE_PATH = QShippingMethod.shippingMethod;
    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "name", BASE_PATH.name.asc(),
        "baseFee", BASE_PATH.baseFee.asc(),
        "estimatedDaysMin", BASE_PATH.estimatedDaysMin.asc()
    );
}
