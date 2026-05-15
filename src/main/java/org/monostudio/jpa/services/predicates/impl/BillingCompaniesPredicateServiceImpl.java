package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.services.predicates.BillingCompaniesPredicateService;

import java.util.Map;

@Service
public class BillingCompaniesPredicateServiceImpl
    implements BillingCompaniesPredicateService {
    private final Logger logger = LoggerFactory.getLogger(BillingCompaniesPredicateServiceImpl.class);

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
                    case "idNumber":
                        return basePath.idNumber.eq(stringValue);
                    case "name":
                        predicate.and(basePath.name.eq(stringValue));
                        break;
                    case "idNumberLike":
                        predicate.and(basePath.idNumber.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "nameLike":
                        predicate.and(basePath.name.likeIgnoreCase("%" + stringValue + "%"));
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
