package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.monostudio.api.DataCrudGenericController;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.UserRolePojo;
import org.monostudio.api.models.UserRoleWithPermissionsPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.api.services.UserRolePermissionsReadService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.UserRole;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.UserRolesCrudService;
import org.monostudio.jpa.services.predicates.UserRolesPredicateService;
import org.monostudio.jpa.sortspecs.UserRolesSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/user_roles")
@Tag(name = "Params management")
@PreAuthorize("isAuthenticated()")
public class DataUserRolesController
    extends DataCrudGenericController<UserRolePojo, UserRole> {
    private final UserRolePermissionsReadService userRolePermissionsReadService;

    @Autowired
    public DataUserRolesController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        UserRolesCrudService crudService,
        UserRolesPredicateService predicateService,
        UserRolePermissionsReadService userRolePermissionsReadService
    ) {
        super(paginationService, sortService, crudService, predicateService);
        this.userRolePermissionsReadService = userRolePermissionsReadService;
    }

    @GetMapping("/with_permissions")
    @Operation(summary = "List user roles with their access permissions.")
    @PreAuthorize("hasAuthority('user_roles:read')")
    public List<UserRoleWithPermissionsPojo> readWithPermissions() {
        return userRolePermissionsReadService.listRolesWithPermissions();
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
    public void create(@Valid @RequestBody UserRolePojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace user roles data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('user_roles:update')")
    public void update(@RequestBody UserRolePojo input, @PathVariable Long id)
        throws BadInputException, EntityNotFoundException {
        crudService.update(input, id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove user roles.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('user_roles:delete')")
    public void delete(@PathVariable Long id)
        throws EntityNotFoundException {
        crudService.delete(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partial update of user roles.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('user_roles:update')")
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id
    ) throws BadInputException, EntityNotFoundException {
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("User role not found: " + id));
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return UserRolesSortSpec.orderSpecMap;
    }
}
