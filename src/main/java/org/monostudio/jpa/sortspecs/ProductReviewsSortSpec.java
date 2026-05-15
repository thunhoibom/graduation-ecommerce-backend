package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QProductReview;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ProductReviewsSortSpec {
    private static final QProductReview BASE_PATH = QProductReview.productReview;

    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "id", BASE_PATH.id.asc(),
        "rating", BASE_PATH.rating.asc(),
        "approved", BASE_PATH.approved.asc(),
        "verifiedPurchase", BASE_PATH.verifiedPurchase.asc(),
        "createdAt", BASE_PATH.createdAt.desc()
    );
}
