package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.monostudio.api.models.ShipmentTrackingWebhookPayload;
import org.monostudio.api.services.ShipmentTrackingService;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.ShipmentTracking;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.shipping.GhnStatusMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/mock/shipping")
@Tag(name = "Public shipping mock")
@Slf4j
@RequiredArgsConstructor
public class PublicShippingMockController {
    private final ShipmentTrackingService trackingService;
    private final GhnStatusMapper ghnStatusMapper;
    private final OrdersRepository ordersRepository;

    @PostMapping("/tracking")
    @Operation(summary = "Mock shipping tracking webhook for QA/internal testing")
    public ResponseEntity<Map<String, Object>> mockTracking(@RequestBody ShipmentTrackingWebhookPayload payload) {
        log.info("Mock shipping tracking event: order={} tracking={} status={}",
            payload.getOrder_id(), payload.getTracking_number(), payload.getStatus());

        ShipmentTracking saved = trackingService.record(payload);
        GhnStatusMapper.WorkflowAction action = ghnStatusMapper.map(payload.getStatus());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Mock tracking accepted");
        response.put("trackingId", saved.getId());
        response.put("orderId", saved.getOrder() != null ? saved.getOrder().getId() : payload.getOrder_id());
        response.put("trackingNumber", saved.getTrackingNumber());
        response.put("rawStatus", saved.getStatus());
        response.put("mappedAction", action.name());
        response.put("eventTime", saved.getEventTime());
        response.put("location", saved.getLocation());
        response.put("description", saved.getDescription());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders")
    @Operation(summary = "List recent orders for mock screen auto-fill")
    public ResponseEntity<List<Map<String, Object>>> listRecentOrders(
        @RequestParam(defaultValue = "20") int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<Order> recentOrders = ordersRepository.findRecentOrders(safeLimit);
        List<Map<String, Object>> response = new ArrayList<>(recentOrders.size());
        for (Order order : recentOrders) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", order.getId());
            row.put("date", order.getDate());
            row.put("fulfillmentStatus", order.getFulfillmentStatus());
            row.put("paymentStatus", order.getPaymentStatus());
            row.put("trackingNumber", order.getTrackingNumber());
            row.put("totalValue", order.getTotalValue());
            response.add(row);
        }
        return ResponseEntity.ok(response);
    }
}
