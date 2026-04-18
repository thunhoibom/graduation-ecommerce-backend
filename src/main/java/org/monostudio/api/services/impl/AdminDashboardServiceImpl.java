package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.*;
import org.monostudio.api.services.AdminDashboardService;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.repositories.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static org.monostudio.config.Constants.ORDER_STATUS_PAID_CONFIRMED;
import static org.monostudio.config.Constants.ORDER_STATUS_COMPLETED;

@Transactional(readOnly = true)
@Service
public class AdminDashboardServiceImpl
    implements AdminDashboardService {

    private final OrdersRepository ordersRepository;
    private final ProductVariantsRepository productVariantsRepository;

    @Autowired
    public AdminDashboardServiceImpl(
        OrdersRepository ordersRepository,
        ProductVariantsRepository productVariantsRepository
    ) {
        this.ordersRepository = ordersRepository;
        this.productVariantsRepository = productVariantsRepository;
    }

    // ─── Full Dashboard ─────────────────────────────────────────────────────────

    @Override
    public AdminDashboardStatsPojo getDashboardStats(LocalDate from, LocalDate to) {
        Instant fromInstant = toInstant(from, true);
        Instant toInstant = toInstant(to, false);

        // Revenue from paid/confirmed + completed orders
        long paidConfirmedRevenue = ordersRepository.sumRevenueByStatusAndDateBetween(
            ORDER_STATUS_PAID_CONFIRMED, fromInstant, toInstant);
        long completedRevenue = ordersRepository.sumRevenueByStatusAndDateBetween(
            ORDER_STATUS_COMPLETED, fromInstant, toInstant);
        long totalRevenue = paidConfirmedRevenue + completedRevenue;

        long totalOrders = ordersRepository.countByDateBetween(fromInstant, toInstant);

        Collection<RevenueStatPojo> revenueByPeriod = getRevenueByPeriod(from, to, "day");
        Collection<TopProductPojo> topProducts = getTopProducts(from, to, 10);
        Collection<OrderStatusCountPojo> statusBreakdown = getOrderStatusBreakdown(from, to);
        Collection<LowStockAlertPojo> lowStockAlerts = getLowStockAlerts();

        return AdminDashboardStatsPojo.builder()
            .totalRevenue(totalRevenue)
            .totalOrders(totalOrders)
            .revenueByPeriod(revenueByPeriod)
            .topProducts(topProducts)
            .orderStatusBreakdown(statusBreakdown)
            .lowStockAlerts(lowStockAlerts)
            .build();
    }

    // ─── Revenue ───────────────────────────────────────────────────────────────

    @Override
    public Collection<RevenueStatPojo> getRevenueByPeriod(LocalDate from, LocalDate to, String groupBy) {
        Instant fromInstant = toInstant(from, true);
        Instant toInstant = toInstant(to, false);

        List<RevenueByPeriodProjection> raw;
        switch (normalizeGroupBy(groupBy)) {
            case "week"  -> raw = ordersRepository.revenueByWeek(fromInstant, toInstant);
            case "month" -> raw = ordersRepository.revenueByMonth(fromInstant, toInstant);
            default      -> raw = ordersRepository.revenueByDay(fromInstant, toInstant);
        }

        return raw.stream()
            .map(r -> RevenueStatPojo.builder()
                .date(r.getPeriodDate())
                .revenue(r.getRevenue())
                .orderCount(r.getOrderCount())
                .build())
            .collect(Collectors.toList());
    }

    // ─── Top Products ───────────────────────────────────────────────────────────

    @Override
    public Collection<TopProductPojo> getTopProducts(LocalDate from, LocalDate to, int limit) {
        Instant fromInstant = toInstant(from, true);
        Instant toInstant = toInstant(to, false);

        return ordersRepository.findTopProductsByUnitsSold(fromInstant, toInstant, limit)
            .stream()
            .map(r -> TopProductPojo.builder()
                .productId(r.getProductId())
                .productName(r.getProductName())
                .unitsSold(r.getUnitsSold())
                .revenue(r.getRevenue())
                .build())
            .collect(Collectors.toList());
    }

    // ─── Low Stock ─────────────────────────────────────────────────────────────

    @Override
    public Collection<LowStockAlertPojo> getLowStockAlerts() {
        return productVariantsRepository.findLowStockAlerts()
            .stream()
            .map(this::toLowStockAlert)
            .collect(Collectors.toList());
    }

    // ─── Order Status Breakdown ─────────────────────────────────────────────────

    @Override
    public Collection<OrderStatusCountPojo> getOrderStatusBreakdown(LocalDate from, LocalDate to) {
        Instant fromInstant = toInstant(from, true);
        Instant toInstant = toInstant(to, false);

        return ordersRepository.countByStatusGrouped(fromInstant, toInstant)
            .stream()
            .map(r -> OrderStatusCountPojo.builder()
                .status(r.getStatus())
                .count(r.getCount())
                .build())
            .collect(Collectors.toList());
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Instant toInstant(LocalDate date, boolean startOfDay) {
        if (date == null) {
            // Default to 30 days ago for "from", now for "to"
            if (startOfDay) {
                return LocalDate.now(ZoneOffset.UTC).minusDays(30).atStartOfDay(ZoneOffset.UTC).toInstant();
            }
            return Instant.now();
        }
        return startOfDay
            ? date.atStartOfDay(ZoneOffset.UTC).toInstant()
            : date.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();
    }

    private String normalizeGroupBy(String groupBy) {
        if (groupBy == null) {
            return "day";
        }
        String normalized = groupBy.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.equals("week") && !normalized.equals("month")) {
            return "day";
        }
        return normalized;
    }

    private LowStockAlertPojo toLowStockAlert(ProductVariant variant) {
        String productName = (variant.getProduct() != null) ? variant.getProduct().getName() : null;
        return LowStockAlertPojo.builder()
            .variantId(variant.getId())
            .productName(productName)
            .size(variant.getSize())
            .color(variant.getColor())
            .currentStock(variant.getStockCurrent())
            .criticalStock(variant.getStockCritical())
            .build();
    }
}
