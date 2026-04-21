package org.monostudio.api.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.monostudio.api.models.AdminNotificationEventPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.services.AdminNotificationService;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.monostudio.config.Constants.ORDER_STATUS_PENDING;

@Service
public class AdminNotificationServiceImpl implements AdminNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(AdminNotificationServiceImpl.class);
    private static final String EVENT_ORDER_CREATED = "order.created";
    private static final String EVENT_KEEP_ALIVE = "keep-alive";

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final long emitterTimeoutMs;

    public AdminNotificationServiceImpl(
        @Value("${monostudio.notifications.sse-timeout-ms:1800000}") long emitterTimeoutMs
    ) {
        this.emitterTimeoutMs = emitterTimeoutMs;
    }

    @Override
    public SseEmitter subscribe(String principalKey) {
        String emitterId = principalKey + ":" + UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(emitterTimeoutMs);
        emitters.put(emitterId, emitter);

        emitter.onCompletion(() -> removeEmitter(emitterId, "completed"));
        emitter.onTimeout(() -> removeEmitter(emitterId, "timed out"));
        emitter.onError(ex -> removeEmitter(emitterId, "error: " + ex.getMessage()));

        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .id(emitterId)
                .data(Map.of("status", "ok", "timestamp", Instant.now().toString())));
        } catch (IOException ex) {
            removeEmitter(emitterId, "failed to initialize: " + ex.getMessage());
        }

        logger.info("Admin SSE subscribed: {} (active emitters={})", emitterId, emitters.size());
        return emitter;
    }

    @Override
    public void publishOrderCreated(OrderPojo orderPojo) {
        AdminNotificationEventPojo payload = buildOrderCreatedEvent(orderPojo);
        emitters.forEach((emitterId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name(EVENT_ORDER_CREATED)
                    .id(payload.getEventId())
                    .data(payload));
            } catch (Exception ex) {
                safeComplete(emitter);
                removeEmitter(emitterId, "publish failed: " + ex.getMessage());
            }
        });
    }

    @Override
    @Scheduled(fixedDelayString = "${monostudio.notifications.sse-heartbeat-ms:15000}")
    public void publishHeartbeat() {
        emitters.forEach((emitterId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name(EVENT_KEEP_ALIVE)
                    .data(Map.of("timestamp", Instant.now().toString())));
            } catch (Exception ex) {
                safeComplete(emitter);
                removeEmitter(emitterId, "heartbeat failed: " + ex.getMessage());
            }
        });
    }

    @Override
    public AdminNotificationEventPojo buildOrderCreatedEvent(OrderPojo orderPojo) {
        Instant createdAt = orderPojo.getDate() != null ? orderPojo.getDate() : Instant.now();
        Long orderId = orderPojo.getId() != null ? orderPojo.getId() : orderPojo.getBuyOrder();
        return AdminNotificationEventPojo.builder()
            .eventId(orderId + "-" + createdAt.toEpochMilli())
            .type(EVENT_ORDER_CREATED)
            .orderId(orderId)
            .orderCode(String.valueOf(orderPojo.getBuyOrder() != null ? orderPojo.getBuyOrder() : orderId))
            .createdAt(createdAt)
            .totalAmount(orderPojo.getTotalValue())
            .status(orderPojo.getStatus() != null ? orderPojo.getStatus() : ORDER_STATUS_PENDING)
            .build();
    }

    private void removeEmitter(String emitterId, String reason) {
        emitters.remove(emitterId);
        logger.debug("Removed SSE emitter {} ({})", emitterId, reason);
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // Ignore cleanup failures for disconnected clients.
        }
    }
}
