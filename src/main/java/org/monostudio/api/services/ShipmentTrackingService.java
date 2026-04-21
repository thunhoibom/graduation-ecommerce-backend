package org.monostudio.api.services;

import org.monostudio.api.models.ShipmentTrackingWebhookPayload;
import org.monostudio.jpa.entities.ShipmentTracking;
import org.monostudio.search.models.ShipmentTrackingDocument;

import java.util.List;

public interface ShipmentTrackingService {
    ShipmentTracking record(ShipmentTrackingWebhookPayload payload);
    List<ShipmentTrackingDocument> getByTrackingNumber(String trackingNumber);
    List<ShipmentTrackingDocument> getByOrderId(Long orderId);
}
