package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.DiscountCode;
import org.monostudio.jpa.entities.QDiscountCode;
import org.monostudio.jpa.services.PredicateService;

public interface DiscountCodesPredicateService
    extends PredicateService<DiscountCode> {
    QDiscountCode basePath = QDiscountCode.discountCode;
}
