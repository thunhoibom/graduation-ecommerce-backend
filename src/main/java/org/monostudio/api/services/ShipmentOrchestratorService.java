package org.monostudio.api.services;

public interface ShipmentOrchestratorService {
    void requestShipmentCreation(Long orderId);

    int processPendingRetries();
}
