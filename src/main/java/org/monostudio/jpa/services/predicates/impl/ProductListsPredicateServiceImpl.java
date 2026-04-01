package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.jpa.services.predicates.ProductListsPredicateService;

import java.util.Map;

@Transactional
@Service
public class ProductListsPredicateServiceImpl
    implements ProductListsPredicateService {
    private final Logger logger = LoggerFactory.getLogger(ProductListsPredicateServiceImpl.class);

    @Override
    public Predicate parseMap(Map<String, String> queryParamsMap) {
        BooleanBuilder predicate = new BooleanBuilder();
        if (queryParamsMap!=null) {
            for (String paramName : queryParamsMap.keySet()) {
                String value = queryParamsMap.get(paramName);
                try {
                    switch (paramName) {
                        case "name":
                        case "listName":
                            return predicate.and(basePath.name.eq(value));
                        case "code":
                        case "listCode":
                            return predicate.and(basePath.code.eq(value));
                        case "nameLike":
                            predicate.and(basePath.name.likeIgnoreCase("%" + value + "%"));
                            break;
                        case "codeLike":
                            predicate.and(basePath.code.likeIgnoreCase("%" + value + "%"));
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
