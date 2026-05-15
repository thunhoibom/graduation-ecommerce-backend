package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.ProductReviewPojo;
import org.monostudio.jpa.entities.ProductReview;
import org.monostudio.jpa.services.PatchService;

public interface ProductReviewsPatchService
    extends PatchService<ProductReviewPojo, ProductReview> {
}
