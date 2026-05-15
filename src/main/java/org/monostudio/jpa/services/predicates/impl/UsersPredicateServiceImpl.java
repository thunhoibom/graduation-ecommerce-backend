package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.services.predicates.UsersPredicateService;

import java.util.Map;

@Service
public class UsersPredicateServiceImpl
    implements UsersPredicateService {
    private final Logger logger = LoggerFactory.getLogger(UsersPredicateServiceImpl.class);

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
                    case "name":
                        return basePath.name.eq(stringValue);
                    case "email":
                        predicate.and(basePath.person.email.eq(stringValue));
                        break;
                    case "nameLike":
                        predicate.and(basePath.name.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "emailLike":
                        predicate.and(basePath.person.email.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "role":
                        predicate.and(basePath.userRole.name.eq(stringValue));
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
