package org.monostudio.api.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.ShipmentTrackingWebhookPayload;
import org.monostudio.api.services.OrdersProcessService;
import org.monostudio.api.services.ShipmentTrackingService;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.ShipmentTracking;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.ShipmentTrackingRepository;
import org.monostudio.search.models.ShipmentTrackingDocument;
import org.monostudio.search.repositories.ShipmentTrackingSearchRepository;
import org.monostudio.shipping.kafka.KafkaShippingProducer;
import org.monostudio.shipping.kafka.ShipmentTrackingEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShipmentTrackingServiceImpl implements ShipmentTrackingService {

    private final ShipmentTrackingRepository trackingRepository;
    private final OrdersRepository ordersRepository;
    private final KafkaShippingProducer kafkaShippingProducer;
    private final OrdersProcessService ordersProcessService;
    private final ShipmentTrackingSearchRepository searchRepository;

    @Override
    @Transactional
    public ShipmentTracking record(ShipmentTrackingWebhookPayload payload) {
        Order order = ordersRepository.findById(payload.getOrder_id())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + payload.getOrder_id()));

        // 1. Save history to PostgreSQL
        ShipmentTracking tracking = ShipmentTracking.builder()
                .order(order)
                .trackingNumber(payload.getTracking_number())
                .shipperCode(payload.getShipper_code())
                .status(payload.getStatus())
                .location(payload.getLocation())
                .description(payload.getDescription())
                .eventTime(payload.getEvent_time())
                .build();
        
        tracking = trackingRepository.save(tracking);

        // 2. Update order if it's the first time receiving tracking or if status is special
        if (order.getTrackingNumber() == null) {
            ordersRepository.setTracking(order.getId(), payload.getTracking_number(), payload.getShipper_code());
        }

        applyWorkflowTransition(order.getId(), payload.getStatus());

        // 4. Publish to Kafka for Elasticsearch and other consumers
        ShipmentTrackingEvent event = ShipmentTrackingEvent.builder()
                .trackingId(tracking.getId())
                .orderId(order.getId())
                .trackingNumber(tracking.getTrackingNumber())
                .shipperCode(tracking.getShipperCode())
                .status(tracking.getStatus())
                .location(tracking.getLocation())
                .description(tracking.getDescription())
                .eventTime(tracking.getEventTime())
                .build();
        
        kafkaShippingProducer.publish(event);

        return tracking;
    }

    @Override
    public List<ShipmentTrackingDocument> getByTrackingNumber(String trackingNumber) {
        return searchRepository.findByTrackingNumberOrderByEventTimeDesc(trackingNumber);
    }

    @Override
    public List<ShipmentTrackingDocument> getByOrderId(Long orderId) {
        return searchRepository.findByOrderIdOrderByEventTimeDesc(orderId);
    }

    private void applyWorkflowTransition(Long orderId, String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return;
        }
        String normalized = rawStatus.trim().toUpperCase(Locale.ROOT);
        OrderPojo pojo = OrderPojo.builder().id(orderId).build();

        try {
            if (normalized.contains("RETURN")) {
                ordersProcessService.markAsReturned(pojo);
                return;
            }
            if (normalized.equals("DELIVERED") || normalized.equals("DELIVERY_COMPLETE") || normalized.equals("COMPLETED")) {
                ordersProcessService.markAsCompleted(pojo);
                return;
            }
            if (normalized.contains("DELIVERY_FAILED") || normalized.equals("FAILED") || normalized.contains("UNDELIVERABLE")) {
                ordersProcessService.markAsDeliveryFailed(pojo);
                return;
            }
            if (normalized.contains("CANCELLED") || normalized.contains("RECALL")) {
                ordersProcessService.markAsDeliveryCancelled(pojo);
                return;
            }
            if (normalized.equals("IN_TRANSIT") || normalized.equals("ON_ROUTE") || normalized.equals("OUT_FOR_DELIVERY")) {
                ordersProcessService.markAsDeliveryOnRoute(pojo);
            }
        } catch (Exception e) {
            log.error("Failed to apply shipping status {} to order {}: {}", rawStatus, orderId, e.getMessage());
        }
    }
}
