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
import org.monostudio.api.models.DiscountCodePojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.DiscountCode;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.DiscountCodesCrudService;
import org.monostudio.jpa.services.predicates.DiscountCodesPredicateService;
import org.monostudio.jpa.sortspecs.DiscountCodesSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/discount-codes")
@Tag(name = "Discount codes management")
public class DataDiscountCodesController
    extends DataCrudGenericController<DiscountCodePojo, DiscountCode> {

    @Autowired
    public DataDiscountCodesController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        DiscountCodesCrudService crudService,
        DiscountCodesPredicateService predicateService
    ) {
        super(paginationService, sortService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List discount codes.")
    public DataPagePojo<DiscountCodePojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Create a new discount code.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('discountCodes:create')")
    public void create( DiscountCodePojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @Override
    @PutMapping("/{id}")
    @Operation(summary = "Replace discount code data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('discountCodes:update')")
    public void update(DiscountCodePojo input, @PathVariable Long id)
        throws BadInputException, EntityNotFoundException {
        crudService.update(input, id);
    }

    @Override
    @PatchMapping("/{id}")
    @Operation(summary = "Update parts of discount code data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('discountCodes:update')")
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id
    ) throws BadInputException, EntityNotFoundException {
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("No element was found to update"));
    }

    @Override
    @DeleteMapping("/{id}")
    @Operation(summary = "Remove discount codes.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('discountCodes:delete')")
    public void delete(@PathVariable Long id)
        throws EntityNotFoundException {
        crudService.delete(id);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return DiscountCodesSortSpec.ORDER_SPEC_MAP;
    }
}
