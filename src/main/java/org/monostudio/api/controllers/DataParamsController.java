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
import org.monostudio.api.models.ParamPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Param;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.ParamsCrudService;
import org.monostudio.jpa.services.predicates.ParamsPredicateService;
import org.monostudio.jpa.sortspecs.ParamsSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/params")
@Tag(name = "System Parameters")
@PreAuthorize("isAuthenticated()")
public class DataParamsController
    extends DataCrudGenericController<ParamPojo, Param> {

    @Autowired
    public DataParamsController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        ParamsCrudService crudService,
        ParamsPredicateService predicateService
    ) {
        super(paginationService, sortService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List system parameters.")
    @PreAuthorize("hasAuthority('params:read')")
    public DataPagePojo<ParamPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Create a new system parameter.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('params:create')")
    public void create(@RequestBody ParamPojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a system parameter.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('params:update')")
    public void update(@RequestBody ParamPojo input, @PathVariable Long id)
        throws BadInputException, EntityNotFoundException {
        crudService.update(input, id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update parts of a system parameter.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('params:update')")
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id
    ) throws BadInputException, EntityNotFoundException {
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("No element was found to update"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a system parameter.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('params:delete')")
    public void delete(@PathVariable Long id)
        throws EntityNotFoundException {
        crudService.delete(id);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return ParamsSortSpec.ORDER_SPEC_MAP;
    }
}