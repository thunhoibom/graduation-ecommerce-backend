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
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.ShippingMethodPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.ShippingMethodsCrudService;
import org.monostudio.jpa.services.predicates.ShippingMethodsPredicateService;
import org.monostudio.jpa.sortspecs.ShippingMethodsSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/shipping-methods")
@Tag(name = "Shipping methods management")
public class DataShippingMethodsController
    extends DataCrudGenericController<ShippingMethodPojo, ShippingMethod> {

    @Autowired
    public DataShippingMethodsController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        ShippingMethodsCrudService crudService,
        ShippingMethodsPredicateService predicateService
    ) {
        super(paginationService, sortService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List shipping methods.")
    public DataPagePojo<ShippingMethodPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Define new shipping methods.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('shipping-methods:create')")
    public void create( ShippingMethodPojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace shipping methods data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('shipping-methods:update')")
    public void update(ShippingMethodPojo input, @PathVariable Long id)
        throws BadInputException, EntityNotFoundException {
        crudService.update(input, id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update parts of shipping methods data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('shipping-methods:update')")
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id
    ) throws BadInputException, EntityNotFoundException {
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("No element was found to update"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove shipping methods.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('shipping-methods:delete')")
    public void delete(@PathVariable Long id)
        throws EntityNotFoundException {
        crudService.delete(id);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return ShippingMethodsSortSpec.ORDER_SPEC_MAP;
    }
}
