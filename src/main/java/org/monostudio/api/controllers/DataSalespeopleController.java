package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
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

    @PatchMapping("/{id}")
    @Operation(summary = "Partial update of salespeople.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('salespeople:update')")
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id
    ) throws BadInputException, EntityNotFoundException {
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("Salesperson not found: " + id));
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return SalespeopleSortSpec.ORDER_SPEC_MAP;
    }
}
