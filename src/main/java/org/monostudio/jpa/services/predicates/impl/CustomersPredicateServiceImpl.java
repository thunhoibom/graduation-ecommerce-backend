package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.entities.QPerson;
import org.monostudio.jpa.services.predicates.CustomersPredicateService;

import java.util.Map;

@Service
public class CustomersPredicateServiceImpl
    implements CustomersPredicateService {
    public static final QPerson personPath = basePath.person;
    private final Logger logger = LoggerFactory.getLogger(CustomersPredicateServiceImpl.class);

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
                        return personPath.idNumber.eq(stringValue);
                    case "name":
                        predicate.and(personPath.firstName.eq(stringValue)
                            .or(personPath.lastName.eq(stringValue)));
                        break;
                    case "firstName":
                        predicate.and(personPath.firstName.eq(stringValue));
                        break;
                    case "lastName":
                        predicate.and(personPath.lastName.eq(stringValue));
                        break;
                    case "email":
                        predicate.and(personPath.email.eq(stringValue));
                        break;
                    case "nameLike":
                        predicate.and(personPath.firstName.likeIgnoreCase("%" + stringValue + "%")
                            .or(personPath.lastName.likeIgnoreCase("%" + stringValue + "%")));
                        break;
                    case "firstNameLike":
                        predicate.and(personPath.firstName.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "lastNameLike":
                        predicate.and(personPath.lastName.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "idNumberLike":
                        predicate.and(personPath.idNumber.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "emailLike":
                        predicate.and(personPath.email.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "phoneLike": {
                        String trimmed = stringValue.trim();
                        if (!trimmed.isEmpty()) {
                            String like = "%" + trimmed + "%";
                            BooleanExpression phoneMatch = personPath.phone1.likeIgnoreCase(like)
                                .or(personPath.phone2.likeIgnoreCase(like));
                            predicate.and(phoneMatch);
                        }
                        break;
                    }
                    case "q": {
                        String q = stringValue.trim();
                        if (!q.isEmpty()) {
                            String like = "%" + q + "%";
                            predicate.and(
                                personPath.firstName.likeIgnoreCase(like)
                                    .or(personPath.lastName.likeIgnoreCase(like))
                                    .or(personPath.email.likeIgnoreCase(like))
                                    .or(personPath.phone1.likeIgnoreCase(like))
                                    .or(personPath.phone2.likeIgnoreCase(like))
                                    .or(personPath.idNumber.likeIgnoreCase(like))
                            );
                        }
                        break;
                    }
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
