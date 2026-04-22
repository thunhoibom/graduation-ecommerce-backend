package org.monostudio.api.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.ProductAuditLogPojo;
import org.monostudio.api.services.ProductAuditLogService;
import org.monostudio.jpa.entities.ProductAuditLog;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.repositories.ProductAuditLogsRepository;
import org.monostudio.jpa.repositories.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ProductAuditLogServiceImpl
    implements ProductAuditLogService {
    private static final Logger logger = LoggerFactory.getLogger(ProductAuditLogServiceImpl.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private final ProductAuditLogsRepository productAuditLogsRepository;
    private final UsersRepository usersRepository;
    private final ObjectMapper objectMapper;

    public ProductAuditLogServiceImpl(
        ProductAuditLogsRepository productAuditLogsRepository,
        UsersRepository usersRepository,
        ObjectMapper objectMapper
    ) {
        this.productAuditLogsRepository = productAuditLogsRepository;
        this.usersRepository = usersRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void recordBestEffort(AuditEvent event) {
        try {
            ProductAuditLog log = ProductAuditLog.builder()
                .action(event.action())
                .entityType(event.entityType())
                .entityId(event.entityId())
                .productId(event.productId())
                .variantId(event.variantId())
                .entityCode(event.entityCode())
                .summary(event.summary())
                .beforeSnapshot(toJson(event.beforeSnapshot()))
                .afterSnapshot(toJson(event.afterSnapshot()))
                .actorUsername(event.actorUsername())
                .actorUserId(resolveUserId(event.actorUsername()))
                .requestSource(event.requestSource())
                .correlationId(event.correlationId())
                .build();

            productAuditLogsRepository.save(log);
        } catch (Exception ex) {
            logger.warn(
                "Failed to record product audit log for action={} entityType={} entityId={}: {}",
                event.action(),
                event.entityType(),
                event.entityId(),
                ex.getMessage()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DataPagePojo<ProductAuditLogPojo> search(
        ProductAuditLog.EntityType entityType,
        Long entityId,
        Long productId,
        Long variantId,
        String action,
        String actorUsername,
        Instant fromTime,
        Instant toTime,
        int pageIndex,
        int pageSize
    ) {
        Page<ProductAuditLog> page = productAuditLogsRepository.search(
            entityType,
            entityId,
            productId,
            variantId,
            normalize(action),
            normalize(actorUsername),
            fromTime,
            toTime,
            pageRequest(pageIndex, pageSize)
        );
        return toPagePojo(page);
    }

    @Override
    @Transactional(readOnly = true)
    public DataPagePojo<ProductAuditLogPojo> findByProductId(Long productId, int pageIndex, int pageSize) {
        Page<ProductAuditLog> page = productAuditLogsRepository.findByProductId(
            productId,
            pageRequest(pageIndex, pageSize)
        );
        return toPagePojo(page);
    }

    @Override
    @Transactional(readOnly = true)
    public DataPagePojo<ProductAuditLogPojo> findByVariantId(Long variantId, int pageIndex, int pageSize) {
        Page<ProductAuditLog> page = productAuditLogsRepository.findByVariantId(
            variantId,
            pageRequest(pageIndex, pageSize)
        );
        return toPagePojo(page);
    }

    private DataPagePojo<ProductAuditLogPojo> toPagePojo(Page<ProductAuditLog> page) {
        return new DataPagePojo<>(
            page.stream().map(this::toPojo).toList(),
            page.getNumber(),
            page.getTotalElements(),
            page.getSize()
        );
    }

    private ProductAuditLogPojo toPojo(ProductAuditLog log) {
        return ProductAuditLogPojo.builder()
            .id(log.getId())
            .occurredAt(log.getOccurredAt())
            .actorUsername(log.getActorUsername())
            .actorUserId(log.getActorUserId())
            .action(log.getAction())
            .entityType(log.getEntityType())
            .entityId(log.getEntityId())
            .productId(log.getProductId())
            .variantId(log.getVariantId())
            .entityCode(log.getEntityCode())
            .summary(log.getSummary())
            .beforeSnapshot(log.getBeforeSnapshot())
            .afterSnapshot(log.getAfterSnapshot())
            .requestSource(log.getRequestSource())
            .correlationId(log.getCorrelationId())
            .build();
    }

    private String toJson(Object snapshot) {
        if (snapshot == null) {
            return null;
        }
        if (snapshot instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            return "{\"serializationError\":true}";
        }
    }

    private Long resolveUserId(String actorUsername) {
        String normalizedActor = normalize(actorUsername);
        if (normalizedActor == null) {
            return null;
        }
        return usersRepository.findByName(normalizedActor)
            .map(User::getId)
            .orElse(null);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PageRequest pageRequest(int pageIndex, int pageSize) {
        int safePageIndex = Math.max(pageIndex, 0);
        int safePageSize = pageSize <= 0
            ? DEFAULT_PAGE_SIZE
            : Math.min(pageSize, MAX_PAGE_SIZE);
        return PageRequest.of(
            safePageIndex,
            safePageSize,
            Sort.by(Sort.Direction.DESC, "occurredAt")
        );
    }
}
