package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.monostudio.api.DataCrudGenericController;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.UserPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.UsersCrudService;
import org.monostudio.jpa.services.predicates.UsersPredicateService;
import org.monostudio.jpa.sortspecs.UsersSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/users")
@Tag(name = "Users management")
@PreAuthorize("isAuthenticated()")
public class DataUsersController
    extends DataCrudGenericController<UserPojo, User> {

    @Autowired
    public DataUsersController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        UsersCrudService crudService,
        UsersPredicateService predicateService
    ) {
        super(paginationService, sortService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List users.")
    @PreAuthorize("hasAuthority('users:read')")
    public DataPagePojo<UserPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Register new users.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('users:create')")
    public void create(@Valid @RequestBody UserPojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace users data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('users:update')")
    public void update(@RequestBody UserPojo input, @PathVariable Long id)
        throws BadInputException, EntityNotFoundException {
        crudService.update(input, id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove users.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('users:delete')")
    public void delete(@PathVariable Long id)
        throws EntityNotFoundException {
        crudService.delete(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partial update of users.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('users:update')")
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id
    ) throws BadInputException, EntityNotFoundException {
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return UsersSortSpec.ORDER_SPEC_MAP;
    }
}
