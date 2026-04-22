package org.monostudio.api.services.impl;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.monostudio.api.models.StockCountSessionPojo;
import org.monostudio.api.models.inventory.StockCountLineUpsertRequest;
import org.monostudio.api.models.inventory.StockCountSessionCreateRequest;
import org.monostudio.api.services.StockAdjustmentService;
import org.monostudio.api.services.StockCountService;
import org.monostudio.api.services.impl.inventory.InventoryMapper;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.StockAdjustment;
import org.monostudio.jpa.entities.StockCountLine;
import org.monostudio.jpa.entities.StockCountSession;
import org.monostudio.jpa.entities.StockCountSession.StockCountStatus;
import org.monostudio.jpa.repositories.ProductVariantsRepository;
import org.monostudio.jpa.repositories.StockCountLinesRepository;
import org.monostudio.jpa.repositories.StockCountSessionsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockCountServiceImpl
    implements StockCountService {

    private final StockCountSessionsRepository stockCountSessionsRepository;
    private final StockCountLinesRepository stockCountLinesRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final StockAdjustmentService stockAdjustmentService;

    public StockCountServiceImpl(
        StockCountSessionsRepository stockCountSessionsRepository,
        StockCountLinesRepository stockCountLinesRepository,
        ProductVariantsRepository productVariantsRepository,
        StockAdjustmentService stockAdjustmentService
    ) {
        this.stockCountSessionsRepository = stockCountSessionsRepository;
        this.stockCountLinesRepository = stockCountLinesRepository;
        this.productVariantsRepository = productVariantsRepository;
        this.stockAdjustmentService = stockAdjustmentService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockCountSessionPojo> list(String status) {
        StockCountStatus parsed = parseStatus(status);
        return stockCountSessionsRepository.findByStatus(parsed).stream()
            .map(session -> {
                session.setLines(stockCountLinesRepository.findByStockCountSessionId(session.getId()));
                return InventoryMapper.toPojo(session);
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StockCountSessionPojo getById(Long id) {
        StockCountSession session = getSessionOrThrow(id);
        session.setLines(stockCountLinesRepository.findByStockCountSessionId(id));
        return InventoryMapper.toPojo(session);
    }

    @Override
    @Transactional
    public StockCountSessionPojo create(StockCountSessionCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        StockCountSession session = StockCountSession.builder()
            .code(nextCode("SC"))
            .status(StockCountStatus.PLANNED)
            .warehouseId(defaultValue(request.getWarehouseId(), "MAIN"))
            .locationCode(defaultValue(request.getLocationCode(), "MAIN-A1"))
            .plannedAt(parseInstant(request.getPlannedAt(), Instant.now()))
            .note(request.getNote())
            .requestedBy(request.getRequestedBy())
            .build();
        return InventoryMapper.toPojo(stockCountSessionsRepository.save(session));
    }

    @Override
    @Transactional
    public StockCountSessionPojo start(Long id, Long actorId, String note) {
        StockCountSession session = getSessionOrThrow(id);
        if (session.getStatus() != StockCountStatus.PLANNED) {
            throw new IllegalStateException("Only PLANNED session can start");
        }
        List<ProductVariant> variants = productVariantsRepository.findAll().stream()
            .filter(ProductVariant::isActive)
            .collect(Collectors.toList());
        List<StockCountLine> lines = variants.stream()
            .map(variant -> StockCountLine.builder()
                .stockCountSession(session)
                .variant(variant)
                .expectedQty(variant.getStockCurrent())
                .countedQty(null)
                .varianceQty(null)
                .reason(null)
                .build())
            .collect(Collectors.toList());
        stockCountLinesRepository.saveAll(lines);
        session.setLines(lines);
        session.setStatus(StockCountStatus.IN_PROGRESS);
        session.setRequestedBy(actorId != null ? actorId : session.getRequestedBy());
        session.setNote(appendNote(session.getNote(), "START", note));
        return InventoryMapper.toPojo(stockCountSessionsRepository.save(session));
    }

    @Override
    @Transactional
    public StockCountSessionPojo updateCountLines(Long id, StockCountLineUpsertRequest request) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("lines are required");
        }
        StockCountSession session = getSessionOrThrow(id);
        if (session.getStatus() != StockCountStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only IN_PROGRESS session can update lines");
        }
        List<StockCountLine> existingLines = stockCountLinesRepository.findByStockCountSessionId(id);
        Map<Long, StockCountLine> byVariantId = new HashMap<>();
        for (StockCountLine line : existingLines) {
            byVariantId.put(line.getVariant().getId(), line);
        }

        for (StockCountLineUpsertRequest.Line lineRequest : request.getLines()) {
            if (lineRequest == null || lineRequest.getVariantId() == null || lineRequest.getCountedQty() == null || lineRequest.getCountedQty() < 0) {
                throw new IllegalArgumentException("Each line requires variantId and countedQty >= 0");
            }
            StockCountLine line = byVariantId.get(lineRequest.getVariantId());
            if (line == null) {
                ProductVariant variant = productVariantsRepository.findById(lineRequest.getVariantId())
                    .orElseThrow(() -> new EntityNotFoundException("Variant not found: " + lineRequest.getVariantId()));
                line = StockCountLine.builder()
                    .stockCountSession(session)
                    .variant(variant)
                    .expectedQty(variant.getStockCurrent())
                    .build();
            }
            line.setCountedQty(lineRequest.getCountedQty());
            line.setVarianceQty(lineRequest.getCountedQty() - line.getExpectedQty());
            line.setReason(lineRequest.getReason());
            stockCountLinesRepository.save(line);
        }

        session.setLines(stockCountLinesRepository.findByStockCountSessionId(id));
        return InventoryMapper.toPojo(session);
    }

    @Override
    @Transactional
    public StockCountSessionPojo completeCount(Long id, Long actorId, String note) {
        StockCountSession session = getSessionOrThrow(id);
        if (session.getStatus() != StockCountStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only IN_PROGRESS session can complete count");
        }
        List<StockCountLine> lines = stockCountLinesRepository.findByStockCountSessionId(id);
        boolean hasCounted = lines.stream().anyMatch(line -> line.getCountedQty() != null);
        if (!hasCounted) {
            throw new IllegalStateException("No counted lines found");
        }
        session.setStatus(StockCountStatus.COUNTED);
        session.setCountedAt(Instant.now());
        session.setNote(appendNote(session.getNote(), "COUNTED", note));
        session.setLines(lines);
        return InventoryMapper.toPojo(stockCountSessionsRepository.save(session));
    }

    @Override
    @Transactional
    public StockCountSessionPojo approve(Long id, Long actorId, String note) {
        StockCountSession session = getSessionOrThrow(id);
        if (session.getStatus() != StockCountStatus.COUNTED) {
            throw new IllegalStateException("Only COUNTED session can be approved");
        }
        session.setStatus(StockCountStatus.APPROVED);
        session.setApprovedAt(Instant.now());
        session.setApprovedBy(actorId);
        session.setNote(appendNote(session.getNote(), "APPROVE", note));
        return InventoryMapper.toPojo(stockCountSessionsRepository.save(session));
    }

    @Override
    @Transactional
    public StockCountSessionPojo postVariance(Long id, Long actorId, String note) {
        StockCountSession session = getSessionOrThrow(id);
        if (session.getStatus() == StockCountStatus.POSTED) {
            throw new IllegalStateException("Variance already posted");
        }
        if (session.getStatus() != StockCountStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED session can post variance");
        }
        List<StockCountLine> lines = stockCountLinesRepository.findByStockCountSessionId(id);
        for (StockCountLine line : lines) {
            if (line.getCountedQty() == null) {
                continue;
            }
            int delta = line.getCountedQty() - line.getExpectedQty();
            if (delta == 0) {
                continue;
            }
            ProductVariant variant = productVariantsRepository.findByIdWithLock(line.getVariant().getId())
                .orElseThrow(() -> new EntityNotFoundException("Variant not found: " + line.getVariant().getId()));
            if (delta < 0 && variant.getStockCurrent() < Math.abs(delta)) {
                throw new IllegalStateException("Posting variance would make stock negative: " + variant.getSku());
            }
            stockAdjustmentService.applyManualDelta(
                variant.getId(),
                delta,
                StockAdjustment.StockAdjustmentReason.STOCK_COUNT_VARIANCE,
                "Stock count " + session.getCode() + " line " + line.getId() + (line.getReason() != null ? " - " + line.getReason() : ""),
                null,
                actorId
            );
        }
        session.setStatus(StockCountStatus.POSTED);
        session.setPostedAt(Instant.now());
        session.setNote(appendNote(session.getNote(), "POST", note));
        session.setLines(lines);
        return InventoryMapper.toPojo(stockCountSessionsRepository.save(session));
    }

    private StockCountSession getSessionOrThrow(Long id) {
        return stockCountSessionsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Stock count session not found: " + id));
    }

    private StockCountStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return StockCountStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Instant parseInstant(String value, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            return fallback;
        }
    }

    private String defaultValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String appendNote(String current, String action, String note) {
        if (note == null || note.isBlank()) {
            return current;
        }
        if (current == null || current.isBlank()) {
            return "[" + action + "] " + note.trim();
        }
        return current + "\n[" + action + "] " + note.trim();
    }

    private String nextCode(String prefix) {
        return prefix + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
