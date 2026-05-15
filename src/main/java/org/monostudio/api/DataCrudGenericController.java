package org.monostudio.api;

import com.querydsl.core.types.Predicate;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.services.CrudService;
import org.monostudio.jpa.services.PredicateService;
import org.monostudio.jpa.services.SortSpecParserService;

import jakarta.persistence.EntityNotFoundException;
import java.util.Map;

/**
 * Base class that implements {@link DataCrudController}.<br/>
 * Thus, it implements all four CRUD operations for a type of data, and its method are named as such.
 *
 * @param <M> Its signature model class
 * @param <E> Its signature entity class
 */
public abstract class DataCrudGenericController<M, E>
    extends DataGenericController<M, E>
    implements DataCrudController<M> {

    protected DataCrudGenericController(
        PaginationService paginationService,
        SortSpecParserService sortSpecParserService,
        CrudService<M, E> crudService,
        PredicateService predicateService
    ) {
        super(paginationService, sortSpecParserService, crudService, predicateService);
    }

    @Override
    public void update(M input, Map<String, String> requestParams) throws BadInputException, EntityNotFoundException {
        if (requestParams.isEmpty()) {
            throw new BadInputException("Missing request params");
        }
        Predicate predicate = predicateService.parseMap(requestParams);
        crudService.update(input, predicate)
            .orElseThrow(() -> new EntityNotFoundException("No element was found to update"));
    }

    @Override
    public void partialUpdate(Map<String, Object> input, Map<String, String> requestParams) throws BadInputException, EntityNotFoundException {
        if (requestParams.isEmpty()) {
            throw new BadInputException("Missing request params");
        }
        Predicate predicate = predicateService.parseMap(requestParams);
        crudService.partialUpdate(input, predicate)
            .orElseThrow(() -> new EntityNotFoundException("No element was found to update"));
    }

    @Override
    public void delete(Map<String, String> requestParams) throws EntityNotFoundException {
        if (!requestParams.isEmpty()) {
            Predicate predicate = predicateService.parseMap(requestParams);
            crudService.delete(predicate);
        }
    }
}
