package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.services.ProductCategoryTreeResolverService;
import org.monostudio.jpa.services.predicates.ProductsPredicateService;

import java.util.List;
import java.util.Map;

@Service
public class ProductsPredicateServiceImpl
    implements ProductsPredicateService {
    private final Logger logger = LoggerFactory.getLogger(ProductsPredicateServiceImpl.class);
    private final ProductCategoryTreeResolverService categoryTreeResolver;

    @Autowired
    public ProductsPredicateServiceImpl(
        ProductCategoryTreeResolverService categoryTreeResolver
    ) {
        this.categoryTreeResolver = categoryTreeResolver;
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
                    case "barcode":
                        return basePath.barcode.eq(stringValue);
                    case "name":
                        return basePath.name.eq(stringValue);
                    case "barcodeLike":
                        predicate.and(basePath.barcode.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "nameLike":
                        predicate.and(basePath.name.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "categoryCode":
                        List<Long> branchIds = categoryTreeResolver.getBranchIdsFromRootCode(stringValue);
                        predicate.and(basePath.productCategory.code.eq(stringValue)
                            .or(basePath.productCategory.id.in(branchIds)));
                        break;
                    case "categoryCodeLike":
                        predicate.and(basePath.productCategory.code.likeIgnoreCase("%" + stringValue + "%"));
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
