package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.*;
import org.monostudio.api.services.AdminDashboardService;
import org.monostudio.jpa.entities.PurchaseOrder.PurchaseOrderStatus;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.StockCountSession.StockCountStatus;
import org.monostudio.jpa.entities.StockTransfer.StockTransferStatus;
import org.monostudio.jpa.repositories.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
public class AdminDashboardServiceImpl
    implements AdminDashboardService {

    private final OrdersRepository ordersRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final PurchaseOrdersRepository purchaseOrdersRepository;
    private final StockTransfersRepository stockTransfersRepository;
    private final StockCountSessionsRepository stockCountSessionsRepository;

    @Autowired
    public AdminDashboardServiceImpl(
        OrdersRepository ordersRepository,
        ProductVariantsRepository productVariantsRepository,
        PurchaseOrdersRepository purchaseOrdersRepository,
        StockTransfersRepository stockTransfersRepository,
        StockCountSessionsRepository stockCountSessionsRepository
    ) {
        this.ordersRepository = ordersRepository;
        this.productVariantsRepository = productVariantsRepository;
        this.purchaseOrdersRepository = purchaseOrdersRepository;
        this.stockTransfersRepository = stockTransfersRepository;
        this.stockCountSessionsRepository = stockCountSessionsRepository;
    }

    // ─── Full Dashboard ─────────────────────────────────────────────────────────

    @Override
    public AdminDashboardStatsPojo getDashboardStats(LocalDate from, LocalDate to) {
        Instant fromInstant = toInstant(from, true);
        Instant toInstant = toInstant(to, false);

        Collection<RevenueStatPojo> revenueByPeriod = getRevenueByPeriod(from, to, "day");
        long totalRevenue = revenueByPeriod.stream()
            .mapToLong(RevenueStatPojo::getRevenue)
            .sum();
        long totalOrders = ordersRepository.countByDateBetween(fromInstant, toInstant);
        Collection<TopProductPojo> topProducts = getTopProducts(from, to, 10);
        Collection<OrderStatusCountPojo> statusBreakdown = getOrderStatusBreakdown(from, to);
        Collection<LowStockAlertPojo> lowStockAlerts = getLowStockAlerts();
        InventoryKpiPojo inventoryKpis = getInventoryKpis();

        return AdminDashboardStatsPojo.builder()
            .totalRevenue(totalRevenue)
            .totalOrders(totalOrders)
            .revenueByPeriod(revenueByPeriod)
            .topProducts(topProducts)
            .orderStatusBreakdown(statusBreakdown)
            .lowStockAlerts(lowStockAlerts)
            .inventoryKpis(inventoryKpis)
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

    @Override
    public Collection<RestockSuggestionPojo> getLowStockRestockSuggestions(int lookbackDays, int leadTimeDays) {
        int safeLookbackDays = Math.max(1, lookbackDays);
        int safeLeadTimeDays = Math.max(1, leadTimeDays);
        Instant to = Instant.now();
        Instant from = to.minus(safeLookbackDays, ChronoUnit.DAYS);

        Map<Long, Long> soldByVariant = ordersRepository.findVariantUnitsSold(from, to)
            .stream()
            .collect(Collectors.toMap(
                VariantSalesProjection::getVariantId,
                VariantSalesProjection::getUnitsSold,
                Long::sum
            ));

        return productVariantsRepository.findLowStockAlerts()
            .stream()
            .map(v -> toRestockSuggestion(v, safeLookbackDays, safeLeadTimeDays, soldByVariant.getOrDefault(v.getId(), 0L)))
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

    @Override
    public InventoryKpiPojo getInventoryKpis() {
        long openPurchaseOrders = purchaseOrdersRepository.countByStatusIn(List.of(
            PurchaseOrderStatus.SUBMITTED,
            PurchaseOrderStatus.APPROVED,
            PurchaseOrderStatus.PARTIALLY_RECEIVED
        ));
        long pendingTransferApprovals = stockTransfersRepository.countByStatus(StockTransferStatus.SUBMITTED);
        long pendingStockCountApprovals = stockCountSessionsRepository.countByStatus(StockCountStatus.COUNTED);
        long approvedStockCountsToPost = stockCountSessionsRepository.countByStatus(StockCountStatus.APPROVED);

        return InventoryKpiPojo.builder()
            .openPurchaseOrders(openPurchaseOrders)
            .pendingTransferApprovals(pendingTransferApprovals)
            .pendingStockCountApprovals(pendingStockCountApprovals)
            .approvedStockCountsToPost(approvedStockCountsToPost)
            .build();
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

    private RestockSuggestionPojo toRestockSuggestion(
        ProductVariant variant,
        int lookbackDays,
        int leadTimeDays,
        long soldInLookback
    ) {
        double avgDailySold = soldInLookback / (double) lookbackDays;
        int projectedDemand = (int) Math.ceil(avgDailySold * leadTimeDays);
        int safetyStock = Math.max(variant.getStockCritical(), (int) Math.ceil(avgDailySold * 7));
        int recommendedRestockQty = Math.max(0, projectedDemand + safetyStock - variant.getStockCurrent());

        String productName = (variant.getProduct() != null) ? variant.getProduct().getName() : null;
        return RestockSuggestionPojo.builder()
            .variantId(variant.getId())
            .sku(variant.getSku())
            .productName(productName)
            .size(variant.getSize())
            .color(variant.getColor())
            .currentStock(variant.getStockCurrent())
            .criticalStock(variant.getStockCritical())
            .lookbackDays(lookbackDays)
            .leadTimeDays(leadTimeDays)
            .soldInLookback(soldInLookback)
            .avgDailySold(avgDailySold)
            .projectedDemand(projectedDemand)
            .safetyStock(safetyStock)
            .recommendedRestockQty(recommendedRestockQty)
            .build();
    }
}
