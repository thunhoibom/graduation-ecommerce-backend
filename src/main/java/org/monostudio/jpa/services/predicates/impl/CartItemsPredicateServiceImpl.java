package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.services.predicates.CartItemsPredicateService;

import java.util.Map;

@Service
public class CartItemsPredicateServiceImpl
    implements CartItemsPredicateService {

    @Override
    public Predicate parseMap(Map<String, String> queryParamsMap) {
        BooleanBuilder predicate = new BooleanBuilder();
        for (Map.Entry<String, String> entry : queryParamsMap.entrySet()) {
            String paramName = entry.getKey();
            String stringValue = entry.getValue();
            switch (paramName) {
                case "id":
                    predicate.and(basePath.id.eq(Long.valueOf(stringValue)));
                    break;
                case "variantId":
                    predicate.and(basePath.variant.id.eq(Long.valueOf(stringValue)));
                    break;
                case "sessionId":
                    predicate.and(basePath.cartSession.id.eq(Long.valueOf(stringValue)));
                    break;
                default:
                    break;
            }
        }
        return predicate;
    }
}
