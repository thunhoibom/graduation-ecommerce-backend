package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.DataCrudGenericController;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Salesperson;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.SalespeopleCrudService;
import org.monostudio.jpa.services.predicates.SalespeoplePredicateService;
import org.monostudio.jpa.sortspecs.SalespeopleSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/salespeople")
@Tag(name = "People management")
@PreAuthorize("isAuthenticated()")
public class DataSalespeopleController
    extends DataCrudGenericController<PersonPojo, Salesperson> {

    @Autowired
    public DataSalespeopleController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        SalespeopleCrudService crudService,
        SalespeoplePredicateService predicateService
    ) {
        super(paginationService, sortService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List salespeople.")
    @PreAuthorize("hasAuthority('salespeople:read')")
    public DataPagePojo<PersonPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Register new salespeople.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('salespeople:create')")
    public void create( PersonPojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace salespeople data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('salespeople:update')")
    public void update(PersonPojo input, @PathVariable Long id)
        throws BadInputException, EntityNotFoundException {
        crudService.update(input, id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deregister salespeople.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('salespeople:delete')")
    public void delete(@PathVariable Long id)
        throws EntityNotFoundException {
        crudService.delete(id);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return SalespeopleSortSpec.ORDER_SPEC_MAP;
    }
}
