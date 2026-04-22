package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.monostudio.api.models.StockCountSessionPojo;
import org.monostudio.api.models.inventory.StockCountLineUpsertRequest;
import org.monostudio.api.models.inventory.StockCountSessionCreateRequest;
import org.monostudio.api.models.inventory.StockCountStatusActionRequest;
import org.monostudio.api.services.StockCountService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data/inventory/stock-counts")
@Tag(name = "Inventory - Stock Counts")
@PreAuthorize("isAuthenticated()")
public class DataStockCountsController {

    private final StockCountService stockCountService;

    public DataStockCountsController(StockCountService stockCountService) {
        this.stockCountService = stockCountService;
    }

    @GetMapping
    @Operation(summary = "List stock count sessions")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:read','products:read')")
    public List<StockCountSessionPojo> list(@RequestParam(required = false) String status) {
        return stockCountService.list(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stock count detail")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:read','products:read')")
    public StockCountSessionPojo getById(@PathVariable Long id) {
        return stockCountService.getById(id);
    }

    @PostMapping
    @Operation(summary = "Create stock count session")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public StockCountSessionPojo create(@RequestBody StockCountSessionCreateRequest request) {
        return stockCountService.create(request);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start stock count")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public StockCountSessionPojo start(@PathVariable Long id, @RequestBody(required = false) StockCountStatusActionRequest request) {
        return stockCountService.start(id, request != null ? request.getActorId() : null, request != null ? request.getNote() : null);
    }

    @PostMapping("/{id}/lines")
    @Operation(summary = "Upsert counted lines")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public StockCountSessionPojo upsertLines(@PathVariable Long id, @RequestBody StockCountLineUpsertRequest request) {
        return stockCountService.updateCountLines(id, request);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete counting")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public StockCountSessionPojo complete(@PathVariable Long id, @RequestBody(required = false) StockCountStatusActionRequest request) {
        return stockCountService.completeCount(id, request != null ? request.getActorId() : null, request != null ? request.getNote() : null);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve stock count")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public StockCountSessionPojo approve(@PathVariable Long id, @RequestBody(required = false) StockCountStatusActionRequest request) {
        return stockCountService.approve(id, request != null ? request.getActorId() : null, request != null ? request.getNote() : null);
    }

    @PostMapping("/{id}/post")
    @Operation(summary = "Post stock count variance")
    @PreAuthorize("hasAnyAuthority('stockAdjustments:create','products:update')")
    public StockCountSessionPojo post(@PathVariable Long id, @RequestBody(required = false) StockCountStatusActionRequest request) {
        return stockCountService.postVariance(id, request != null ? request.getActorId() : null, request != null ? request.getNote() : null);
    }
}
