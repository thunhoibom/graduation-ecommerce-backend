package org.monostudio.api.services;

import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.FinanceCallbackLogItemPojo;
import org.monostudio.api.models.FinancePaymentItemPojo;
import org.monostudio.api.models.FinanceReconciliationMismatchPojo;
import org.monostudio.api.models.FinanceReconciliationSummaryPojo;
import org.monostudio.api.models.FinanceRefundOperationPojo;
import org.monostudio.api.models.FinanceRefundRetryItemPojo;

import java.time.LocalDate;

public interface FinanceOperationsService {
    DataPagePojo<FinancePaymentItemPojo> getPayments(
        Long orderId,
        String paymentStatus,
        String orderStatus,
        LocalDate from,
        LocalDate to,
        int page,
        int size
    );

    DataPagePojo<FinanceRefundOperationPojo> getRefundOperations(
        Long orderId,
        String status,
        LocalDate from,
        LocalDate to,
        int page,
        int size
    );

    DataPagePojo<FinanceRefundRetryItemPojo> getRefundRetries(
        String status,
        Long orderId,
        int page,
        int size
    );

    DataPagePojo<FinanceCallbackLogItemPojo> getCallbackLogs(
        Long orderId,
        String result,
        int page,
        int size
    );

    FinanceReconciliationSummaryPojo getReconciliationSummary(LocalDate from, LocalDate to);

    DataPagePojo<FinanceReconciliationMismatchPojo> getReconciliationMismatches(
        LocalDate from,
        LocalDate to,
        Boolean resolved,
        int page,
        int size
    );

    String exportSettlementCsv(LocalDate from, LocalDate to);

    FinanceRefundRetryItemPojo manualRetry(Long queueId) throws Exception;

    void resolveMismatch(String mismatchKey, String note, String resolvedBy);
}
