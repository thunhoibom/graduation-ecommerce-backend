package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.services.predicates.ProductReviewsPredicateService;

import java.util.Map;

@Service
public class ProductReviewsPredicateServiceImpl
    implements ProductReviewsPredicateService {
    private final Logger logger = LoggerFactory.getLogger(ProductReviewsPredicateServiceImpl.class);

    @Autowired
    public ProductReviewsPredicateServiceImpl() {
    }

    @Override
    public Predicate parseMap(Map<String, String> queryParamsMap) {
        BooleanBuilder predicate = new BooleanBuilder();
        for (Map.Entry<String, String> entry : queryParamsMap.entrySet()) {
            String paramName = entry.getKey();
            String stringValue = entry.getValue();
            try {
                switch (paramName) {
                    case "id":
                        return basePath.id.eq(Long.valueOf(stringValue));
                    case "productId":
                        return basePath.product.id.eq(Long.valueOf(stringValue));
                    case "productBarcode":
                        return basePath.product.barcode.eq(stringValue);
                    case "customerId":
                        return basePath.customer.id.eq(Long.valueOf(stringValue));
                    case "rating":
                        return basePath.rating.eq(Integer.valueOf(stringValue));
                    case "approved":
                        predicate.and(basePath.approved.eq(Boolean.parseBoolean(stringValue)));
                        break;
                    case "verifiedPurchase":
                        predicate.and(basePath.verifiedPurchase.eq(Boolean.parseBoolean(stringValue)));
                        break;
                    default:
                        break;
                }
            } catch (NumberFormatException exc) {
                logger.info("Param '{}' couldn't be parsed as number (value: '{}')", paramName, stringValue);
            }
        }
        return predicate;
    }
}
