package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.monostudio.api.models.GoodsReceiptPojo;
import org.monostudio.api.models.PurchaseOrderPojo;
import org.monostudio.api.models.inventory.GoodsReceiptCreateRequest;
import org.monostudio.api.models.inventory.PurchaseOrderCreateRequest;
import org.monostudio.api.models.inventory.PurchaseOrderStatusActionRequest;
import org.monostudio.api.services.PurchaseOrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data/inventory/purchase-orders")
@Tag(name = "Inventory - Purchase Orders")
@PreAuthorize("isAuthenticated()")
public class DataPurchaseOrdersController {

    private final PurchaseOrderService purchaseOrderService;

    public DataPurchaseOrdersController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping
    @Operation(summary = "List purchase orders")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:read','products:read')")
    public List<PurchaseOrderPojo> list(@RequestParam(required = false) String status) {
        return purchaseOrderService.list(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase order detail")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:read','products:read')")
    public PurchaseOrderPojo getById(@PathVariable Long id) {
        return purchaseOrderService.getById(id);
    }

    @PostMapping
    @Operation(summary = "Create purchase order")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public PurchaseOrderPojo create(@RequestBody PurchaseOrderCreateRequest request) {
        return purchaseOrderService.create(request);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit purchase order")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public PurchaseOrderPojo submit(@PathVariable Long id, @RequestBody(required = false) PurchaseOrderStatusActionRequest request) {
        return purchaseOrderService.submit(id, request != null ? request.getActorId() : null, request != null ? request.getNote() : null);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve purchase order")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public PurchaseOrderPojo approve(@PathVariable Long id, @RequestBody(required = false) PurchaseOrderStatusActionRequest request) {
        return purchaseOrderService.approve(id, request != null ? request.getActorId() : null, request != null ? request.getNote() : null);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel purchase order")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public PurchaseOrderPojo cancel(@PathVariable Long id, @RequestBody(required = false) PurchaseOrderStatusActionRequest request) {
        return purchaseOrderService.cancel(id, request != null ? request.getActorId() : null, request != null ? request.getNote() : null);
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "Receive goods for purchase order")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public GoodsReceiptPojo receive(@PathVariable Long id, @RequestBody GoodsReceiptCreateRequest request) {
        return purchaseOrderService.receive(id, request);
    }
}
