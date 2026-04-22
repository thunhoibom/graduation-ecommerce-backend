package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.ProductAuditLogPojo;
import org.monostudio.api.services.ProductAuditLogService;
import org.monostudio.jpa.entities.ProductAuditLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/data/product-audit-logs")
@Tag(name = "Product Audit Logs")
@PreAuthorize("hasAuthority('products:read')")
public class DataProductAuditLogsController {

    private final ProductAuditLogService productAuditLogService;

    public DataProductAuditLogsController(ProductAuditLogService productAuditLogService) {
        this.productAuditLogService = productAuditLogService;
    }

    @GetMapping
    @Operation(summary = "Search product/variant audit logs with pagination and filters.")
    public DataPagePojo<ProductAuditLogPojo> search(
        @RequestParam(required = false) ProductAuditLog.EntityType entityType,
        @RequestParam(required = false) Long entityId,
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) Long variantId,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String actor,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to,
        @RequestParam(defaultValue = "0") int pageIndex,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return productAuditLogService.search(
            entityType,
            entityId,
            productId,
            variantId,
            action,
            actor,
            from,
            to,
            pageIndex,
            pageSize
        );
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Get product audit logs by product ID.")
    public DataPagePojo<ProductAuditLogPojo> findByProductId(
        @PathVariable Long productId,
        @RequestParam(defaultValue = "0") int pageIndex,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return productAuditLogService.findByProductId(productId, pageIndex, pageSize);
    }

    @GetMapping("/variants/{variantId}")
    @Operation(summary = "Get product audit logs by variant ID.")
    public DataPagePojo<ProductAuditLogPojo> findByVariantId(
        @PathVariable Long variantId,
        @RequestParam(defaultValue = "0") int pageIndex,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return productAuditLogService.findByVariantId(variantId, pageIndex, pageSize);
    }
}
