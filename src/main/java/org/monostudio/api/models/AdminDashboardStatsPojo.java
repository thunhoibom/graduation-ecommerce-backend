package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Top-level container for all admin dashboard statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class AdminDashboardStatsPojo {
    /** Total recognized revenue within the requested date range (net of refunds), in cents. */
    private long totalRevenue;
    /** Total order count within the requested date range. */
    private long totalOrders;
    /** Order count broken down by status across the entire system. */
    private Collection<OrderStatusCountPojo> orderStatusBreakdown;
    /** Revenue broken down by time period (day / week / month). */
    private Collection<RevenueStatPojo> revenueByPeriod;
    /** Top N products by units sold within the date range. */
    private Collection<TopProductPojo> topProducts;
    /** Variants currently at or below their critical stock threshold. */
    private Collection<LowStockAlertPojo> lowStockAlerts;
    /** High-level inventory process KPIs for admin operations. */
    private InventoryKpiPojo inventoryKpis;
}
