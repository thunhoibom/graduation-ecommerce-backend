package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.services.predicates.ReturnRequestsPredicateService;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Service
public class ReturnRequestsPredicateServiceImpl
    implements ReturnRequestsPredicateService {
    private final Logger logger = LoggerFactory.getLogger(ReturnRequestsPredicateServiceImpl.class);

    @Override
    public Predicate parseMap(Map<String, String> queryParamsMap) {
        BooleanBuilder predicate = new BooleanBuilder();
        for (Map.Entry<String, String> entry : queryParamsMap.entrySet()) {
            String paramName = entry.getKey();
            String stringValue = entry.getValue();
            try {
                switch (paramName) {
                    case "id":
                        predicate.and(basePath.id.eq(Long.valueOf(stringValue)));
                        break;
                    case "orderId":
                        predicate.and(basePath.order.id.eq(Long.valueOf(stringValue)));
                        break;
                    case "date":
                        predicate.and(basePath.date.eq(Instant.parse(stringValue)));
                        break;
                    case "status":
                        predicate.and(basePath.status.stringValue().eq(stringValue));
                        break;
                    default:
                        break;
                }
            } catch (NumberFormatException exc) {
                logger.info("Param '{}' couldn't be parsed as number (value: '{}')", paramName, stringValue);
            } catch (DateTimeParseException exc) {
                logger.warn("Param '{}' couldn't be parsed as date (value: '{}')", paramName, stringValue);
            } catch (IllegalArgumentException exc) {
                logger.warn("Param '{}' couldn't be parsed as enum (value: '{}')", paramName, stringValue);
            }
        }
        return predicate;
    }
}
