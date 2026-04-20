package org.monostudio.api.services;

import org.monostudio.api.models.ReturnRequestPojo;
import org.monostudio.common.exceptions.BadInputException;

import jakarta.persistence.EntityNotFoundException;
import java.util.Map;

public interface ReturnRequestService {
    /**
     * Creates a new return request from a customer.
     *
     * @param input Return request data
     * @return The created return request
     * @throws BadInputException When input data is invalid
     */
    ReturnRequestPojo createReturnRequest(ReturnRequestPojo input) throws BadInputException;

    /**
     * Approves a return request, releases reserved stock, and sets it to REFUND_PROCESSING.
     *
     * @param id Return request ID
     * @param adminNotes Optional admin notes
     * @param refundAmount The refund amount to process
     * @return The updated return request
     * @throws EntityNotFoundException When the return request is not found
     * @throws BadInputException When the return request is not in a valid state
     */
    ReturnRequestPojo approveReturnRequest(Long id, String adminNotes, Integer refundAmount)
        throws EntityNotFoundException, BadInputException;

    /**
     * Rejects a return request.
     *
     * @param id Return request ID
     * @param adminNotes Admin notes explaining the rejection
     * @return The updated return request
     * @throws EntityNotFoundException When the return request is not found
     * @throws BadInputException When the return request is not in a valid state
     */
    ReturnRequestPojo rejectReturnRequest(Long id, String adminNotes)
        throws EntityNotFoundException, BadInputException;

    /**
     * Marks a return request as received (items have been returned to warehouse).
     *
     * @param id Return request ID
     * @param adminNotes Optional admin notes
     * @return The updated return request
     * @throws EntityNotFoundException When the return request is not found
     * @throws BadInputException When the return request is not in a valid state
     */
    ReturnRequestPojo markAsReceived(Long id, String adminNotes)
        throws EntityNotFoundException, BadInputException;

    /**
     * Completes the refund for a return request.
     *
     * @param id Return request ID
     * @param adminNotes Optional admin notes
     * @return The updated return request
     * @throws EntityNotFoundException When the return request is not found
     * @throws BadInputException When the return request is not in a valid state
     */
    ReturnRequestPojo completeRefund(Long id, String adminNotes)
        throws EntityNotFoundException, BadInputException;

    /**
     * Adds a tracking number to a return request.
     *
     * @param id Return request ID
     * @param trackingNumber The tracking number
     * @return The updated return request
     * @throws EntityNotFoundException When the return request is not found
     * @throws BadInputException When the return request is not in a valid state
     */
    ReturnRequestPojo addTrackingNumber(Long id, String trackingNumber)
        throws EntityNotFoundException, BadInputException;

    /**
     * Cancels a return request (only allowed when PENDING).
     *
     * @param id Return request ID
     * @return The updated return request
     * @throws EntityNotFoundException When the return request is not found
     * @throws BadInputException When the return request is not in a valid state
     */
    ReturnRequestPojo cancelReturnRequest(Long id)
        throws EntityNotFoundException, BadInputException;

    /**
     * Starts the refund process for a return request.
     * Transitions the request to REFUND_PROCESSING.
     *
     * @param id Return request ID
     * @param adminNotes Optional admin notes
     * @return The updated return request
     * @throws EntityNotFoundException When the return request is not found
     * @throws BadInputException When the return request is not in a valid state
     */
    ReturnRequestPojo startRefund(Long id, String adminNotes)
        throws EntityNotFoundException, BadInputException;

    /**
     * Adds an admin note to a return request.
     *
     * @param id Return request ID
     * @param note The note to add
     * @return The updated return request
     * @throws EntityNotFoundException When the return request is not found
     * @throws BadInputException When the return request is not in a valid state
     */
    ReturnRequestPojo addNote(Long id, String note)
        throws EntityNotFoundException, BadInputException;
}
