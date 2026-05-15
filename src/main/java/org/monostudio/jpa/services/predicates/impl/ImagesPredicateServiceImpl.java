package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.services.predicates.ImagesPredicateService;

import java.util.Map;

@Service
public class ImagesPredicateServiceImpl
    implements ImagesPredicateService {
    private final Logger logger = LoggerFactory.getLogger(ImagesPredicateServiceImpl.class);

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
                    case "code":
                        return basePath.code.eq(stringValue);
                    case "filename":
                        return basePath.filename.eq(stringValue);
                    case "codeLike":
                        predicate.and(basePath.code.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "filenameLike":
                        predicate.and(basePath.filename.likeIgnoreCase("%" + stringValue + "%"));
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
