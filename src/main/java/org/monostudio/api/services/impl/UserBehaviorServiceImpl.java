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
import java.util.Comparator;
import java.util.HashMap;
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
    public static final String EVENT_ADD_TO_CART = "ADD_TO_CART";
    public static final String EVENT_BEGIN_CHECKOUT = "BEGIN_CHECKOUT";
    public static final String EVENT_PURCHASE = "PURCHASE";

    private static final int MAX_PAYLOAD_CHARS = 4000;
    private static final int MAX_DEVICE_ID_LEN = 64;
    private static final int MAX_EVENTS_SCAN = 200;

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
            if (!isSupportedEventType(type)) {
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
        return rankCategoryCodes(events, maxCategories);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> rankCategoryCodesForCustomer(Long customerId, int maxCategories) {
        if (customerId == null || maxCategories <= 0) {
            return List.of();
        }
        List<UserBehaviorEvent> events = userBehaviorEventsRepository.findByCustomerIdOrderByCreatedAtDesc(
            customerId,
            PageRequest.of(0, MAX_EVENTS_SCAN)
        );
        return rankCategoryCodes(events, maxCategories);
    }

    private List<String> rankCategoryCodes(List<UserBehaviorEvent> events, int maxCategories) {
        Map<String, Double> scoreByCategory = new HashMap<>();
        int rank = 0;
        for (UserBehaviorEvent e : events) {
            String type = e.getEventType() == null ? "" : e.getEventType().trim();
            double eventWeight = eventWeight(type);
            if (eventWeight <= 0) {
                continue;
            }
            List<String> categoryCodes = extractCategoryCodes(e.getPayload());
            if (categoryCodes.isEmpty()) {
                continue;
            }

            // Recency-decay without extra infra: newer events get stronger signal.
            double recencyFactor = 1.0d - (Math.min(rank, MAX_EVENTS_SCAN - 1) / (double) MAX_EVENTS_SCAN);
            double combinedWeight = eventWeight * Math.max(0.2d, recencyFactor);
            for (String code : categoryCodes) {
                scoreByCategory.merge(code, combinedWeight, Double::sum);
            }
            rank++;
        }
        return scoreByCategory.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
            .map(Map.Entry::getKey)
            .limit(maxCategories)
            .toList();
    }

    private static boolean isSupportedEventType(String type) {
        return EVENT_SEARCH_SUBMIT.equals(type)
            || EVENT_PRODUCT_VIEW.equals(type)
            || EVENT_ADD_TO_CART.equals(type)
            || EVENT_BEGIN_CHECKOUT.equals(type)
            || EVENT_PURCHASE.equals(type);
    }

    private static double eventWeight(String eventType) {
        return switch (eventType) {
            case EVENT_PURCHASE -> 3.0d;
            case EVENT_ADD_TO_CART -> 2.2d;
            case EVENT_BEGIN_CHECKOUT -> 1.6d;
            case EVENT_PRODUCT_VIEW -> 1.0d;
            case EVENT_SEARCH_SUBMIT -> 0.5d;
            default -> 0d;
        };
    }

    private List<String> extractCategoryCodes(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            List<String> out = new ArrayList<>();
            if (node.hasNonNull("categoryCodes") && node.get("categoryCodes").isArray()) {
                for (JsonNode entry : node.get("categoryCodes")) {
                    if (entry != null && !entry.asText().isBlank()) {
                        out.add(entry.asText().trim());
                    }
                }
            }
            if (node.hasNonNull("categoryCode")) {
                String code = node.get("categoryCode").asText();
                if (!code.isBlank()) {
                    out.add(code.trim());
                }
            }
            Set<String> unique = new LinkedHashSet<>(out);
            return new ArrayList<>(unique);
        } catch (Exception e) {
            log.debug("Could not parse behavior payload: {}", e.getMessage());
        }
        return List.of();
    }
}
