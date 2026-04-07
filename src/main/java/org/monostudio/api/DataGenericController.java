package org.monostudio.api;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.jpa.services.CrudService;
import org.monostudio.jpa.services.PredicateService;
import org.monostudio.jpa.services.SortSpecParserService;

import java.util.Map;

/**
 * Base class that implements {@link org.monostudio.api.DataController}.<br/>
 * Thus, it can only read data of a given type.
 *
 * @param <M> The model class
 * @param <E> The entity class
 */
@RequestMapping("/api")
public abstract class DataGenericController<M, E>
    implements DataController<M> {
    protected final PaginationService paginationService;
    protected final SortSpecParserService sortService;
    protected final CrudService<M, E> crudService;
    protected final PredicateService predicateService;

    protected abstract Map<String, OrderSpecifier<?>> getOrderSpecMap();

    protected DataGenericController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        CrudService<M, E> crudService,
        PredicateService predicateService
    ) {
        this.paginationService = paginationService;
        this.sortService = sortService;
        this.crudService = crudService;
        this.predicateService = predicateService;
    }

    /**
     * Retrieve a page of items with a fixed size and offset index.
     * An optional Map (like query string parameters) can be provided for filtering criteria
     *
     * @param requestParams May contain filtering conditions and/or page size & page index parameters.
     * @return A paged collection of Pojos.
     */
    @Override
    public DataPagePojo<M> readMany(@Nullable Map<String, String> requestParams) {
        int pageIndex = paginationService.determineRequestedPageIndex(requestParams);
        int pageSize = paginationService.determineRequestedPageSize(requestParams);

        Sort order = null;
        if (requestParams!=null && !requestParams.isEmpty()) {
            order = sortService.parse(getOrderSpecMap(), requestParams);
        }

        Predicate filters = null;
        if (requestParams!=null && !requestParams.isEmpty()) {
            filters = predicateService.parseMap(requestParams);
        }

        return crudService.readMany(pageIndex, pageSize, order, filters);
    }
}
