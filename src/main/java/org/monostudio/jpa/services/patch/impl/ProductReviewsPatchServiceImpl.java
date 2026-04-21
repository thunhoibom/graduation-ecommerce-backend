package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ProductReviewPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductReview;
import org.monostudio.jpa.services.patch.ProductReviewsPatchService;

import java.util.Map;

@Service
@NoArgsConstructor
public class ProductReviewsPatchServiceImpl
    implements ProductReviewsPatchService {

    @Override
    public ProductReview patchExistingEntity(Map<String, Object> changes, ProductReview existing) throws BadInputException {
        ProductReview target = new ProductReview(existing);
        target.setProduct(existing.getProduct());
        target.setCustomer(existing.getCustomer());
        target.setImages(existing.getImages());
        target.setReplies(existing.getReplies());

        if (changes.containsKey("rating")) {
            Integer rating = (Integer) changes.get("rating");
            if (rating != null && rating >= 1 && rating <= 5) {
                target.setRating(rating);
            }
        }

        if (changes.containsKey("title")) {
            String title = (String) changes.get("title");
            target.setTitle(title);
        }

        if (changes.containsKey("body")) {
            String body = (String) changes.get("body");
            target.setBody(body);
        }

        if (changes.containsKey("approved")) {
            Boolean approved = (Boolean) changes.get("approved");
            if (approved != null) {
                target.setApproved(approved);
            }
        }

        return target;
    }

    @Override
    public ProductReview patchExistingEntity(ProductReviewPojo changes, ProductReview existing) throws BadInputException {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
