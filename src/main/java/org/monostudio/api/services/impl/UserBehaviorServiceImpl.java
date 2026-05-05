package org.monostudio.api.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.monostudio.api.models.BehaviorEventRequestPojo;
import org.monostudio.api.services.UserBehaviorService;
import org.monostudio.jpa.entities.UserBehaviorEvent;
import org.monostudio.jpa.repositories.UserBehaviorEventsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBehaviorServiceImpl implements UserBehaviorService {

    public static final String EVENT_SEARCH_SUBMIT = "SEARCH_SUBMIT";
    public static final String EVENT_PRODUCT_VIEW = "PRODUCT_VIEW";

    private static final int MAX_PAYLOAD_CHARS = 4000;
    private static final int MAX_DEVICE_ID_LEN = 64;
    private static final int MAX_EVENTS_SCAN = 50;

    private final UserBehaviorEventsRepository userBehaviorEventsRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void recordBestEffort(BehaviorEventRequestPojo request) {
        try {
            if (request == null || request.getDeviceId() == null || request.getDeviceId().isBlank()) {
                return;
            }
            if (request.getEventType() == null || request.getEventType().isBlank()) {
                return;
            }
            String type = request.getEventType().trim();
            if (!EVENT_SEARCH_SUBMIT.equals(type) && !EVENT_PRODUCT_VIEW.equals(type)) {
                log.debug("Ignored behavior event type: {}", type);
                return;
            }
            String deviceId = request.getDeviceId().trim();
            if (deviceId.length() > MAX_DEVICE_ID_LEN) {
                deviceId = deviceId.substring(0, MAX_DEVICE_ID_LEN);
            }
            String json = objectMapper.writeValueAsString(
                request.getPayload() != null ? request.getPayload() : Map.of()
            );
            if (json.length() > MAX_PAYLOAD_CHARS) {
                log.debug("Behavior payload too large, skipped");
                return;
            }
            UserBehaviorEvent entity = UserBehaviorEvent.builder()
                .deviceId(deviceId)
                .customerId(request.getCustomerId())
                .eventType(type)
                .payload(json)
                .build();
            userBehaviorEventsRepository.save(entity);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize behavior payload: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to record user behavior: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> rankCategoryCodesForDevice(String deviceId, int maxCategories) {
        if (deviceId == null || deviceId.isBlank() || maxCategories <= 0) {
            return List.of();
        }
        List<UserBehaviorEvent> events = userBehaviorEventsRepository.findByDeviceIdOrderByCreatedAtDesc(
            deviceId.trim(),
            PageRequest.of(0, MAX_EVENTS_SCAN)
        );
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (UserBehaviorEvent e : events) {
            if (!EVENT_PRODUCT_VIEW.equals(e.getEventType())) {
                continue;
            }
            String code = extractCategoryCode(e.getPayload());
            if (code != null && !code.isBlank()) {
                ordered.add(code.trim());
            }
            if (ordered.size() >= maxCategories) {
                break;
            }
        }
        return new ArrayList<>(ordered);
    }

    private String extractCategoryCode(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.hasNonNull("categoryCode")) {
                return node.get("categoryCode").asText();
            }
        } catch (Exception e) {
            log.debug("Could not parse behavior payload: {}", e.getMessage());
        }
        return null;
    }
}
