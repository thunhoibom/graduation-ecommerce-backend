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
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.CustomersCrudService;
import org.monostudio.jpa.services.predicates.CustomersPredicateService;
import org.monostudio.jpa.sortspecs.CustomersSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/data/customers")
@Tag(name = "People management")
@PreAuthorize("isAuthenticated()")
public class DataCustomersController
    extends DataCrudGenericController<PersonPojo, Customer> {

    @Autowired
    public DataCustomersController(
        PaginationService paginationService,
        SortSpecParserService sortSpecParserService,
        CustomersCrudService crudService,
        CustomersPredicateService predicateService
    ) {
        super(paginationService, sortSpecParserService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List customers.")
    @PreAuthorize("hasAuthority('customers:read')")
    public DataPagePojo<PersonPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Register new customer.")
    @PreAuthorize("hasAuthority('customers:create')")
    @ResponseStatus(CREATED)
    public void create(@Valid @RequestBody PersonPojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @Override
    @PutMapping
    @Operation(summary = "Replace customers data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('customers:update')")
    public void update(@Valid @RequestBody PersonPojo input, @RequestParam Map<String, String> requestParams)
        throws EntityNotFoundException, BadInputException {
        super.update(input, requestParams);
    }

    @Override
    @DeleteMapping
    @Operation(summary = "Deregister customers.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('customers:delete')")
    public void delete(Map<String, String> requestParams)
        throws EntityNotFoundException {
        super.delete(requestParams);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return CustomersSortSpec.ORDER_SPEC_MAP;
    }
}
