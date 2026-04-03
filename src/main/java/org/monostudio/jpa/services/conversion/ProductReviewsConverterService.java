package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.ProductReviewPojo;
import org.monostudio.jpa.entities.ProductReview;
import org.monostudio.jpa.services.ConverterService;

public interface ProductReviewsConverterService
    extends ConverterService<ProductReviewPojo, ProductReview> {
}
