package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.DataGenericController;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.StockAdjustmentPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.api.services.StockAdjustmentService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.StockAdjustment;
import org.monostudio.jpa.services.SortSpecParserService;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;

/**
 * Read-only controller for stock adjustment audit log.
 * Manual stock adjustments are triggered via POST /adjustments.
 */
@RestController
@RequestMapping("/api/data/stock-adjustments")
@Tag(name = "Stock Adjustments")
@PreAuthorize("isAuthenticated()")
public class DataStockAdjustmentsController
    extends DataGenericController<StockAdjustmentPojo, StockAdjustment> {

    private final StockAdjustmentService stockAdjustmentService;

    @Autowired
    public DataStockAdjustmentsController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        StockAdjustmentService stockAdjustmentService
    ) {
        // Only used for readMany - stock adjustments are append-only
        super(paginationService, sortService, null, null);
        this.stockAdjustmentService = stockAdjustmentService;
    }

    @Override
    @GetMapping
    @Operation(summary = "List stock adjustments with pagination.")
    @PreAuthorize("hasAuthority('stockAdjustments:read')")
    public DataPagePojo<StockAdjustmentPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        int page = 0, size = 20;
        try {
            if (allRequestParams != null) {
                if (allRequestParams.containsKey("page")) {
                    page = Integer.parseInt(allRequestParams.get("page"));
                }
                if (allRequestParams.containsKey("size")) {
                    size = Integer.parseInt(allRequestParams.get("size"));
                }
            }
        } catch (NumberFormatException ignored) {}

        List<StockAdjustmentPojo> adjustments = stockAdjustmentService.getAll(page, size);
        DataPagePojo<StockAdjustmentPojo> result = new DataPagePojo<>();
        result.setItems(adjustments);
        result.setPageIndex(page);
        result.setPageSize(size);
        // totalCount not available from list method — use items only
        return result;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a stock adjustment by id.")
    @PreAuthorize("hasAuthority('stockAdjustments:read')")
    public StockAdjustmentPojo getById(@PathVariable Long id) {
        // Fallback: get first from getAll matching id
        List<StockAdjustmentPojo> all = stockAdjustmentService.getAll(0, 10000);
        return all.stream()
            .filter(a -> a.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Stock adjustment not found: " + id));
    }

    @GetMapping("/variants/{variantId}")
    @Operation(summary = "Get all stock adjustments for a specific variant.")
    @PreAuthorize("hasAuthority('stockAdjustments:read')")
    public List<StockAdjustmentPojo> getByVariant(
        @PathVariable Long variantId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        if (page == 0 && size == 50) {
            return stockAdjustmentService.getByVariant(variantId);
        }
        return stockAdjustmentService.getByVariant(variantId, page, size);
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get all stock adjustments for a specific order.")
    @PreAuthorize("hasAuthority('stockAdjustments:read')")
    public List<StockAdjustmentPojo> getByOrder(@PathVariable Long orderId) {
        return stockAdjustmentService.getByOrder(orderId);
    }

    @PostMapping
    @Operation(summary = "Record a manual stock adjustment (admin).")
    @PreAuthorize("hasAuthority('stockAdjustments:create')")
    public StockAdjustmentPojo recordManualAdjustment(@RequestBody StockAdjustmentRequest request)
        throws BadInputException {

        if (request.getVariantId() == null) {
            throw new BadInputException("variantId is required");
        }
        if (request.getQuantityDelta() == 0) {
            throw new BadInputException("quantityDelta cannot be zero");
        }

        StockAdjustment.StockAdjustmentReason reason = StockAdjustment.StockAdjustmentReason.MANUAL_ADJUSTMENT;

        return stockAdjustmentService.recordFull(
            request.getVariantId(),
            reason,
            request.getQuantityDelta(),
            request.getDescription(),
            null,  // sessionId
            request.getOrderId(),
            null,  // returnRequestId
            request.getPerformedBy()
        );
    }

    @Override
    protected Map<String, com.querydsl.core.types.OrderSpecifier<?>> getOrderSpecMap() {
        // Stock adjustments are append-only — sorting is handled at service layer
        return Map.of();
    }

    // ─── Request body for manual adjustment ──────────────────────────

    public static class StockAdjustmentRequest {
        private Long variantId;
        private int quantityDelta;
        private String description;
        private Long orderId;
        private Long performedBy;

        public Long getVariantId() { return variantId; }
        public void setVariantId(Long variantId) { this.variantId = variantId; }
        public int getQuantityDelta() { return quantityDelta; }
        public void setQuantityDelta(int quantityDelta) { this.quantityDelta = quantityDelta; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public Long getPerformedBy() { return performedBy; }
        public void setPerformedBy(Long performedBy) { this.performedBy = performedBy; }
    }
}