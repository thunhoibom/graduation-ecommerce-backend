package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.services.predicates.ParamsPredicateService;

import java.util.Map;

@Service
public class ParamsPredicateServiceImpl
    implements ParamsPredicateService {

    @Override
    public Predicate parseMap(Map<String, String> queryParamsMap) {
        BooleanBuilder predicate = new BooleanBuilder();
        for (Map.Entry<String, String> entry : queryParamsMap.entrySet()) {
            String paramName = entry.getKey();
            String stringValue = entry.getValue();
            switch (paramName) {
                case "id":
                    try {
                        predicate.and(basePath.id.eq(Long.valueOf(stringValue)));
                    } catch (NumberFormatException ignored) {}
                    break;
                case "category":
                    predicate.and(basePath.category.eq(stringValue));
                    break;
                case "categoryLike":
                    predicate.and(basePath.category.likeIgnoreCase("%" + stringValue + "%"));
                    break;
                case "name":
                    predicate.and(basePath.name.eq(stringValue));
                    break;
                case "nameLike":
                    predicate.and(basePath.name.likeIgnoreCase("%" + stringValue + "%"));
                    break;
                case "valueLike":
                    predicate.and(basePath.value.likeIgnoreCase("%" + stringValue + "%"));
                    break;
                default:
                    // ignore unknown params
                    break;
            }
        }
        return predicate;
    }
}