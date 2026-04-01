package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.jpa.services.predicates.ProductListItemsPredicateService;

import java.util.Map;

@Transactional
@Service
public class ProductListItemsPredicateServiceImpl
    implements ProductListItemsPredicateService {
    private final Logger logger = LoggerFactory.getLogger(ProductListItemsPredicateServiceImpl.class);

    @Override
    public Predicate parseMap(Map<String, String> queryParamsMap) {
        BooleanBuilder predicate = new BooleanBuilder();
        if (queryParamsMap!=null) {
            for (String paramName : queryParamsMap.keySet()) {
                String value = queryParamsMap.get(paramName);
                try {
                    switch (paramName) {
                        case "listName":
                            predicate.and(basePath.list.name.eq(value));
                            break;
                        case "listCode":
                            predicate.and(basePath.list.code.eq(value));
                            break;
                        case "productName":
                            predicate.and(basePath.product.name.eq(value));
                            break;
                        case "productCode":
                            predicate.and(basePath.product.barcode.eq(value));
                            break;
                        case "productNameLike":
                            predicate.and(basePath.product.name.likeIgnoreCase("%" + value + "%"));
                            break;
                        case "productCodeLike":
                            predicate.and(basePath.product.barcode.likeIgnoreCase("%" + value + "%"));
                            break;
                        default:
                            break;
                    }
                } catch (NumberFormatException exc) {
                    logger.warn("Param '{}' couldn't be parsed as number (value: '{}')", paramName, value, exc);
                }
            }
        }
        return predicate;
    }
}
