package org.monostudio.api.services;

/**
 * Orchestrates the creation of GHN return-shipments (reverse logistics).
 *
 * <p>When an admin approves a return request, this service is called to create
 * a GHN shipping order where the customer is the sender and the shop warehouse
 * is the destination. GHN responds with a tracking number that is stored on the
 * {@link org.monostudio.jpa.entities.ReturnRequest} so both admin and customer
 * can monitor the return journey.</p>
 *
 * <p>If the GHN API is temporarily unavailable, the request is enqueued in
 * {@link org.monostudio.jpa.entities.ReturnShipmentDispatchQueue} and retried
 * automatically via a scheduled job with exponential backoff.</p>
 */
public interface ReturnShipmentService {

    /**
     * Immediately attempt to create a GHN return-shipment for the given return request.
     * If the API call fails, the request is automatically enqueued for retry.
     *
     * @param returnRequestId ID of the return request to create a shipment for
     */
    void requestReturnShipmentCreation(Long returnRequestId);

    /**
     * Process all pending retry entries that are due.
     *
     * @return Number of entries successfully processed
     */
    int processPendingRetries();
}
