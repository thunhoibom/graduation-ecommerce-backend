package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.monostudio.api.models.SupplierPojo;
import org.monostudio.api.models.inventory.SupplierUpsertRequest;
import org.monostudio.api.services.SupplierManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data/inventory/suppliers")
@Tag(name = "Inventory - Suppliers")
@PreAuthorize("isAuthenticated()")
public class DataSuppliersController {

    private final SupplierManagementService supplierManagementService;

    public DataSuppliersController(SupplierManagementService supplierManagementService) {
        this.supplierManagementService = supplierManagementService;
    }

    @GetMapping
    @Operation(summary = "List suppliers")
    @PreAuthorize("hasAnyAuthority('suppliers:read','products:read')")
    public List<SupplierPojo> list(@RequestParam(required = false) String keyword) {
        return supplierManagementService.list(keyword);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier by id")
    @PreAuthorize("hasAnyAuthority('suppliers:read','products:read')")
    public SupplierPojo getById(@PathVariable Long id) {
        return supplierManagementService.getById(id);
    }

    @PostMapping
    @Operation(summary = "Create supplier")
    @PreAuthorize("hasAnyAuthority('suppliers:create','products:update')")
    public SupplierPojo create(@RequestBody SupplierUpsertRequest request) {
        return supplierManagementService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update supplier")
    @PreAuthorize("hasAnyAuthority('suppliers:update','products:update')")
    public SupplierPojo update(@PathVariable Long id, @RequestBody SupplierUpsertRequest request) {
        return supplierManagementService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete supplier")
    @PreAuthorize("hasAnyAuthority('suppliers:delete','products:update')")
    public void delete(@PathVariable Long id) {
        supplierManagementService.softDelete(id);
    }
}
