package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.DataCrudGenericController;
import org.monostudio.api.models.BillingTypePojo;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.BillingType;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.BillingTypesCrudService;
import org.monostudio.jpa.services.predicates.BillingTypesPredicateService;
import org.monostudio.jpa.sortspecs.BillingTypesSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/billing_types")
@Tag(name = "Billing Types management")
public class DataBillingTypesController
    extends DataCrudGenericController<BillingTypePojo, BillingType> {

    @Autowired
    public DataBillingTypesController(
        PaginationService paginationService,
        SortSpecParserService sortSpecParserService,
        BillingTypesCrudService crudService,
        BillingTypesPredicateService predicateService
    ) {
        super(paginationService, sortSpecParserService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List billing types.")
    @PreAuthorize("hasAuthority('billing_types:read')")
    public DataPagePojo<BillingTypePojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @PostMapping
    @Operation(summary = "Create a billing type.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('billing_types:create')")
    public void create(@RequestBody BillingTypePojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a billing type.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('billing_types:update')")
    public void update(@RequestBody BillingTypePojo input, @PathVariable Long id)
        throws BadInputException, EntityNotFoundException {
        crudService.update(input, id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partial update of a billing type.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('billing_types:update')")
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id
    ) throws BadInputException, EntityNotFoundException {
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("Billing type not found: " + id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a billing type.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('billing_types:delete')")
    public void delete(@PathVariable Long id)
        throws EntityNotFoundException {
        crudService.delete(id);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return BillingTypesSortSpec.ORDER_SPEC_MAP;
    }
}
