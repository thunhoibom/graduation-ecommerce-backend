package org.monostudio.api.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ProductVariantPojo;
import org.monostudio.api.models.StockAdjustmentPojo;
import org.monostudio.api.services.StockAdjustmentService;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.StockAdjustment;
import org.monostudio.jpa.repositories.ProductVariantsRepository;
import org.monostudio.jpa.repositories.StockAdjustmentsRepository;
import org.monostudio.jpa.services.conversion.ProductVariantsConverterService;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockAdjustmentServiceImpl
    implements StockAdjustmentService {
    private static final Logger logger = LoggerFactory.getLogger(StockAdjustmentServiceImpl.class);

    private final StockAdjustmentsRepository stockAdjustmentsRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final ProductVariantsConverterService variantsConverterService;

    @Autowired
    public StockAdjustmentServiceImpl(
        StockAdjustmentsRepository stockAdjustmentsRepository,
        ProductVariantsRepository productVariantsRepository,
        ProductVariantsConverterService variantsConverterService
    ) {
        this.stockAdjustmentsRepository = stockAdjustmentsRepository;
        this.productVariantsRepository = productVariantsRepository;
        this.variantsConverterService = variantsConverterService;
    }

    @Override
    @Transactional
    public StockAdjustmentPojo record(Long variantId, StockAdjustment.StockAdjustmentReason reason, int quantityDelta) {
        return recordFull(variantId, reason, quantityDelta, null, null, null, null, null);
    }

    @Override
    @Transactional
    public StockAdjustmentPojo recordFull(
        Long variantId,
        StockAdjustment.StockAdjustmentReason reason,
        int quantityDelta,
        String description,
        String sessionId,
        Long orderId,
        Long returnRequestId,
        Long performedBy
    ) {
        ProductVariant variant = productVariantsRepository.getById(variantId);
        return recordForVariant(variant, reason, quantityDelta, description, sessionId, orderId, returnRequestId, performedBy);
    }

    @Override
    @Transactional
    public StockAdjustmentPojo recordForVariant(
        ProductVariant variant,
        StockAdjustment.StockAdjustmentReason reason,
        int quantityDelta,
        String description,
        String sessionId,
        Long orderId,
        Long returnRequestId,
        Long performedBy
    ) {
        int stockBefore = variant.getStockCurrent();
        int stockAfter = stockBefore + quantityDelta;

        StockAdjustment adjustment = StockAdjustment.builder()
            .variant(variant)
            .reason(reason)
            .quantityDelta(quantityDelta)
            .stockBefore(stockBefore)
            .stockAfter(stockAfter)
            .description(description)
            .sessionId(sessionId)
            .orderId(orderId)
            .returnRequestId(returnRequestId)
            .performedBy(performedBy)
            .build();

        StockAdjustment saved = stockAdjustmentsRepository.saveAndFlush(adjustment);
        logger.info("StockAdjustment logged: variant={}, reason={}, delta={}, {} -> {}",
            variant.getId(), reason, quantityDelta, stockBefore, stockAfter);

        return toPojo(saved, variant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentPojo> getByVariant(Long variantId) {
        return stockAdjustmentsRepository.findByVariantIdDeep(variantId).stream()
            .map(a -> toPojo(a, a.getVariant()))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentPojo> getByVariant(Long variantId, int page, int size) {
        return stockAdjustmentsRepository.findByVariantId(variantId, PageRequest.of(page, size)).stream()
            .map(a -> toPojo(a, a.getVariant()))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentPojo> getByOrder(Long orderId) {
        return stockAdjustmentsRepository.findByOrderId(orderId).stream()
            .map(a -> toPojo(a, a.getVariant()))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentPojo> getByReturnRequest(Long returnRequestId) {
        return stockAdjustmentsRepository.findByReturnRequestId(returnRequestId).stream()
            .map(a -> toPojo(a, a.getVariant()))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentPojo> getByDateRange(Instant start, Instant end, int page, int size) {
        return stockAdjustmentsRepository.findByDateRangeDeep(start, end, PageRequest.of(page, size)).stream()
            .map(a -> toPojo(a, a.getVariant()))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentPojo> getAll(int page, int size) {
        return stockAdjustmentsRepository.findAllDeep(PageRequest.of(page, size)).stream()
            .map(a -> toPojo(a, a.getVariant()))
            .collect(Collectors.toList());
    }

    private StockAdjustmentPojo toPojo(StockAdjustment adjustment, ProductVariant variant) {
        StockAdjustmentPojo.StockAdjustmentPojoBuilder builder = StockAdjustmentPojo.builder()
            .id(adjustment.getId())
            .date(adjustment.getDate())
            .reason(adjustment.getReason().name())
            .quantityDelta(adjustment.getQuantityDelta())
            .stockBefore(adjustment.getStockBefore())
            .stockAfter(adjustment.getStockAfter())
            .description(adjustment.getDescription())
            .sessionId(adjustment.getSessionId())
            .orderId(adjustment.getOrderId())
            .returnRequestId(adjustment.getReturnRequestId())
            .performedBy(adjustment.getPerformedBy());

        if (variant != null) {
            builder.variantId(variant.getId())
                .variantSku(variant.getSku())
                .variantSize(variant.getSize())
                .variantColor(variant.getColor());
            if (variant.getProduct() != null) {
                builder.productName(variant.getProduct().getName());
            }
        }

        return builder.build();
    }
}
