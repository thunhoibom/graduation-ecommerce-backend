package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.*;
import org.monostudio.api.services.AdminDashboardService;
import org.monostudio.config.cache.CacheNames;

import java.time.LocalDate;
import java.util.Collection;

/**
 * REST endpoints for the admin dashboard statistics.
 * All endpoints require authentication with the {@code dashboard:read} authority.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin Dashboard")
@PreAuthorize("isAuthenticated()")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @Autowired
    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // ─── Full Dashboard ─────────────────────────────────────────────────────────

    /**
     * Aggregated dashboard statistics for the given date range.
     * All sub-stats are included (revenue, top products, low stock, order statuses).
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('dashboard:read')")
    @Operation(summary = "Get all dashboard statistics for a date range.")
    @Cacheable(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, key = "@cacheKeyBuilder.dashboard(#from, #to)")
    public AdminDashboardStatsPojo getDashboardStats(
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return dashboardService.getDashboardStats(from, to);
    }

    // ─── Revenue ───────────────────────────────────────────────────────────────

    /**
     * Revenue broken down by day, week, or month.
     *
     * @param groupBy {@code day} (default), {@code week}, or {@code month}.
     */
    @GetMapping("/stats/revenue")
    @PreAuthorize("hasAuthority('dashboard:read')")
    @Operation(summary = "Get revenue statistics grouped by time period.")
    @Cacheable(cacheNames = CacheNames.ADMIN_REVENUE_STATS, key = "@cacheKeyBuilder.dashboardRevenue(#from, #to, #groupBy)")
    public Collection<RevenueStatPojo> getRevenueByPeriod(
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam @Nullable String groupBy
    ) {
        return dashboardService.getRevenueByPeriod(from, to, groupBy);
    }

    // ─── Top Products ───────────────────────────────────────────────────────────

    /**
     * Top selling products by units sold within the date range.
     *
     * @param limit Maximum number of products (default 10).
     */
    @GetMapping("/stats/top-products")
    @PreAuthorize("hasAuthority('dashboard:read')")
    @Operation(summary = "Get top selling products by units sold.")
    @Cacheable(cacheNames = CacheNames.ADMIN_TOP_PRODUCTS, key = "@cacheKeyBuilder.dashboardTopProducts(#from, #to, #limit)")
    public Collection<TopProductPojo> getTopProducts(
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "10") int limit
    ) {
        return dashboardService.getTopProducts(from, to, limit);
    }

    // ─── Low Stock ─────────────────────────────────────────────────────────────

    /**
     * All active product variants currently at or below their critical stock threshold.
     */
    @GetMapping("/stats/low-stock")
    @PreAuthorize("hasAuthority('dashboard:read')")
    @Operation(summary = "Get all variants at or below critical stock level.")
    @Cacheable(cacheNames = CacheNames.ADMIN_LOW_STOCK, key = "'all'")
    public Collection<LowStockAlertPojo> getLowStockAlerts() {
        return dashboardService.getLowStockAlerts();
    }

    /**
     * Low-stock variants with suggested restock quantity based on recent sales velocity.
     */
    @GetMapping("/stats/low-stock/restock-suggestions")
    @PreAuthorize("hasAuthority('dashboard:read')")
    @Operation(summary = "Get low-stock variants with suggested restock quantity.")
    public Collection<RestockSuggestionPojo> getLowStockRestockSuggestions(
        @RequestParam(defaultValue = "30") int lookbackDays,
        @RequestParam(defaultValue = "14") int leadTimeDays
    ) {
        return dashboardService.getLowStockRestockSuggestions(lookbackDays, leadTimeDays);
    }

    // ─── Order Status Breakdown ─────────────────────────────────────────────────

    /**
     * Order counts grouped by status, optionally filtered by date range.
     */
    @GetMapping("/stats/order-statuses")
    @PreAuthorize("hasAuthority('dashboard:read')")
    @Operation(summary = "Get order counts grouped by status.")
    @Cacheable(cacheNames = CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, key = "@cacheKeyBuilder.dashboard(#from, #to)")
    public Collection<OrderStatusCountPojo> getOrderStatusBreakdown(
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return dashboardService.getOrderStatusBreakdown(from, to);
    }
}
