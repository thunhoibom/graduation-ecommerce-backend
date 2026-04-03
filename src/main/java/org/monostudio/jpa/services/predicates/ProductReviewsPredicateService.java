package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QProductReview;
import org.monostudio.jpa.services.PredicateService;

public interface ProductReviewsPredicateService
    extends PredicateService {
    QProductReview basePath = QProductReview.productReview;
}
