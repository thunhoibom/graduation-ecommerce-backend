package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.services.predicates.AddressBookPredicateService;

import java.util.Map;

@Service
public class AddressBookPredicateServiceImpl
    implements AddressBookPredicateService {
    private final Logger logger = LoggerFactory.getLogger(AddressBookPredicateServiceImpl.class);

    @Autowired
    public AddressBookPredicateServiceImpl() {
    }

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
                    case "userId":
                        return basePath.user.id.eq(Long.valueOf(stringValue));
                    case "label":
                        return basePath.label.eq(stringValue);
                    case "labelLike":
                        predicate.and(basePath.label.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "defaultShipping":
                        predicate.and(basePath.defaultShipping.eq(Boolean.parseBoolean(stringValue)));
                        break;
                    case "defaultBilling":
                        predicate.and(basePath.defaultBilling.eq(Boolean.parseBoolean(stringValue)));
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
