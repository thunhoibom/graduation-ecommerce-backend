package org.monostudio.api.services.impl.inventory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.monostudio.api.models.GoodsReceiptLinePojo;
import org.monostudio.api.models.GoodsReceiptPojo;
import org.monostudio.api.models.PurchaseOrderLinePojo;
import org.monostudio.api.models.PurchaseOrderPojo;
import org.monostudio.api.models.StockCountLinePojo;
import org.monostudio.api.models.StockCountSessionPojo;
import org.monostudio.api.models.StockTransferLinePojo;
import org.monostudio.api.models.StockTransferPojo;
import org.monostudio.api.models.SupplierPojo;
import org.monostudio.jpa.entities.GoodsReceipt;
import org.monostudio.jpa.entities.GoodsReceiptLine;
import org.monostudio.jpa.entities.PurchaseOrder;
import org.monostudio.jpa.entities.PurchaseOrderLine;
import org.monostudio.jpa.entities.StockCountLine;
import org.monostudio.jpa.entities.StockCountSession;
import org.monostudio.jpa.entities.StockTransfer;
import org.monostudio.jpa.entities.StockTransferLine;
import org.monostudio.jpa.entities.Supplier;
import org.monostudio.jpa.repositories.PurchaseOrderLineTotalsProjection;

public final class InventoryMapper {
    private InventoryMapper() {}

    public static SupplierPojo toPojo(Supplier s) {
        return SupplierPojo.builder()
            .id(s.getId())
            .code(s.getCode())
            .name(s.getName())
            .contactName(s.getContactName())
            .phone(s.getPhone())
            .email(s.getEmail())
            .address(s.getAddress())
            .active(s.isActive())
            .deleted(s.isDeleted())
            .createdAt(ts(s.getCreatedAt()))
            .updatedAt(ts(s.getUpdatedAt()))
            .build();
    }

    public static PurchaseOrderPojo toPojo(PurchaseOrder po) {
        List<PurchaseOrderLine> lines = po.getLines() == null ? List.of() : po.getLines();
        PurchaseOrderTotals totals = summarizeLines(lines);
        return PurchaseOrderPojo.builder()
            .id(po.getId())
            .code(po.getCode())
            .status(po.getStatus() != null ? po.getStatus().name() : null)
            .supplierId(po.getSupplier() != null ? po.getSupplier().getId() : null)
            .supplierCode(po.getSupplier() != null ? po.getSupplier().getCode() : null)
            .supplierName(po.getSupplier() != null ? po.getSupplier().getName() : null)
            .expectedDate(ts(po.getExpectedDate()))
            .submittedAt(ts(po.getSubmittedAt()))
            .approvedAt(ts(po.getApprovedAt()))
            .receivedAt(ts(po.getReceivedAt()))
            .requestedBy(po.getRequestedBy())
            .approvedBy(po.getApprovedBy())
            .warehouseId(po.getWarehouseId())
            .locationCode(po.getLocationCode())
            .note(po.getNote())
            .createdAt(ts(po.getCreatedAt()))
            .updatedAt(ts(po.getUpdatedAt()))
            .lineCount(totals.lineCount())
            .orderedTotalAmount(totals.orderedTotalAmount())
            .receivedTotalAmount(totals.receivedTotalAmount())
            .lines(lines.stream().map(InventoryMapper::toPojo).collect(Collectors.toList()))
            .build();
    }

    public static PurchaseOrderPojo applyTotals(PurchaseOrderPojo pojo, PurchaseOrderTotals totals) {
        if (pojo == null || totals == null) {
            return pojo;
        }
        pojo.setLineCount(totals.lineCount());
        pojo.setOrderedTotalAmount(totals.orderedTotalAmount());
        pojo.setReceivedTotalAmount(totals.receivedTotalAmount());
        return pojo;
    }

    public static PurchaseOrderTotals summarizeLines(List<PurchaseOrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return PurchaseOrderTotals.empty();
        }
        int lineCount = lines.size();
        long orderedTotalAmount = 0L;
        long receivedTotalAmount = 0L;
        for (PurchaseOrderLine line : lines) {
            if (line == null) {
                continue;
            }
            int unitCost = line.getUnitCost() != null ? line.getUnitCost() : 0;
            orderedTotalAmount += (long) line.getOrderedQty() * unitCost;
            receivedTotalAmount += (long) line.getReceivedQty() * unitCost;
        }
        return new PurchaseOrderTotals(lineCount, orderedTotalAmount, receivedTotalAmount);
    }

    public static PurchaseOrderTotals toTotals(PurchaseOrderLineTotalsProjection projection) {
        if (projection == null) {
            return PurchaseOrderTotals.empty();
        }
        return new PurchaseOrderTotals(
            projection.getLineCount() != null ? projection.getLineCount().intValue() : 0,
            projection.getOrderedTotalAmount() != null ? projection.getOrderedTotalAmount() : 0L,
            projection.getReceivedTotalAmount() != null ? projection.getReceivedTotalAmount() : 0L
        );
    }

    public record PurchaseOrderTotals(int lineCount, long orderedTotalAmount, long receivedTotalAmount) {
        public static PurchaseOrderTotals empty() {
            return new PurchaseOrderTotals(0, 0L, 0L);
        }
    }

    public static PurchaseOrderLinePojo toPojo(PurchaseOrderLine line) {
        int unitCost = line.getUnitCost() != null ? line.getUnitCost() : 0;
        return PurchaseOrderLinePojo.builder()
            .id(line.getId())
            .variantId(line.getVariant() != null ? line.getVariant().getId() : null)
            .variantSku(line.getVariant() != null ? line.getVariant().getSku() : null)
            .barcode(line.getVariant() != null ? line.getVariant().getBarcode() : null)
            .productName(line.getVariant() != null && line.getVariant().getProduct() != null ? line.getVariant().getProduct().getName() : null)
            .orderedQty(line.getOrderedQty())
            .receivedQty(line.getReceivedQty())
            .unitCost(line.getUnitCost())
            .lineTotalAmount((long) line.getOrderedQty() * unitCost)
            .note(line.getNote())
            .build();
    }

    public static GoodsReceiptPojo toPojo(GoodsReceipt receipt) {
        return GoodsReceiptPojo.builder()
            .id(receipt.getId())
            .code(receipt.getCode())
            .purchaseOrderId(receipt.getPurchaseOrder() != null ? receipt.getPurchaseOrder().getId() : null)
            .purchaseOrderCode(receipt.getPurchaseOrder() != null ? receipt.getPurchaseOrder().getCode() : null)
            .receivedBy(receipt.getReceivedBy())
            .note(receipt.getNote())
            .createdAt(ts(receipt.getCreatedAt()))
            .lines(receipt.getLines() == null ? List.of() : receipt.getLines().stream().map(InventoryMapper::toPojo).collect(Collectors.toList()))
            .build();
    }

    public static GoodsReceiptLinePojo toPojo(GoodsReceiptLine line) {
        return GoodsReceiptLinePojo.builder()
            .purchaseOrderLineId(line.getPurchaseOrderLine() != null ? line.getPurchaseOrderLine().getId() : null)
            .variantId(line.getPurchaseOrderLine() != null && line.getPurchaseOrderLine().getVariant() != null ? line.getPurchaseOrderLine().getVariant().getId() : null)
            .variantSku(line.getPurchaseOrderLine() != null && line.getPurchaseOrderLine().getVariant() != null ? line.getPurchaseOrderLine().getVariant().getSku() : null)
            .receivedQty(line.getReceivedQty())
            .build();
    }

    public static StockTransferPojo toPojo(StockTransfer transfer) {
        return StockTransferPojo.builder()
            .id(transfer.getId())
            .code(transfer.getCode())
            .status(transfer.getStatus() != null ? transfer.getStatus().name() : null)
            .warehouseId(transfer.getWarehouseId())
            .fromLocation(transfer.getFromLocation())
            .toLocation(transfer.getToLocation())
            .requestedBy(transfer.getRequestedBy())
            .approvedBy(transfer.getApprovedBy())
            .submittedAt(ts(transfer.getSubmittedAt()))
            .approvedAt(ts(transfer.getApprovedAt()))
            .completedAt(ts(transfer.getCompletedAt()))
            .note(transfer.getNote())
            .createdAt(ts(transfer.getCreatedAt()))
            .updatedAt(ts(transfer.getUpdatedAt()))
            .lines(transfer.getLines() == null ? List.of() : transfer.getLines().stream().map(InventoryMapper::toPojo).collect(Collectors.toList()))
            .build();
    }

    public static StockTransferLinePojo toPojo(StockTransferLine line) {
        return StockTransferLinePojo.builder()
            .id(line.getId())
            .variantId(line.getVariant() != null ? line.getVariant().getId() : null)
            .variantSku(line.getVariant() != null ? line.getVariant().getSku() : null)
            .productName(line.getVariant() != null && line.getVariant().getProduct() != null ? line.getVariant().getProduct().getName() : null)
            .quantity(line.getQuantity())
            .build();
    }

    public static StockCountSessionPojo toPojo(StockCountSession session) {
        return StockCountSessionPojo.builder()
            .id(session.getId())
            .code(session.getCode())
            .status(session.getStatus() != null ? session.getStatus().name() : null)
            .warehouseId(session.getWarehouseId())
            .locationCode(session.getLocationCode())
            .plannedAt(ts(session.getPlannedAt()))
            .countedAt(ts(session.getCountedAt()))
            .approvedAt(ts(session.getApprovedAt()))
            .postedAt(ts(session.getPostedAt()))
            .requestedBy(session.getRequestedBy())
            .approvedBy(session.getApprovedBy())
            .note(session.getNote())
            .createdAt(ts(session.getCreatedAt()))
            .updatedAt(ts(session.getUpdatedAt()))
            .lines(session.getLines() == null ? List.of() : session.getLines().stream().filter(Objects::nonNull).map(InventoryMapper::toPojo).collect(Collectors.toList()))
            .build();
    }

    public static StockCountLinePojo toPojo(StockCountLine line) {
        return StockCountLinePojo.builder()
            .id(line.getId())
            .variantId(line.getVariant() != null ? line.getVariant().getId() : null)
            .variantSku(line.getVariant() != null ? line.getVariant().getSku() : null)
            .productName(line.getVariant() != null && line.getVariant().getProduct() != null ? line.getVariant().getProduct().getName() : null)
            .expectedQty(line.getExpectedQty())
            .countedQty(line.getCountedQty())
            .varianceQty(line.getVarianceQty())
            .reason(line.getReason())
            .build();
    }

    private static String ts(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
