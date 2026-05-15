package org.monostudio.api.services.impl;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.monostudio.api.models.StockTransferPojo;
import org.monostudio.api.models.inventory.StockTransferCreateRequest;
import org.monostudio.api.services.StockAdjustmentService;
import org.monostudio.api.services.StockTransferService;
import org.monostudio.api.services.impl.inventory.InventoryMapper;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.StockAdjustment;
import org.monostudio.jpa.entities.StockTransfer;
import org.monostudio.jpa.entities.StockTransfer.StockTransferStatus;
import org.monostudio.jpa.entities.StockTransferLine;
import org.monostudio.jpa.repositories.ProductVariantsRepository;
import org.monostudio.jpa.repositories.StockTransferLinesRepository;
import org.monostudio.jpa.repositories.StockTransfersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockTransferServiceImpl
    implements StockTransferService {

    private final StockTransfersRepository stockTransfersRepository;
    private final StockTransferLinesRepository stockTransferLinesRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final StockAdjustmentService stockAdjustmentService;

    public StockTransferServiceImpl(
        StockTransfersRepository stockTransfersRepository,
        StockTransferLinesRepository stockTransferLinesRepository,
        ProductVariantsRepository productVariantsRepository,
        StockAdjustmentService stockAdjustmentService
    ) {
        this.stockTransfersRepository = stockTransfersRepository;
        this.stockTransferLinesRepository = stockTransferLinesRepository;
        this.productVariantsRepository = productVariantsRepository;
        this.stockAdjustmentService = stockAdjustmentService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockTransferPojo> list(String status) {
        StockTransferStatus parsed = parseStatus(status);
        return stockTransfersRepository.findByStatus(parsed).stream()
            .map(transfer -> {
                transfer.setLines(stockTransferLinesRepository.findByStockTransferId(transfer.getId()));
                return InventoryMapper.toPojo(transfer);
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StockTransferPojo getById(Long id) {
        StockTransfer transfer = stockTransfersRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Stock transfer not found: " + id));
        transfer.setLines(stockTransferLinesRepository.findByStockTransferId(id));
        return InventoryMapper.toPojo(transfer);
    }

    @Override
    @Transactional
    public StockTransferPojo create(StockTransferCreateRequest request) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("Transfer lines are required");
        }
        if (request.getFromLocation() == null || request.getFromLocation().isBlank()
            || request.getToLocation() == null || request.getToLocation().isBlank()) {
            throw new IllegalArgumentException("fromLocation and toLocation are required");
        }
        if (request.getFromLocation().trim().equalsIgnoreCase(request.getToLocation().trim())) {
            throw new IllegalArgumentException("fromLocation and toLocation must be different");
        }

        StockTransfer transfer = StockTransfer.builder()
            .code(nextCode("TRF"))
            .status(StockTransferStatus.DRAFT)
            .warehouseId(defaultValue(request.getWarehouseId(), "MAIN"))
            .fromLocation(request.getFromLocation().trim())
            .toLocation(request.getToLocation().trim())
            .requestedBy(request.getRequestedBy())
            .note(request.getNote())
            .build();
        transfer = stockTransfersRepository.save(transfer);

        List<StockTransferLine> lines = new ArrayList<>();
        for (StockTransferCreateRequest.Line lineRequest : request.getLines()) {
            if (lineRequest == null || lineRequest.getVariantId() == null || lineRequest.getQuantity() == null || lineRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Each transfer line requires variantId and quantity > 0");
            }
            ProductVariant variant = productVariantsRepository.findById(lineRequest.getVariantId())
                .orElseThrow(() -> new EntityNotFoundException("Variant not found: " + lineRequest.getVariantId()));
            lines.add(StockTransferLine.builder()
                .stockTransfer(transfer)
                .variant(variant)
                .quantity(lineRequest.getQuantity())
                .build());
        }
        stockTransferLinesRepository.saveAll(lines);
        transfer.setLines(lines);
        return InventoryMapper.toPojo(transfer);
    }

    @Override
    @Transactional
    public StockTransferPojo submit(Long id, Long actorId, String note) {
        StockTransfer transfer = getTransferOrThrow(id);
        if (transfer.getStatus() != StockTransferStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT transfer can be submitted");
        }
        transfer.setStatus(StockTransferStatus.SUBMITTED);
        transfer.setSubmittedAt(Instant.now());
        transfer.setRequestedBy(actorId != null ? actorId : transfer.getRequestedBy());
        transfer.setNote(appendNote(transfer.getNote(), note, "SUBMIT"));
        return InventoryMapper.toPojo(stockTransfersRepository.save(transfer));
    }

    @Override
    @Transactional
    public StockTransferPojo approve(Long id, Long actorId, String note) {
        StockTransfer transfer = getTransferOrThrow(id);
        if (transfer.getStatus() != StockTransferStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED transfer can be approved");
        }
        transfer.setStatus(StockTransferStatus.APPROVED);
        transfer.setApprovedAt(Instant.now());
        transfer.setApprovedBy(actorId);
        transfer.setNote(appendNote(transfer.getNote(), note, "APPROVE"));
        return InventoryMapper.toPojo(stockTransfersRepository.save(transfer));
    }

    @Override
    @Transactional
    public StockTransferPojo complete(Long id, Long actorId, String note) {
        StockTransfer transfer = getTransferOrThrow(id);
        if (transfer.getStatus() == StockTransferStatus.COMPLETED) {
            throw new IllegalStateException("Transfer is already completed");
        }
        if (transfer.getStatus() != StockTransferStatus.APPROVED && transfer.getStatus() != StockTransferStatus.IN_TRANSIT) {
            throw new IllegalStateException("Only APPROVED/IN_TRANSIT transfer can be completed");
        }
        List<StockTransferLine> lines = stockTransferLinesRepository.findByStockTransferId(id);
        if (lines.isEmpty()) {
            throw new IllegalStateException("Transfer has no lines");
        }
        transfer.setStatus(StockTransferStatus.IN_TRANSIT);
        stockTransfersRepository.save(transfer);

        for (StockTransferLine line : lines) {
            ProductVariant variant = productVariantsRepository.findByIdWithLock(line.getVariant().getId())
                .orElseThrow(() -> new EntityNotFoundException("Variant not found: " + line.getVariant().getId()));
            if (variant.getStockCurrent() < line.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for variant " + variant.getSku());
            }
            stockAdjustmentService.applyManualDelta(
                variant.getId(),
                -line.getQuantity(),
                StockAdjustment.StockAdjustmentReason.TRANSFER_OUTBOUND,
                "Transfer " + transfer.getCode() + " outbound " + transfer.getFromLocation() + "->" + transfer.getToLocation(),
                null,
                actorId
            );
            stockAdjustmentService.applyManualDelta(
                variant.getId(),
                line.getQuantity(),
                StockAdjustment.StockAdjustmentReason.TRANSFER_INBOUND,
                "Transfer " + transfer.getCode() + " inbound " + transfer.getFromLocation() + "->" + transfer.getToLocation(),
                null,
                actorId
            );
        }
        transfer.setStatus(StockTransferStatus.COMPLETED);
        transfer.setCompletedAt(Instant.now());
        transfer.setNote(appendNote(transfer.getNote(), note, "COMPLETE"));
        transfer.setLines(lines);
        return InventoryMapper.toPojo(stockTransfersRepository.save(transfer));
    }

    @Override
    @Transactional
    public StockTransferPojo cancel(Long id, Long actorId, String note) {
        StockTransfer transfer = getTransferOrThrow(id);
        if (transfer.getStatus() == StockTransferStatus.COMPLETED) {
            throw new IllegalStateException("Completed transfer cannot be cancelled");
        }
        transfer.setStatus(StockTransferStatus.CANCELLED);
        transfer.setNote(appendNote(transfer.getNote(), note != null ? note : "cancelled by " + actorId, "CANCEL"));
        return InventoryMapper.toPojo(stockTransfersRepository.save(transfer));
    }

    private StockTransfer getTransferOrThrow(Long id) {
        return stockTransfersRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Stock transfer not found: " + id));
    }

    private StockTransferStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return StockTransferStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String nextCode(String prefix) {
        return prefix + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String defaultValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String appendNote(String current, String note, String action) {
        if (note == null || note.isBlank()) {
            return current;
        }
        if (current == null || current.isBlank()) {
            return "[" + action + "] " + note.trim();
        }
        return current + "\n[" + action + "] " + note.trim();
    }
}
