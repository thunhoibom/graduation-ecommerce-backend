package org.monostudio.jpa.services.impl;

import com.querydsl.core.types.OrderSpecifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.QSort;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.services.SortSpecParserService;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Service
public class SortSpecParserServiceImpl
    implements SortSpecParserService {
    static final String SORT_PROPERTY_QUERY_MAP_KEY = "sortBy";
    static final String SORT_DIRECTION_QUERY_MAP_KEY = "order";

    @Override
    public Sort parse(
        @NotNull @NotEmpty Map<String, OrderSpecifier<?>> orderSpecMap,
        @NotNull Map<String, String> queryMap
    ) {
        if (!queryMap.containsKey(SORT_PROPERTY_QUERY_MAP_KEY)) {
            return Sort.unsorted();
        }
        String propertyName = queryMap.get(SORT_PROPERTY_QUERY_MAP_KEY);
        OrderSpecifier<?> orderSpecifier = orderSpecMap.get(propertyName);
        Sort sortBy = QSort.by(orderSpecifier);
        switch (queryMap.get(SORT_DIRECTION_QUERY_MAP_KEY)) {
            case "asc":
                return sortBy.ascending();
            case "desc":
                return sortBy.descending();
            default:
                return sortBy;
        }
    }
}
