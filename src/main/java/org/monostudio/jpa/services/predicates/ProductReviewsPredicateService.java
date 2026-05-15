package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.ProductReview;
import org.monostudio.jpa.entities.QProductReview;
import org.monostudio.jpa.services.PredicateService;

public interface ProductReviewsPredicateService
    extends PredicateService<ProductReview> {
    QProductReview basePath = QProductReview.productReview;
}
