package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.monostudio.api.models.StockTransferPojo;
import org.monostudio.api.models.inventory.StockTransferCreateRequest;
import org.monostudio.api.models.inventory.StockTransferStatusActionRequest;
import org.monostudio.api.services.StockTransferService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data/inventory/transfers")
@Tag(name = "Inventory - Stock Transfers")
@PreAuthorize("isAuthenticated()")
public class DataStockTransfersController {

    private final StockTransferService stockTransferService;

    public DataStockTransfersController(StockTransferService stockTransferService) {
        this.stockTransferService = stockTransferService;
    }

    @GetMapping
    @Operation(summary = "List stock transfers")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:read','products:read')")
    public List<StockTransferPojo> list(@RequestParam(required = false) String status) {
        return stockTransferService.list(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stock transfer detail")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:read','products:read')")
    public StockTransferPojo getById(@PathVariable Long id) {
        return stockTransferService.getById(id);
    }

    @PostMapping
    @Operation(summary = "Create stock transfer")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public StockTransferPojo create(@RequestBody StockTransferCreateRequest request) {
        return stockTransferService.create(request);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit stock transfer")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public StockTransferPojo submit(@PathVariable Long id, @RequestBody(required = false) StockTransferStatusActionRequest request) {
        return stockTransferService.submit(id, request != null ? request.getActorId() : null, request != null ? request.getNote() : null);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve stock transfer")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public StockTransferPojo approve(@PathVariable Long id, @RequestBody(required = false) StockTransferStatusActionRequest request) {
        return stockTransferService.approve(id, request != null ? request.getActorId() : null, request != null ? request.getNote() : null);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete stock transfer")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public StockTransferPojo complete(@PathVariable Long id, @RequestBody(required = false) StockTransferStatusActionRequest request) {
        return stockTransferService.complete(id, request != null ? request.getActorId() : null, request != null ? request.getNote() : null);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel stock transfer")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public StockTransferPojo cancel(@PathVariable Long id, @RequestBody(required = false) StockTransferStatusActionRequest request) {
        return stockTransferService.cancel(id, request != null ? request.getActorId() : null, request != null ? request.getNote() : null);
    }
}
