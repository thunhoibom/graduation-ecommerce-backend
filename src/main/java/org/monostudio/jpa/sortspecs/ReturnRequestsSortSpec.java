package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QReturnRequest;
import org.monostudio.jpa.entities.QOrder;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ReturnRequestsSortSpec {
    private static final QReturnRequest BASE_PATH = QReturnRequest.returnRequest;
    private static final QOrder ORDER_PATH = BASE_PATH.order;
    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "id", BASE_PATH.id.asc(),
        "date", BASE_PATH.date.asc(),
        "status", BASE_PATH.status.stringValue().asc(),
        "orderId", ORDER_PATH.id.asc(),
        "refundAmount", BASE_PATH.refundAmount.asc()
    );
}
