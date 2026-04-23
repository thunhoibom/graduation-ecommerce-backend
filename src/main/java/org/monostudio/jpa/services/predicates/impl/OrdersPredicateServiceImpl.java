package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.services.predicates.OrdersPredicateService;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Service
public class OrdersPredicateServiceImpl
    implements OrdersPredicateService {
    private final Logger logger = LoggerFactory.getLogger(OrdersPredicateServiceImpl.class);

    @Override
    public Predicate parseMap(Map<String, String> queryParamsMap) {
        BooleanBuilder predicate = new BooleanBuilder();
        for (Map.Entry<String, String> entry : queryParamsMap.entrySet()) {
            String paramName = entry.getKey();
            String stringValue = entry.getValue();
            try {
                switch (paramName) {
                    case "id":
                    case "buyOrder":
                        return basePath.id.eq(Long.valueOf(stringValue));
                    case "date":
                        predicate.and(basePath.date.eq(Instant.parse(stringValue)));
                        break;
                    case "statusName":
                    case "fulfillmentStatus":
                        predicate.and(basePath.fulfillmentStatus.eq(stringValue));
                        break;
                    case "paymentStatus":
                        predicate.and(basePath.paymentStatus.eq(stringValue));
                        break;
                    case "token":
                        predicate.and(basePath.transactionToken.eq(stringValue));
                        break;
                    default:
                        break;
                }
            } catch (NumberFormatException exc) {
                logger.info("Param '{}' couldn't be parsed as number (value: '{}')", paramName, stringValue);
            } catch (DateTimeParseException exc) {
                logger.warn("Param '{}' couldn't be parsed as date (value: '{}')", paramName, stringValue);
            }
        }

        return predicate;
    }
}
