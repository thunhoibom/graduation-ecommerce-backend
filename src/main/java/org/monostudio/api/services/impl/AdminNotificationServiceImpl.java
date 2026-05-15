package org.monostudio.api.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.monostudio.api.models.AdminNotificationEventPojo;
import org.monostudio.api.models.ImagePojo;
import org.monostudio.api.models.OrderDetailPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.services.AdminNotificationService;
import org.monostudio.jpa.repositories.VariantImagesRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.monostudio.config.Constants.ORDER_FULFILLMENT_STATUS_PENDING;

@Service
public class AdminNotificationServiceImpl implements AdminNotificationService {

    @FunctionalInterface
    private interface SseSendAction {
        void send() throws IOException;
    }

    private static final Logger logger = LoggerFactory.getLogger(AdminNotificationServiceImpl.class);
    private static final String EVENT_ORDER_CREATED = "order.created";
    private static final String EVENT_KEEP_ALIVE = "keep-alive";

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final long emitterTimeoutMs;
    private final VariantImagesRepository variantImagesRepository;

    public AdminNotificationServiceImpl(
        @Value("${monostudio.notifications.sse-timeout-ms:1800000}") long emitterTimeoutMs,
        VariantImagesRepository variantImagesRepository
    ) {
        this.emitterTimeoutMs = emitterTimeoutMs;
        this.variantImagesRepository = variantImagesRepository;
    }

    @Override
    public SseEmitter subscribe(String principalKey) {
        String emitterId = principalKey + ":" + UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(emitterTimeoutMs);
        emitters.put(emitterId, emitter);

        emitter.onCompletion(() -> removeEmitter(emitterId, "completed"));
        emitter.onTimeout(() -> removeEmitter(emitterId, "timed out"));
        emitter.onError(ex -> removeEmitter(emitterId, "error: " + ex.getMessage()));

        // Defer the first write until Spring has started async processing for this emitter.
        CompletableFuture.runAsync(() -> sendOrDropEmitter(
            emitterId,
            emitter,
            () -> emitter.send(SseEmitter.event()
                .name("connected")
                .id(emitterId)
                .data(Map.of("status", "ok", "timestamp", Instant.now().toString())))
        ));

        logger.info("Admin SSE subscribed: {} (active emitters={})", emitterId, emitters.size());
        return emitter;
    }

    @Override
    public void publishOrderCreated(OrderPojo orderPojo) {
        AdminNotificationEventPojo payload = buildOrderCreatedEvent(orderPojo);
        for (Map.Entry<String, SseEmitter> entry : new ArrayList<>(emitters.entrySet())) {
            String emitterId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            sendOrDropEmitter(
                emitterId,
                emitter,
                () -> emitter.send(SseEmitter.event()
                    .name(EVENT_ORDER_CREATED)
                    .id(payload.getEventId())
                    .data(payload))
            );
        }
    }

    @Override
    @Scheduled(fixedDelayString = "${monostudio.notifications.sse-heartbeat-ms:15000}")
    public void publishHeartbeat() {
        for (Map.Entry<String, SseEmitter> entry : new ArrayList<>(emitters.entrySet())) {
            String emitterId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            sendOrDropEmitter(
                emitterId,
                emitter,
                () -> emitter.send(SseEmitter.event()
                    .name(EVENT_KEEP_ALIVE)
                    .data(Map.of("timestamp", Instant.now().toString())))
            );
        }
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
            .status(orderPojo.getStatus() != null ? orderPojo.getStatus() : ORDER_FULFILLMENT_STATUS_PENDING)
            .previewImageUrl(resolvePreviewImageUrl(orderPojo))
            .build();
    }

    private String resolvePreviewImageUrl(OrderPojo orderPojo) {
        if (orderPojo.getDetails() == null) {
            return null;
        }
        for (OrderDetailPojo detail : orderPojo.getDetails()) {
            String variantImageUrl = resolveVariantImageUrl(detail);
            if (StringUtils.isNotBlank(variantImageUrl)) {
                return variantImageUrl;
            }
        }
        return null;
    }

    private String resolveVariantImageUrl(OrderDetailPojo detail) {
        if (detail.getVariant() != null) {
            if (StringUtils.isNotBlank(detail.getVariant().getPrimaryImageUrl())) {
                return detail.getVariant().getPrimaryImageUrl();
            }
            if (detail.getVariant().getImages() != null) {
                for (ImagePojo image : detail.getVariant().getImages()) {
                    if (image != null && StringUtils.isNotBlank(image.getUrl())) {
                        return image.getUrl();
                    }
                }
            }
        }
        if (detail.getVariantId() == null) {
            return null;
        }
        return variantImagesRepository.findPrimaryByVariantId(detail.getVariantId())
            .map(variantImage -> variantImage.getImage().getUrl())
            .orElseGet(() -> {
                var variantImages = variantImagesRepository.deepFindByVariantId(detail.getVariantId());
                if (variantImages.isEmpty()) {
                    return null;
                }
                return variantImages.get(0).getImage().getUrl();
            });
    }

    private void removeEmitter(String emitterId, String reason) {
        emitters.remove(emitterId);
        logger.debug("Removed SSE emitter {} ({})", emitterId, reason);
    }

    /**
     * Sends one SSE event; on client disconnect or completed emitter, completes quietly without ERROR logs.
     * <p>
     * Client closes (tab refresh, navigation, proxy idle timeout) commonly surface as {@link IOException}
     * ("connection was aborted", "Broken pipe"); those are expected and not application errors.
     */
    private void sendOrDropEmitter(String emitterId, SseEmitter emitter, SseSendAction action) {
        try {
            action.send();
        } catch (AsyncRequestNotUsableException ex) {
            logBenignDisconnect(emitterId, ex);
            safeComplete(emitter);
            removeEmitter(emitterId, "client disconnected");
        } catch (IOException ex) {
            logBenignDisconnect(emitterId, ex);
            safeComplete(emitter);
            removeEmitter(emitterId, "client disconnected");
        } catch (UncheckedIOException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception exception) {
                logBenignDisconnect(emitterId, exception);
            } else {
                logger.debug("SSE UncheckedIOException {}: {}", emitterId, ex.getMessage());
            }
            safeComplete(emitter);
            removeEmitter(emitterId, "client disconnected");
        } catch (IllegalStateException ex) {
            logger.trace("SSE emitter {} already completed: {}", emitterId, ex.getMessage());
            safeComplete(emitter);
            removeEmitter(emitterId, "illegal state after send");
        } catch (RuntimeException ex) {
            if (isBenignDisconnect(ex)) {
                logBenignDisconnect(emitterId, ex);
                safeComplete(emitter);
                removeEmitter(emitterId, "client disconnected");
                return;
            }
            throw ex;
        }
    }

    private void logBenignDisconnect(String emitterId, Exception ex) {
        if (isBenignDisconnect(ex)) {
            logger.trace("SSE client gone {}: {}", emitterId, ex.getMessage());
            return;
        }
        logger.debug("SSE disconnect {}: {}", emitterId, ex.getMessage());
    }

    private boolean isBenignDisconnect(Throwable ex) {
        if (ex instanceof AsyncRequestNotUsableException) {
            return true;
        }
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        if (message.contains("aborted")
            || message.contains("Broken pipe")
            || message.contains("connection reset")
            || message.contains("Connection reset")
            || message.contains("An established connection was aborted")
            || message.contains("Response not usable after response errors")) {
            return true;
        }
        Throwable cause = ex.getCause();
        return cause != null && cause != ex && isBenignDisconnect(cause);
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // Ignore cleanup failures for disconnected clients.
        }
    }
}
