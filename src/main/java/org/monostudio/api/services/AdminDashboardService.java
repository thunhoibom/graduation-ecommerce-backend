package org.monostudio.api.services;

import org.monostudio.api.models.*;

import java.time.LocalDate;
import java.util.Collection;

/**
 * Aggregates statistics for the admin dashboard: revenue, order counts,
 * top products, and low-stock alerts.
 */
public interface AdminDashboardService {

    // ─── Full Dashboard ─────────────────────────────────────────────────────────

    /**
     * All dashboard statistics within the given date range.
     *
     * @param from Start of the date range (inclusive). Pass null for all-time.
     * @param to   End of the date range (inclusive). Pass null for now.
     * @return Aggregated dashboard stats.
     */
    AdminDashboardStatsPojo getDashboardStats(LocalDate from, LocalDate to);

    // ─── Revenue ───────────────────────────────────────────────────────────────

    /**
     * Revenue broken down by time period (day / week / month).
     *
     * @param from    Start date (inclusive). Pass null for all-time.
     * @param to      End date (inclusive). Pass null for now.
     * @param groupBy {@code day}, {@code week}, or {@code month}.
     * @return Revenue stats per period, ordered chronologically.
     */
    Collection<RevenueStatPojo> getRevenueByPeriod(LocalDate from, LocalDate to, String groupBy);

    // ─── Top Products ──────────────────────────────────────────────────────────

    /**
     * Top selling products by units sold within the date range.
     *
     * @param from  Start date (inclusive). Pass null for all-time.
     * @param to    End date (inclusive). Pass null for now.
     * @param limit Maximum number of products to return (default 10).
     * @return Top products sorted by units sold descending.
     */
    Collection<TopProductPojo> getTopProducts(LocalDate from, LocalDate to, int limit);

    // ─── Low Stock ─────────────────────────────────────────────────────────────

    /**
     * All active product variants currently at or below their critical stock threshold.
     *
     * @return Low-stock variants sorted by current stock ascending.
     */
    Collection<LowStockAlertPojo> getLowStockAlerts();

    // ─── Order Status Breakdown ────────────────────────────────────────────────

    /**
     * Count of orders grouped by status, optionally filtered by date range.
     *
     * @param from Start date (inclusive). Pass null for all-time.
     * @param to   End date (inclusive). Pass null for all-time.
     * @return Order counts per status.
     */
    Collection<OrderStatusCountPojo> getOrderStatusBreakdown(LocalDate from, LocalDate to);
}
