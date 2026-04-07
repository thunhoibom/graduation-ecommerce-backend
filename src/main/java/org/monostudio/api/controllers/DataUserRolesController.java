package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.DataCrudGenericController;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.UserRolePojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.UserRole;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.UserRolesCrudService;
import org.monostudio.jpa.services.predicates.UserRolesPredicateService;
import org.monostudio.jpa.sortspecs.UserRolesSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/user_roles")
@Tag(name = "Params management")
@PreAuthorize("isAuthenticated()")
public class DataUserRolesController
    extends DataCrudGenericController<UserRolePojo, UserRole> {

    @Autowired
    public DataUserRolesController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        UserRolesCrudService crudService,
        UserRolesPredicateService predicateService
    ) {
        super(paginationService, sortService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List user roles.")
    @PreAuthorize("hasAuthority('user_roles:read')")
    public DataPagePojo<UserRolePojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Define new user roles.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('user_roles:create')")
    public void create( UserRolePojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @Override
    @PutMapping
    @Operation(summary = "Replace user roles data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('user_roles:update')")
    public void update( UserRolePojo input, @RequestParam Map<String, String> requestParams)
        throws BadInputException, EntityNotFoundException {
        super.update(input, requestParams);
    }

    @Override
    @DeleteMapping
    @Operation(summary = "Remove user roles.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('user_roles:delete')")
    public void delete(@RequestParam Map<String, String> requestParams)
        throws EntityNotFoundException {
        super.delete(requestParams);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return UserRolesSortSpec.orderSpecMap;
    }
}
