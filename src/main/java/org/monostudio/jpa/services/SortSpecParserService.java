package org.monostudio.jpa.services;

import com.querydsl.core.types.OrderSpecifier;
import org.springframework.data.domain.Sort;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * An interface to support parsing of Maps into sort order clauses to be used in queries at the persistence layer.
 */
public interface SortSpecParserService {
    /**
     * Resolves sort order constraints for queries
     *
     * @param orderSpecMap Sort specifiers relevant to one specific entity type
     * @param queryMap     Sort specifiers as requested by an user from a query params map
     * @return A Sort order as parsed from the pair of maps
     */
    Sort parse(@NotNull @NotEmpty Map<String, OrderSpecifier<?>> orderSpecMap, @NotNull Map<String, String> queryMap);
}
