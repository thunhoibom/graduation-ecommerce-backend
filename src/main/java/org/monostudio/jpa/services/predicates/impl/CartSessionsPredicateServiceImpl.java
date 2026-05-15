package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.services.predicates.CartSessionsPredicateService;

import java.util.Map;

@Service
public class CartSessionsPredicateServiceImpl
    implements CartSessionsPredicateService {

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
                case "token":
                    predicate.and(basePath.token.eq(stringValue));
                    break;
                default:
                    break;
            }
        }
        return predicate;
    }
}
