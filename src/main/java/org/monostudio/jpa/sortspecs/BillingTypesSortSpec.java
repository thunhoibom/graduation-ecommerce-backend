package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QBillingType;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class BillingTypesSortSpec {
    private static final QBillingType BASE_PATH = QBillingType.billingType;
    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "name", BASE_PATH.name.asc()
    );
}
