package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.entities.ProductStatus;
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
                        predicate.and(basePath.id.eq(Long.valueOf(stringValue)));
                        break;
                    case "barcode":
                        predicate.and(basePath.barcode.eq(stringValue));
                        break;
                    case "name":
                        predicate.and(basePath.name.eq(stringValue));
                        break;
                    case "barcodeLike":
                        predicate.and(basePath.barcode.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "nameLike":
                        predicate.and(basePath.name.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "category":
                    case "categoryCode":
                        List<Long> branchIds = categoryTreeResolver.getBranchIdsFromRootCode(stringValue);
                        logger.info("[JPA DEBUG] Search code: '{}', IDs found: {}", stringValue, branchIds);
                        
                        predicate.and(basePath.productCategory.code.eq(stringValue)
                            .or(basePath.productCategory.id.in(branchIds)));
                        break;
                    case "categoryCodeLike":
                        predicate.and(basePath.productCategory.code.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "status":
                        try {
                            ProductStatus status = ProductStatus.valueOf(stringValue.toUpperCase());
                            predicate.and(basePath.status.eq(status));
                        } catch (IllegalArgumentException exc) {
                            logger.info("Invalid status value: '{}' — ignoring filter", stringValue);
                        }
                        break;
                    case "minPrice":
                        predicate.and(basePath.price.goe(Integer.parseInt(stringValue)));
                        break;
                    case "maxPrice":
                        predicate.and(basePath.price.loe(Integer.parseInt(stringValue)));
                        break;
                    case "inStock":
                        if (Boolean.parseBoolean(stringValue)) {
                            predicate.and(basePath.stockCurrent.gt(0));
                        }
                        break;
                    case "query":
                        predicate.and(basePath.name.containsIgnoreCase(stringValue)
                            .or(basePath.description.containsIgnoreCase(stringValue))
                            .or(basePath.productCategory.name.containsIgnoreCase(stringValue)));
                        break;
                    case "color":
                        predicate.and(basePath.variants.any().color.equalsIgnoreCase(stringValue));
                        break;
                    case "size":
                        predicate.and(basePath.variants.any().size.equalsIgnoreCase(stringValue));
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
