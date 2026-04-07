package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.DataCrudGenericController;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.ProductListPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductList;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.ProductListCrudService;
import org.monostudio.jpa.services.predicates.ProductListsPredicateService;
import org.monostudio.jpa.sortspecs.ProductListsSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/product_lists")
@Tag(name = "Product Lists management")
public class DataProductListsController
    extends DataCrudGenericController<ProductListPojo, ProductList> {

    @Autowired
    public DataProductListsController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        ProductListCrudService crudService,
        ProductListsPredicateService predicateService
    ) {
        super(paginationService, sortService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "View product lists.")
    public DataPagePojo<ProductListPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Define new product lists.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('product_lists:create')")
    public void create( ProductListPojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @Override
    @PutMapping
    @Operation(summary = "Replace product lists data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('product_lists:update')")
    public void update( ProductListPojo input, @RequestParam Map<String, String> requestParams)
        throws BadInputException, EntityNotFoundException {
        super.update(input, requestParams);
    }

    @Override
    @PatchMapping
    @Operation(summary = "Update parts of product lists data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('product_lists:update')")
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @RequestParam Map<String, String> requestParams
    ) throws BadInputException, EntityNotFoundException {
        super.partialUpdate(input, requestParams);
    }

    @Override
    @DeleteMapping
    @Operation(summary = "Remove product lists.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('product_lists:delete')")
    public void delete(@RequestParam Map<String, String> requestParams)
        throws EntityNotFoundException {
        super.delete(requestParams);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return ProductListsSortSpec.ORDER_SPEC_MAP;
    }
}
