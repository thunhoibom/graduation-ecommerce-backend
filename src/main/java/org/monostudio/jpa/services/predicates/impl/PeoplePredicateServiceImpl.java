package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.services.predicates.PeoplePredicateService;

import java.util.Map;

@Service
public class PeoplePredicateServiceImpl
    implements PeoplePredicateService {
    private final Logger logger = LoggerFactory.getLogger(PeoplePredicateServiceImpl.class);

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
                        predicate.and(basePath.firstName.eq(stringValue)
                            .or(basePath.lastName.eq(stringValue)));
                        break;
                    case "firstName":
                        predicate.and(basePath.firstName.eq(stringValue));
                        break;
                    case "lastName":
                        predicate.and(basePath.lastName.eq(stringValue));
                        break;
                    case "email":
                        predicate.and(basePath.email.eq(stringValue));
                        break;
                    case "nameLike":
                        predicate.and(basePath.firstName.likeIgnoreCase("%" + stringValue + "%")
                            .or(basePath.lastName.likeIgnoreCase("%" + stringValue + "%")));
                        break;
                    case "firstNameLike":
                        predicate.and(basePath.firstName.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "lastNameLike":
                        predicate.and(basePath.lastName.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "idNumberLike":
                        predicate.and(basePath.idNumber.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "emailLike":
                        predicate.and(basePath.email.likeIgnoreCase("%" + stringValue + "%"));
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
