package org.monostudio.api.services.impl;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.monostudio.api.models.GoodsReceiptPojo;
import org.monostudio.api.models.PurchaseOrderPojo;
import org.monostudio.api.models.inventory.GoodsReceiptCreateRequest;
import org.monostudio.api.models.inventory.PurchaseOrderCreateRequest;
import org.monostudio.api.services.PurchaseOrderService;
import org.monostudio.api.services.StockAdjustmentService;
import org.monostudio.api.services.impl.inventory.InventoryMapper;
import org.monostudio.jpa.entities.GoodsReceipt;
import org.monostudio.jpa.entities.GoodsReceiptLine;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.PurchaseOrder;
import org.monostudio.jpa.entities.PurchaseOrder.PurchaseOrderStatus;
import org.monostudio.jpa.entities.PurchaseOrderLine;
import org.monostudio.jpa.entities.StockAdjustment;
import org.monostudio.jpa.entities.Supplier;
import org.monostudio.jpa.repositories.GoodsReceiptsRepository;
import org.monostudio.jpa.repositories.ProductVariantsRepository;
import org.monostudio.jpa.repositories.PurchaseOrderLinesRepository;
import org.monostudio.jpa.repositories.PurchaseOrdersRepository;
import org.monostudio.jpa.repositories.SuppliersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseOrderServiceImpl
    implements PurchaseOrderService {

    private final PurchaseOrdersRepository purchaseOrdersRepository;
    private final PurchaseOrderLinesRepository purchaseOrderLinesRepository;
    private final GoodsReceiptsRepository goodsReceiptsRepository;
    private final SuppliersRepository suppliersRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final StockAdjustmentService stockAdjustmentService;

    public PurchaseOrderServiceImpl(
        PurchaseOrdersRepository purchaseOrdersRepository,
        PurchaseOrderLinesRepository purchaseOrderLinesRepository,
        GoodsReceiptsRepository goodsReceiptsRepository,
        SuppliersRepository suppliersRepository,
        ProductVariantsRepository productVariantsRepository,
        StockAdjustmentService stockAdjustmentService
    ) {
        this.purchaseOrdersRepository = purchaseOrdersRepository;
        this.purchaseOrderLinesRepository = purchaseOrderLinesRepository;
        this.goodsReceiptsRepository = goodsReceiptsRepository;
        this.suppliersRepository = suppliersRepository;
        this.productVariantsRepository = productVariantsRepository;
        this.stockAdjustmentService = stockAdjustmentService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderPojo> list(String status) {
        PurchaseOrderStatus parsedStatus = parseStatus(status);
        return purchaseOrdersRepository.findAllByStatusDeep(parsedStatus).stream()
            .map(InventoryMapper::toPojo)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderPojo getById(Long id) {
        PurchaseOrder po = getPoOrThrow(id);
        po.setLines(purchaseOrderLinesRepository.findByPurchaseOrderId(id));
        return InventoryMapper.toPojo(po);
    }

    @Override
    @Transactional
    public PurchaseOrderPojo create(PurchaseOrderCreateRequest request) {
        if (request == null || request.getSupplierId() == null) {
            throw new IllegalArgumentException("supplierId is required");
        }
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("At least one line is required");
        }
        Supplier supplier = suppliersRepository.findById(request.getSupplierId())
            .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + request.getSupplierId()));
        if (supplier.isDeleted() || !supplier.isActive()) {
            throw new IllegalArgumentException("Supplier is inactive or deleted");
        }

        PurchaseOrder po = PurchaseOrder.builder()
            .code(nextCode("PO"))
            .supplier(supplier)
            .status(PurchaseOrderStatus.DRAFT)
            .expectedDate(parseInstant(request.getExpectedDate()))
            .warehouseId(blankDefault(request.getWarehouseId(), "MAIN"))
            .locationCode(blankDefault(request.getLocationCode(), "MAIN-A1"))
            .note(request.getNote())
            .requestedBy(request.getRequestedBy())
            .build();
        po = purchaseOrdersRepository.save(po);

        List<PurchaseOrderLine> lines = new ArrayList<>();
        for (PurchaseOrderCreateRequest.Line lineRequest : request.getLines()) {
            if (lineRequest == null || lineRequest.getVariantId() == null || lineRequest.getOrderedQty() == null || lineRequest.getOrderedQty() <= 0) {
                throw new IllegalArgumentException("Each line requires variantId and orderedQty > 0");
            }
            ProductVariant variant = productVariantsRepository.findById(lineRequest.getVariantId())
                .orElseThrow(() -> new EntityNotFoundException("Variant not found: " + lineRequest.getVariantId()));
            PurchaseOrderLine line = PurchaseOrderLine.builder()
                .purchaseOrder(po)
                .variant(variant)
                .orderedQty(lineRequest.getOrderedQty())
                .receivedQty(0)
                .unitCost(lineRequest.getUnitCost())
                .note(lineRequest.getNote())
                .build();
            lines.add(line);
        }
        purchaseOrderLinesRepository.saveAll(lines);
        po.setLines(lines);
        return InventoryMapper.toPojo(po);
    }

    @Override
    @Transactional
    public PurchaseOrderPojo submit(Long id, Long actorId, String note) {
        PurchaseOrder po = getPoOrThrow(id);
        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT PO can be submitted");
        }
        po.setStatus(PurchaseOrderStatus.SUBMITTED);
        po.setSubmittedAt(Instant.now());
        po.setRequestedBy(actorId != null ? actorId : po.getRequestedBy());
        if (note != null && !note.isBlank()) {
            po.setNote(appendNote(po.getNote(), "[SUBMIT] " + note.trim()));
        }
        return InventoryMapper.toPojo(purchaseOrdersRepository.save(po));
    }

    @Override
    @Transactional
    public PurchaseOrderPojo approve(Long id, Long actorId, String note) {
        PurchaseOrder po = getPoOrThrow(id);
        if (po.getStatus() != PurchaseOrderStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED PO can be approved");
        }
        po.setStatus(PurchaseOrderStatus.APPROVED);
        po.setApprovedAt(Instant.now());
        po.setApprovedBy(actorId);
        if (note != null && !note.isBlank()) {
            po.setNote(appendNote(po.getNote(), "[APPROVE] " + note.trim()));
        }
        return InventoryMapper.toPojo(purchaseOrdersRepository.save(po));
    }

    @Override
    @Transactional
    public PurchaseOrderPojo cancel(Long id, Long actorId, String note) {
        PurchaseOrder po = getPoOrThrow(id);
        if (po.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new IllegalStateException("Received PO cannot be cancelled");
        }
        po.setStatus(PurchaseOrderStatus.CANCELLED);
        if (note != null && !note.isBlank()) {
            po.setNote(appendNote(po.getNote(), "[CANCEL] by=" + actorId + " " + note.trim()));
        }
        return InventoryMapper.toPojo(purchaseOrdersRepository.save(po));
    }

    @Override
    @Transactional
    public GoodsReceiptPojo receive(Long id, GoodsReceiptCreateRequest request) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("Receipt lines are required");
        }
        PurchaseOrder po = getPoOrThrow(id);
        if (po.getStatus() != PurchaseOrderStatus.APPROVED && po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalStateException("Only APPROVED/PARTIALLY_RECEIVED PO can receive goods");
        }

        List<PurchaseOrderLine> existingLines = purchaseOrderLinesRepository.findByPurchaseOrderId(po.getId());
        Map<Long, PurchaseOrderLine> linesById = existingLines.stream()
            .collect(Collectors.toMap(PurchaseOrderLine::getId, Function.identity()));

        GoodsReceipt receipt = GoodsReceipt.builder()
            .code(nextCode("GRN"))
            .purchaseOrder(po)
            .receivedBy(request.getReceivedBy())
            .note(request.getNote())
            .build();
        receipt = goodsReceiptsRepository.save(receipt);

        List<GoodsReceiptLine> receiptLines = new ArrayList<>();
        for (GoodsReceiptCreateRequest.Line lineRequest : request.getLines()) {
            if (lineRequest == null || lineRequest.getPurchaseOrderLineId() == null || lineRequest.getReceivedQty() == null || lineRequest.getReceivedQty() <= 0) {
                throw new IllegalArgumentException("Each receipt line requires purchaseOrderLineId and receivedQty > 0");
            }
            PurchaseOrderLine poLine = linesById.get(lineRequest.getPurchaseOrderLineId());
            if (poLine == null) {
                throw new IllegalArgumentException("Line does not belong to purchase order: " + lineRequest.getPurchaseOrderLineId());
            }
            int remaining = poLine.getOrderedQty() - poLine.getReceivedQty();
            if (lineRequest.getReceivedQty() > remaining) {
                throw new IllegalArgumentException("Received qty exceeds remaining qty for line: " + poLine.getId());
            }

            poLine.setReceivedQty(poLine.getReceivedQty() + lineRequest.getReceivedQty());
            purchaseOrderLinesRepository.save(poLine);

            receiptLines.add(GoodsReceiptLine.builder()
                .goodsReceipt(receipt)
                .purchaseOrderLine(poLine)
                .receivedQty(lineRequest.getReceivedQty())
                .build());

            stockAdjustmentService.applyManualDelta(
                poLine.getVariant().getId(),
                lineRequest.getReceivedQty(),
                StockAdjustment.StockAdjustmentReason.PURCHASE_ORDER_RECEIPT,
                "PO " + po.getCode() + " receipt " + receipt.getCode(),
                null,
                request.getReceivedBy()
            );
        }

        receipt.setLines(receiptLines);
        goodsReceiptsRepository.save(receipt);

        int totalOrdered = existingLines.stream().mapToInt(PurchaseOrderLine::getOrderedQty).sum();
        int totalReceived = existingLines.stream().mapToInt(PurchaseOrderLine::getReceivedQty).sum();
        po.setStatus(totalReceived >= totalOrdered ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED);
        po.setReceivedAt(Instant.now());
        purchaseOrdersRepository.save(po);

        return InventoryMapper.toPojo(receipt);
    }

    private PurchaseOrder getPoOrThrow(Long id) {
        return purchaseOrdersRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + id));
    }

    private PurchaseOrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PurchaseOrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Instant parseInstant(String isoInstant) {
        if (isoInstant == null || isoInstant.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(isoInstant.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String blankDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String appendNote(String existing, String incoming) {
        if (existing == null || existing.isBlank()) {
            return incoming;
        }
        return existing + "\n" + incoming;
    }

    private String nextCode(String prefix) {
        return prefix + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
