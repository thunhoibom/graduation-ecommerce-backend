package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QOrder;
import org.monostudio.jpa.entities.QPerson;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class OrdersSortSpec {
    private static final QOrder BASE_PATH = QOrder.order;
    private static final QPerson CUSTOMER_PATH = BASE_PATH.customer.person;
    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "buyOrder", BASE_PATH.id.asc(),
        "date", BASE_PATH.date.asc(),
        "status", BASE_PATH.status.code.asc(),
        "customer", CUSTOMER_PATH.lastName.asc(),
        "shipper", BASE_PATH.shippingMethod.name.asc(),
        "totalValue", BASE_PATH.totalValue.asc(),
        "netValue", BASE_PATH.netValue.asc(),
        "totalItems", BASE_PATH.totalItems.asc(),
        "transportValue", BASE_PATH.transportValue.asc()
    );
}
