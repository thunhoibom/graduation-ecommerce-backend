package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.monostudio.api.models.ReturnRequestPojo;
import org.monostudio.api.services.ReturnRequestService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ReturnRequest;
import org.monostudio.jpa.repositories.ReturnRequestsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/public/mock/returns")
@Tag(name = "Public return-request mock")
@Slf4j
@RequiredArgsConstructor
public class PublicReturnRequestMockController {
    private final ReturnRequestsRepository returnRequestsRepository;
    private final ReturnRequestService returnRequestService;

    @GetMapping("/requests")
    @Operation(summary = "List recent return requests for mock tools")
    public ResponseEntity<List<Map<String, Object>>> listRecentRequests(
        @RequestParam(defaultValue = "20") int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<ReturnRequest> requests = returnRequestsRepository.findRecentRequests(safeLimit);
        List<Map<String, Object>> response = new ArrayList<>(requests.size());
        for (ReturnRequest request : requests) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", request.getId());
            row.put("orderId", request.getOrder() != null ? request.getOrder().getId() : null);
            row.put("status", request.getStatus() != null ? request.getStatus().name() : null);
            row.put("trackingNumber", request.getTrackingNumber());
            row.put("refundAmount", request.getRefundAmount());
            row.put("refundMethod", request.getRefundMethod() != null ? request.getRefundMethod().name() : null);
            row.put("date", request.getDate());
            row.put("lastModified", request.getLastModified());
            response.add(row);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests/{id}/simulate")
    @Operation(summary = "Simulate return-request workflow action for QA")
    public ResponseEntity<Map<String, Object>> simulateAction(
        @PathVariable Long id,
        @RequestBody Map<String, Object> payload
    ) throws EntityNotFoundException, BadInputException {
        String action = payload.get("action") instanceof String value ? value : null;
        if (action == null || action.isBlank()) {
            throw new BadInputException("Missing action");
        }

        String normalizedAction = action.trim().toUpperCase(Locale.ROOT);
        String trackingNumber = payload.get("trackingNumber") instanceof String value ? value : null;
        String adminNotes = payload.get("adminNotes") instanceof String value ? value : null;
        Integer refundAmount = payload.get("refundAmount") instanceof Number value ? value.intValue() : null;

        ReturnRequestPojo result;
        switch (normalizedAction) {
            case "APPROVE" -> result = returnRequestService.approveReturnRequest(id, adminNotes, refundAmount);
            case "SET_TRACKING" -> result = returnRequestService.addTrackingNumber(id, trackingNumber);
            case "RECEIVE" -> result = returnRequestService.markAsReceived(id, adminNotes);
            case "START_REFUND" -> result = returnRequestService.startRefund(id, adminNotes);
            case "COMPLETE_REFUND" -> result = returnRequestService.completeRefund(id, adminNotes, null, null, null);
            case "CANCEL" -> result = returnRequestService.cancelReturnRequest(id);
            case "REJECT" -> result = returnRequestService.rejectReturnRequest(id, adminNotes);
            default -> throw new BadInputException("Unsupported action: " + action);
        }

        log.info("Mock return action={} id={} -> status={}", normalizedAction, id, result.getStatus());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Mock return action applied");
        response.put("returnRequestId", result.getId());
        response.put("orderId", result.getOrderId());
        response.put("status", result.getStatus());
        response.put("trackingNumber", result.getTrackingNumber());
        response.put("refundAmount", result.getRefundAmount());
        response.put("refundMethod", result.getRefundMethod());
        response.put("lastModified", result.getLastModified());
        return ResponseEntity.ok(response);
    }
}
