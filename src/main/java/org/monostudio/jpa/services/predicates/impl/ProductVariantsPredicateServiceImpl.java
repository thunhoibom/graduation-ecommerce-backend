package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.entities.QProductVariant;
import org.monostudio.jpa.services.predicates.ProductVariantsPredicateService;

import java.util.Map;

@Service
public class ProductVariantsPredicateServiceImpl
    implements ProductVariantsPredicateService {

    private static final QProductVariant BASE_PATH = ProductVariantsPredicateService.basePath;
    private final Logger logger = LoggerFactory.getLogger(ProductVariantsPredicateServiceImpl.class);

    @Autowired
    public ProductVariantsPredicateServiceImpl() {
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
                        predicate.and(BASE_PATH.id.eq(Long.valueOf(stringValue)));
                        break;
                    case "sku":
                        predicate.and(BASE_PATH.sku.eq(stringValue));
                        break;
                    case "skuLike":
                        predicate.and(BASE_PATH.sku.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "barcode":
                        predicate.and(BASE_PATH.barcode.eq(stringValue));
                        break;
                    case "variantSize":
                        predicate.and(BASE_PATH.size.eq(stringValue));
                        break;
                    case "color":
                        predicate.and(BASE_PATH.color.eq(stringValue));
                        break;
                    case "active":
                        predicate.and(BASE_PATH.active.eq(Boolean.parseBoolean(stringValue)));
                        break;
                    case "productId":
                        predicate.and(BASE_PATH.product.id.eq(Long.valueOf(stringValue)));
                        break;
                    case "productBarcode":
                        predicate.and(BASE_PATH.product.barcode.eq(stringValue));
                        break;
                    case "page":
                    case "size":
                    case "sort":
                    case "direction":
                        // Ignore pagination and sorting params as they are handled elsewhere
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
