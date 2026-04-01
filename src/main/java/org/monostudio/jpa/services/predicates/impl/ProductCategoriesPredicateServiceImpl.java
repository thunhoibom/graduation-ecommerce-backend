package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.monostudio.jpa.entities.QProductCategory;
import org.monostudio.jpa.services.ProductCategoryTreeResolverService;
import org.monostudio.jpa.services.predicates.ProductCategoriesPredicateService;

import java.util.List;
import java.util.Map;

@Service
public class ProductCategoriesPredicateServiceImpl
    implements ProductCategoriesPredicateService {
    private static final QProductCategory parentPath = basePath.parent;
    private final Logger logger = LoggerFactory.getLogger(ProductCategoriesPredicateServiceImpl.class);
    private final ProductCategoryTreeResolverService treeResolver;

    @Autowired
    public ProductCategoriesPredicateServiceImpl(
        ProductCategoryTreeResolverService treeResolver
    ) {
        this.treeResolver = treeResolver;
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
                    case "code":
                        return basePath.code.eq(stringValue);
                    case "name":
                        predicate.and(basePath.name.eq(stringValue));
                        break;
                    case "nameLike":
                        predicate.and(basePath.name.likeIgnoreCase("%" + stringValue + "%"));
                        break;
                    case "parentCode":
                        if (StringUtils.isNotBlank(stringValue)) {
                            predicate.and(parentPath.code.eq(stringValue));
                        }
                        break;
                    case "parentId":
                        if (StringUtils.isBlank(stringValue)) {
                            predicate.and(parentPath.isNull());
                        } else {
                            predicate.and(parentPath.id.eq(Long.valueOf(stringValue)));
                        }
                        break;
                    case "rootId":
                        if (StringUtils.isNotBlank(stringValue)) {
                            List<Long> branchParentIds = treeResolver.getBranchIdsFromRootId(Long.valueOf(stringValue));
                            predicate.and(parentPath.id.in(branchParentIds));
                        }
                        break;
                    case "rootCode":
                        if (StringUtils.isNotBlank(stringValue)) {
                            List<Long> branchParentIds = treeResolver.getBranchIdsFromRootCode(stringValue);
                            if (!CollectionUtils.isEmpty(branchParentIds)) {
                                predicate.and(parentPath.id.in(branchParentIds));
                            }
                        }
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
