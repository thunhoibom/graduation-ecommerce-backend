package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.FinanceCallbackLogItemPojo;
import org.monostudio.api.models.FinanceMismatchResolveRequest;
import org.monostudio.api.models.FinancePaymentItemPojo;
import org.monostudio.api.models.FinanceReconciliationMismatchPojo;
import org.monostudio.api.models.FinanceReconciliationSummaryPojo;
import org.monostudio.api.models.FinanceRefundOperationPojo;
import org.monostudio.api.models.FinanceRefundRetryItemPojo;
import org.monostudio.api.services.FinanceOperationsService;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/finance")
@Tag(name = "Admin Finance")
@PreAuthorize("isAuthenticated()")
public class AdminFinanceController {
    private final FinanceOperationsService financeOperationsService;

    @Autowired
    public AdminFinanceController(FinanceOperationsService financeOperationsService) {
        this.financeOperationsService = financeOperationsService;
    }

    @GetMapping("/payments")
    @Operation(summary = "List payment transactions for finance operations.")
    public DataPagePojo<FinancePaymentItemPojo> getPayments(
        @RequestParam(required = false) Long orderId,
        @RequestParam(required = false) String paymentStatus,
        @RequestParam(required = false) String orderStatus,
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return financeOperationsService.getPayments(orderId, paymentStatus, orderStatus, from, to, page, size);
    }

    @GetMapping("/refunds")
    @Operation(summary = "List refund operations for reconciliation and accounting.")
    public DataPagePojo<FinanceRefundOperationPojo> getRefunds(
        @RequestParam(required = false) Long orderId,
        @RequestParam(required = false) String status,
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return financeOperationsService.getRefundOperations(orderId, status, from, to, page, size);
    }

    @GetMapping("/refund-retries")
    @Operation(summary = "List refund retry queue entries.")
    public DataPagePojo<FinanceRefundRetryItemPojo> getRefundRetries(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long orderId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return financeOperationsService.getRefundRetries(status, orderId, page, size);
    }

    @PostMapping("/refund-retries/{id}/manual-retry")
    @Operation(summary = "Trigger manual retry for a refund queue entry.")
    public FinanceRefundRetryItemPojo manualRetry(@PathVariable Long id) throws Exception {
        return financeOperationsService.manualRetry(id);
    }

    @GetMapping("/payment-callback-logs")
    @Operation(summary = "List payment callback logs for audit and troubleshooting.")
    public DataPagePojo<FinanceCallbackLogItemPojo> getCallbackLogs(
        @RequestParam(required = false) Long orderId,
        @RequestParam(required = false) String result,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return financeOperationsService.getCallbackLogs(orderId, result, page, size);
    }

    @GetMapping("/reconciliation/summary")
    @Operation(summary = "Get reconciliation summary metrics.")
    public FinanceReconciliationSummaryPojo getReconciliationSummary(
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return financeOperationsService.getReconciliationSummary(from, to);
    }

    @GetMapping("/reconciliation/mismatches")
    @Operation(summary = "List reconciliation mismatches.")
    public DataPagePojo<FinanceReconciliationMismatchPojo> getMismatches(
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) Boolean resolved,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return financeOperationsService.getReconciliationMismatches(from, to, resolved, page, size);
    }

    @GetMapping("/reconciliation/settlement-export")
    @Operation(summary = "Export settlement summary and mismatches as CSV.")
    public ResponseEntity<byte[]> exportSettlementCsv(
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String csv = financeOperationsService.exportSettlementCsv(from, to);
        String fromLabel = from != null ? from.toString() : "all";
        String toLabel = to != null ? to.toString() : "now";
        String fileName = "finance-settlement-" + fromLabel + "-to-" + toLabel + ".csv";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .contentType(MediaType.TEXT_PLAIN)
            .body(csv.getBytes());
    }

    @PostMapping("/reconciliation/mismatches/{mismatchKey}/resolve")
    @Operation(summary = "Mark a mismatch as resolved with an accounting note.")
    public Map<String, Object> resolveMismatch(
        @PathVariable String mismatchKey,
        @RequestBody(required = false) FinanceMismatchResolveRequest request,
        Authentication authentication
    ) {
        String resolvedBy = authentication != null ? authentication.getName() : "system";
        String note = request != null ? request.getNote() : null;
        financeOperationsService.resolveMismatch(mismatchKey, note, resolvedBy);
        return Map.of("success", true);
    }
}
