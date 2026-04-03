package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.services.predicates.DiscountCodesPredicateService;

import java.util.Map;

@Service
public class DiscountCodesPredicateServiceImpl
    implements DiscountCodesPredicateService {

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
                case "code":
                    predicate.and(basePath.code.equalsIgnoreCase(stringValue));
                    break;
                case "codeLike":
                    predicate.and(basePath.code.likeIgnoreCase("%" + stringValue + "%"));
                    break;
                case "type":
                    predicate.and(basePath.type.eq(stringValue));
                    break;
                case "active":
                    predicate.and(basePath.active.eq(Boolean.parseBoolean(stringValue)));
                    break;
                default:
                    break;
            }
        }
        return predicate;
    }
}
