package org.monostudio.api.controllers;

import lombok.RequiredArgsConstructor;
import org.monostudio.api.services.ShipmentTrackingService;
import org.monostudio.search.models.ShipmentTrackingDocument;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/tracking")
@RequiredArgsConstructor
public class PublicTrackingController {

    private final ShipmentTrackingService trackingService;

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<List<ShipmentTrackingDocument>> getTracking(@PathVariable String trackingNumber) {
        List<ShipmentTrackingDocument> logs = trackingService.getByTrackingNumber(trackingNumber);
        if (logs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<ShipmentTrackingDocument>> getTrackingByOrder(@PathVariable Long orderId) {
        List<ShipmentTrackingDocument> logs = trackingService.getByOrderId(orderId);
        if (logs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(logs);
    }
}
