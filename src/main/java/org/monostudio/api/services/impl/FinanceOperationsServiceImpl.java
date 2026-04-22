package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.FinanceCallbackLogItemPojo;
import org.monostudio.api.models.FinancePaymentItemPojo;
import org.monostudio.api.models.FinanceReconciliationMismatchPojo;
import org.monostudio.api.models.FinanceReconciliationSummaryPojo;
import org.monostudio.api.models.FinanceRefundOperationPojo;
import org.monostudio.api.models.FinanceRefundRetryItemPojo;
import org.monostudio.api.services.FinanceOperationsService;
import org.monostudio.api.services.RefundRetryService;
import org.monostudio.config.Constants;
import org.monostudio.jpa.entities.FinanceMismatchResolution;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.PaymentCallbackLog;
import org.monostudio.jpa.entities.RefundRetryQueue;
import org.monostudio.jpa.entities.ReturnRequest;
import org.monostudio.jpa.repositories.FinanceMismatchResolutionRepository;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.PaymentCallbackLogRepository;
import org.monostudio.jpa.repositories.RefundRetryQueueRepository;
import org.monostudio.jpa.repositories.ReturnRequestsRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class FinanceOperationsServiceImpl
    implements FinanceOperationsService {

    private static final Set<String> SUCCESS_ORDER_STATUSES = Set.of(
        Constants.ORDER_STATUS_PAID_CONFIRMED,
        Constants.ORDER_STATUS_COMPLETED,
        Constants.ORDER_STATUS_PAID_UNCONFIRMED
    );

    private static final Set<String> FAILED_ORDER_STATUSES = Set.of(
        Constants.ORDER_STATUS_PAYMENT_FAILED,
        Constants.ORDER_STATUS_PAYMENT_CANCELLED
    );

    private final OrdersRepository ordersRepository;
    private final ReturnRequestsRepository returnRequestsRepository;
    private final RefundRetryQueueRepository refundRetryQueueRepository;
    private final PaymentCallbackLogRepository paymentCallbackLogRepository;
    private final FinanceMismatchResolutionRepository financeMismatchResolutionRepository;
    private final RefundRetryService refundRetryService;

    @Autowired
    public FinanceOperationsServiceImpl(
        OrdersRepository ordersRepository,
        ReturnRequestsRepository returnRequestsRepository,
        RefundRetryQueueRepository refundRetryQueueRepository,
        PaymentCallbackLogRepository paymentCallbackLogRepository,
        FinanceMismatchResolutionRepository financeMismatchResolutionRepository,
        RefundRetryService refundRetryService
    ) {
        this.ordersRepository = ordersRepository;
        this.returnRequestsRepository = returnRequestsRepository;
        this.refundRetryQueueRepository = refundRetryQueueRepository;
        this.paymentCallbackLogRepository = paymentCallbackLogRepository;
        this.financeMismatchResolutionRepository = financeMismatchResolutionRepository;
        this.refundRetryService = refundRetryService;
    }

    @Override
    public DataPagePojo<FinancePaymentItemPojo> getPayments(
        Long orderId,
        String paymentStatus,
        String orderStatus,
        LocalDate from,
        LocalDate to,
        int page,
        int size
    ) {
        Instant fromInstant = toInstant(from, true);
        Instant toInstant = toInstant(to, false);
        List<Order> orders = ordersRepository.findAll();
        Map<String, PaymentCallbackLog> callbackByToken = getCallbackByToken(orders);

        List<FinancePaymentItemPojo> filtered = orders.stream()
            .filter(order -> order.getDate() != null
                && !order.getDate().isBefore(fromInstant)
                && !order.getDate().isAfter(toInstant))
            .filter(order -> orderId == null || Objects.equals(order.getId(), orderId))
            .map(order -> toPaymentItem(order, callbackByToken.get(order.getTransactionToken())))
            .filter(item -> !StringUtils.hasText(paymentStatus)
                || item.getPaymentStatus().equalsIgnoreCase(paymentStatus))
            .filter(item -> !StringUtils.hasText(orderStatus)
                || (item.getOrderStatus() != null && item.getOrderStatus().equalsIgnoreCase(orderStatus)))
            .sorted(Comparator.comparing(FinancePaymentItemPojo::getProcessedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(FinancePaymentItemPojo::getOrderId, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());

        return paginate(filtered, page, size);
    }

    @Override
    public DataPagePojo<FinanceRefundOperationPojo> getRefundOperations(
        Long orderId,
        String status,
        LocalDate from,
        LocalDate to,
        int page,
        int size
    ) {
        Instant fromInstant = toInstant(from, true);
        Instant toInstant = toInstant(to, false);

        List<FinanceRefundOperationPojo> filtered = returnRequestsRepository.findAll().stream()
            .filter(request -> request.getDate() != null
                && !request.getDate().isBefore(fromInstant)
                && !request.getDate().isAfter(toInstant))
            .filter(request -> orderId == null || Objects.equals(request.getOrder().getId(), orderId))
            .filter(request -> !StringUtils.hasText(status)
                || request.getStatus().name().equalsIgnoreCase(status))
            .map(this::toRefundItem)
            .sorted(Comparator.comparing(FinanceRefundOperationPojo::getLastModified, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());

        return paginate(filtered, page, size);
    }

    @Override
    public DataPagePojo<FinanceRefundRetryItemPojo> getRefundRetries(
        String status,
        Long orderId,
        int page,
        int size
    ) {
        List<FinanceRefundRetryItemPojo> filtered = refundRetryQueueRepository.findAll().stream()
            .filter(queue -> orderId == null || Objects.equals(queue.getOrder().getId(), orderId))
            .filter(queue -> !StringUtils.hasText(status) || queue.getStatus().name().equalsIgnoreCase(status))
            .map(this::toRetryItem)
            .sorted(Comparator.comparing(FinanceRefundRetryItemPojo::getNextRetryAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());

        return paginate(filtered, page, size);
    }

    @Override
    public DataPagePojo<FinanceCallbackLogItemPojo> getCallbackLogs(
        Long orderId,
        String result,
        int page,
        int size
    ) {
        List<FinanceCallbackLogItemPojo> filtered = paymentCallbackLogRepository
            .findAllByOrderByProcessedAtDesc(PageRequest.of(0, 2000))
            .stream()
            .filter(log -> orderId == null || Objects.equals(log.getOrderId(), orderId))
            .filter(log -> !StringUtils.hasText(result) || log.getResult().name().equalsIgnoreCase(result))
            .map(this::toCallbackItem)
            .collect(Collectors.toList());

        return paginate(filtered, page, size);
    }

    @Override
    public FinanceReconciliationSummaryPojo getReconciliationSummary(LocalDate from, LocalDate to) {
        List<FinancePaymentItemPojo> payments = getPayments(null, null, null, from, to, 1, 10000).getItems()
            .stream().toList();
        List<FinanceRefundOperationPojo> refunds = getRefundOperations(null, null, from, to, 1, 10000).getItems()
            .stream().toList();
        List<FinanceReconciliationMismatchPojo> mismatches = buildMismatches(from, to);

        long grossPaid = payments.stream()
            .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()))
            .mapToLong(FinancePaymentItemPojo::getOrderTotal)
            .sum();
        long refundAmount = refunds.stream()
            .filter(r -> "REFUND_COMPLETED".equalsIgnoreCase(r.getStatus()))
            .map(FinanceRefundOperationPojo::getRefundAmount)
            .filter(Objects::nonNull)
            .mapToLong(Integer::longValue)
            .sum();
        long failedPaymentCount = payments.stream()
            .filter(p -> "FAILED".equalsIgnoreCase(p.getPaymentStatus())
                || "CANCELLED".equalsIgnoreCase(p.getPaymentStatus()))
            .count();
        long unresolvedMismatchCount = mismatches.stream().filter(item -> !item.isResolved()).count();

        return FinanceReconciliationSummaryPojo.builder()
            .grossPaidAmount(grossPaid)
            .refundAmount(refundAmount)
            .netAmount(grossPaid - refundAmount)
            .failedPaymentCount(failedPaymentCount)
            .unresolvedMismatchCount(unresolvedMismatchCount)
            .build();
    }

    @Override
    public DataPagePojo<FinanceReconciliationMismatchPojo> getReconciliationMismatches(
        LocalDate from,
        LocalDate to,
        Boolean resolved,
        int page,
        int size
    ) {
        List<FinanceReconciliationMismatchPojo> data = buildMismatches(from, to).stream()
            .filter(item -> resolved == null || item.isResolved() == resolved)
            .sorted(Comparator.comparing(FinanceReconciliationMismatchPojo::getResolvedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(FinanceReconciliationMismatchPojo::getOrderId, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());

        return paginate(data, page, size);
    }

    @Override
    public String exportSettlementCsv(LocalDate from, LocalDate to) {
        FinanceReconciliationSummaryPojo summary = getReconciliationSummary(from, to);
        List<FinanceReconciliationMismatchPojo> mismatches = getReconciliationMismatches(from, to, null, 1, 10000)
            .getItems()
            .stream()
            .toList();

        String fromText = from != null ? from.toString() : "";
        String toText = to != null ? to.toString() : "";

        StringBuilder builder = new StringBuilder();
        builder.append("from,to,grossPaidAmount,refundAmount,netAmount,failedPaymentCount,unresolvedMismatchCount\n");
        builder.append(csv(fromText)).append(',')
            .append(csv(toText)).append(',')
            .append(summary.getGrossPaidAmount()).append(',')
            .append(summary.getRefundAmount()).append(',')
            .append(summary.getNetAmount()).append(',')
            .append(summary.getFailedPaymentCount()).append(',')
            .append(summary.getUnresolvedMismatchCount()).append('\n');

        builder.append('\n');
        builder.append("mismatchKey,orderId,type,severity,resolved,resolutionNote,resolvedBy,resolvedAt,description\n");
        mismatches.forEach(item -> builder.append(
            Stream.of(
                    item.getMismatchKey(),
                    item.getOrderId() != null ? String.valueOf(item.getOrderId()) : "",
                    item.getType(),
                    item.getSeverity(),
                    String.valueOf(item.isResolved()),
                    item.getResolutionNote(),
                    item.getResolvedBy(),
                    item.getResolvedAt() != null ? item.getResolvedAt().toString() : "",
                    item.getDescription()
                )
                .map(this::csv)
                .collect(Collectors.joining(",")))
            .append('\n'));
        return builder.toString();
    }

    @Override
    @Transactional
    public FinanceRefundRetryItemPojo manualRetry(Long queueId) throws Exception {
        refundRetryService.manualRetry(queueId);
        RefundRetryQueue queue = refundRetryQueueRepository.findById(queueId)
            .orElseThrow();
        return toRetryItem(queue);
    }

    @Override
    @Transactional
    public void resolveMismatch(String mismatchKey, String note, String resolvedBy) {
        FinanceMismatchResolution resolution = financeMismatchResolutionRepository.findByMismatchKey(mismatchKey)
            .orElse(FinanceMismatchResolution.builder().mismatchKey(mismatchKey).build());
        resolution.setNote(note);
        resolution.setResolvedBy(resolvedBy);
        financeMismatchResolutionRepository.saveAndFlush(resolution);
    }

    private List<FinanceReconciliationMismatchPojo> buildMismatches(LocalDate from, LocalDate to) {
        Instant fromInstant = toInstant(from, true);
        Instant toInstant = toInstant(to, false);

        List<Order> orders = ordersRepository.findAll().stream()
            .filter(order -> order.getDate() != null
                && !order.getDate().isBefore(fromInstant)
                && !order.getDate().isAfter(toInstant))
            .toList();

        Map<String, PaymentCallbackLog> callbackByToken = getCallbackByToken(orders);
        Map<Long, RefundRetryQueue> retryByOrder = refundRetryQueueRepository.findAll().stream()
            .collect(Collectors.toMap(
                queue -> queue.getOrder().getId(),
                Function.identity(),
                (left, right) -> left.getUpdatedAt().isAfter(right.getUpdatedAt()) ? left : right
            ));

        List<FinanceReconciliationMismatchPojo> result = new ArrayList<>();
        for (Order order : orders) {
            String orderStatus = order.getStatus() != null ? order.getStatus().getName() : null;
            PaymentCallbackLog callback = callbackByToken.get(order.getTransactionToken());
            String callbackResult = callback != null ? callback.getResult().name() : null;

            if (callback != null && callback.getAuthorizedAmount() != null
                && callback.getAuthorizedAmount() != order.getTotalValue()) {
                result.add(FinanceReconciliationMismatchPojo.builder()
                    .mismatchKey(keyFor(order.getId(), "amount"))
                    .orderId(order.getId())
                    .type("AMOUNT_MISMATCH")
                    .description("Authorized amount " + callback.getAuthorizedAmount()
                        + " != order total " + order.getTotalValue())
                    .severity("HIGH")
                    .build());
            }

            if (callback != null && "SUCCESS".equalsIgnoreCase(callbackResult)
                && FAILED_ORDER_STATUSES.contains(orderStatus)) {
                result.add(FinanceReconciliationMismatchPojo.builder()
                    .mismatchKey(keyFor(order.getId(), "callback-success-order-failed"))
                    .orderId(order.getId())
                    .type("STATUS_MISMATCH")
                    .description("Gateway callback is SUCCESS but order is in failed/cancelled payment status.")
                    .severity("HIGH")
                    .build());
            }

            if (callback != null
                && ("ABORTED".equalsIgnoreCase(callbackResult) || "GATEWAY_ERROR".equalsIgnoreCase(callbackResult))
                && SUCCESS_ORDER_STATUSES.contains(orderStatus)) {
                result.add(FinanceReconciliationMismatchPojo.builder()
                    .mismatchKey(keyFor(order.getId(), "callback-failed-order-success"))
                    .orderId(order.getId())
                    .type("STATUS_MISMATCH")
                    .description("Gateway callback indicates failed/aborted but order status is paid/completed.")
                    .severity("HIGH")
                    .build());
            }

            if (order.getTotalRefundedAmount() > order.getTotalValue()) {
                result.add(FinanceReconciliationMismatchPojo.builder()
                    .mismatchKey(keyFor(order.getId(), "refund-overflow"))
                    .orderId(order.getId())
                    .type("REFUND_OVERFLOW")
                    .description("Total refunded amount exceeds order total.")
                    .severity("CRITICAL")
                    .build());
            }

            RefundRetryQueue retryQueue = retryByOrder.get(order.getId());
            if (retryQueue != null && retryQueue.getStatus() == RefundRetryQueue.RefundStatus.FAILED_PERMANENT) {
                result.add(FinanceReconciliationMismatchPojo.builder()
                    .mismatchKey(keyFor(order.getId(), "refund-permanent-failure"))
                    .orderId(order.getId())
                    .type("REFUND_PERMANENT_FAILURE")
                    .description("Refund retry queue reached FAILED_PERMANENT. Manual accounting action is required.")
                    .severity("CRITICAL")
                    .build());
            }
        }

        hydrateResolutions(result);
        return result;
    }

    private void hydrateResolutions(List<FinanceReconciliationMismatchPojo> data) {
        if (data.isEmpty()) {
            return;
        }
        List<String> keys = data.stream().map(FinanceReconciliationMismatchPojo::getMismatchKey).toList();
        Map<String, FinanceMismatchResolution> resolutions = financeMismatchResolutionRepository
            .findByMismatchKeyIn(keys)
            .stream()
            .collect(Collectors.toMap(FinanceMismatchResolution::getMismatchKey, Function.identity()));

        data.forEach(item -> {
            FinanceMismatchResolution resolution = resolutions.get(item.getMismatchKey());
            if (resolution == null) {
                item.setResolved(false);
                return;
            }
            item.setResolved(true);
            item.setResolutionNote(resolution.getNote());
            item.setResolvedBy(resolution.getResolvedBy());
            item.setResolvedAt(resolution.getResolvedAt());
        });
    }

    private Map<String, PaymentCallbackLog> getCallbackByToken(Collection<Order> orders) {
        List<String> tokens = orders.stream()
            .map(Order::getTransactionToken)
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
        if (tokens.isEmpty()) {
            return Map.of();
        }
        return paymentCallbackLogRepository.findByTokenIn(tokens).stream()
            .collect(Collectors.toMap(PaymentCallbackLog::getToken, Function.identity(), (left, right) -> right));
    }

    private FinancePaymentItemPojo toPaymentItem(Order order, PaymentCallbackLog callback) {
        String orderStatus = order.getStatus() != null ? order.getStatus().getName() : null;
        return FinancePaymentItemPojo.builder()
            .orderId(order.getId())
            .transactionToken(order.getTransactionToken())
            .gateway(order.getPaymentType() != null ? order.getPaymentType().getName() : null)
            .orderStatus(orderStatus)
            .paymentStatus(derivePaymentStatus(orderStatus))
            .orderTotal(order.getTotalValue())
            .callbackResult(callback != null ? callback.getResult().name() : null)
            .callbackAuthorizedAmount(callback != null ? callback.getAuthorizedAmount() : null)
            .processedAt(callback != null ? callback.getProcessedAt() : order.getDate())
            .build();
    }

    private FinanceRefundOperationPojo toRefundItem(ReturnRequest request) {
        return FinanceRefundOperationPojo.builder()
            .returnRequestId(request.getId())
            .orderId(request.getOrder().getId())
            .status(request.getStatus().name())
            .refundAmount(request.getRefundAmount())
            .refundedAmountToDate(request.getOrder().getTotalRefundedAmount())
            .refundMethod(request.getRefundMethod().name())
            .reason(request.getReason())
            .adminNotes(request.getAdminNotes())
            .lastModified(request.getLastModified())
            .build();
    }

    private FinanceRefundRetryItemPojo toRetryItem(RefundRetryQueue queue) {
        return FinanceRefundRetryItemPojo.builder()
            .queueId(queue.getId())
            .orderId(queue.getOrder().getId())
            .transactionToken(queue.getTransactionToken())
            .amount(queue.getAmount())
            .reason(queue.getReason())
            .status(queue.getStatus().name())
            .attemptCount(queue.getAttemptCount())
            .failedAttempts(queue.getFailedAttempts())
            .nextRetryAt(queue.getNextRetryAt())
            .lastError(queue.getLastError())
            .updatedAt(queue.getUpdatedAt())
            .build();
    }

    private FinanceCallbackLogItemPojo toCallbackItem(PaymentCallbackLog log) {
        return FinanceCallbackLogItemPojo.builder()
            .id(log.getId())
            .orderId(log.getOrderId())
            .token(log.getToken())
            .result(log.getResult().name())
            .orderStatusAfter(log.getOrderStatusAfter())
            .authorizedAmount(log.getAuthorizedAmount())
            .processedAt(log.getProcessedAt())
            .build();
    }

    private String derivePaymentStatus(String orderStatus) {
        if (!StringUtils.hasText(orderStatus)) {
            return "UNKNOWN";
        }
        if (SUCCESS_ORDER_STATUSES.contains(orderStatus)) {
            return "SUCCESS";
        }
        if (FAILED_ORDER_STATUSES.contains(orderStatus)) {
            return Constants.ORDER_STATUS_PAYMENT_FAILED.equalsIgnoreCase(orderStatus) ? "FAILED" : "CANCELLED";
        }
        if (Constants.ORDER_STATUS_PAYMENT_STARTED.equalsIgnoreCase(orderStatus)
            || Constants.ORDER_STATUS_PENDING.equalsIgnoreCase(orderStatus)) {
            return "PENDING";
        }
        return "UNKNOWN";
    }

    private String keyFor(Long orderId, String typeSuffix) {
        return "order-" + orderId + "-" + typeSuffix.toLowerCase(Locale.ROOT);
    }

    private Instant toInstant(LocalDate date, boolean startOfDay) {
        if (date == null) {
            if (startOfDay) {
                return LocalDate.now(ZoneOffset.UTC).minusDays(30).atStartOfDay(ZoneOffset.UTC).toInstant();
            }
            return Instant.now();
        }
        return startOfDay
            ? date.atStartOfDay(ZoneOffset.UTC).toInstant()
            : date.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();
    }

    private <T> DataPagePojo<T> paginate(List<T> source, int page, int size) {
        int safeSize = Math.max(1, size);
        int safePage = Math.max(1, page);
        int fromIndex = Math.min((safePage - 1) * safeSize, source.size());
        int toIndex = Math.min(fromIndex + safeSize, source.size());
        List<T> sliced = source.subList(fromIndex, toIndex);
        return new DataPagePojo<>(sliced, safePage, source.size(), safeSize);
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
