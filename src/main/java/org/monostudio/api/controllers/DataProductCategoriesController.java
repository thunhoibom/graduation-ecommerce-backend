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
import org.monostudio.api.models.ProductCategoryPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.ProductCategoriesCrudService;
import org.monostudio.jpa.services.predicates.ProductCategoriesPredicateService;
import org.monostudio.jpa.sortspecs.ProductCategoriesSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/data/product_categories")
@Tag(name = "Product Categories management")
public class DataProductCategoriesController
    extends DataCrudGenericController<ProductCategoryPojo, ProductCategory> {

    @Autowired
    public DataProductCategoriesController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        ProductCategoriesCrudService crudService,
        ProductCategoriesPredicateService predicateService
    ) {
        super(paginationService, sortService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List product categories.")
    public DataPagePojo<ProductCategoryPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        if (allRequestParams==null || allRequestParams.isEmpty()) {
            allRequestParams = Map.of("parentId", "");
        }
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Define new product categories.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('product_categories:create')")
    public void create(@Valid @RequestBody ProductCategoryPojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @Override
    @PutMapping
    @Operation(summary = "Replace product categories data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('product_categories:update')")
    public void update(@Valid @RequestBody ProductCategoryPojo input, @RequestParam Map<String, String> requestParams)
        throws BadInputException, EntityNotFoundException {
        super.update(input, requestParams);
    }

    @Override
    @PatchMapping
    @Operation(summary = "Update parts of product categories data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('product_categories:update')")
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @RequestParam Map<String, String> requestParams
    ) throws BadInputException, EntityNotFoundException {
        super.partialUpdate(input, requestParams);
    }

    @Override
    @DeleteMapping
    @Operation(summary = "Remove product categories.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('product_categories:delete')")
    public void delete(@RequestParam Map<String, String> requestParams)
        throws EntityNotFoundException {
        super.delete(requestParams);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return ProductCategoriesSortSpec.ORDER_SPEC_MAP;
    }
}
