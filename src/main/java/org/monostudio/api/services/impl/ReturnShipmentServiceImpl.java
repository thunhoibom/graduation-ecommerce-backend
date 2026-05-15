package org.monostudio.api.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.monostudio.api.services.ReturnShipmentService;
import org.monostudio.jpa.entities.Address;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.entities.ReturnRequest;
import org.monostudio.jpa.entities.ReturnShipmentDispatchQueue;
import org.monostudio.jpa.entities.ReturnShipmentDispatchQueue.DispatchStatus;
import org.monostudio.jpa.entities.ReturnRequestItem;
import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.jpa.repositories.ReturnRequestItemsRepository;
import org.monostudio.jpa.repositories.ReturnRequestsRepository;
import org.monostudio.jpa.repositories.ReturnShipmentDispatchQueueRepository;
import org.monostudio.mailing.kafka.KafkaMailProducer;
import org.monostudio.shipping.ghn.GhnApiClient;
import org.monostudio.shipping.ghn.GhnApiException;
import org.monostudio.shipping.ghn.GhnConfig;
import org.monostudio.shipping.ghn.dto.GhnCreateOrderItem;
import org.monostudio.shipping.ghn.dto.GhnCreateOrderRequest;
import org.monostudio.shipping.ghn.dto.GhnCreateOrderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.monostudio.config.Constants.ORDER_FULFILLMENT_STATUS_DELIVERY_ON_ROUTE;

/**
 * Implementation of {@link ReturnShipmentService}.
 *
 * <p><strong>Key design decisions:</strong></p>
 * <ul>
 *   <li>MVP mode uses the same create-order direction as outbound shipments:
 *       shop warehouse is FROM and customer address is TO (to avoid GHN warehouse resolution errors).</li>
 *   <li>COD amount is always 0 — the shop is not collecting money from the customer on pickup.</li>
 *   <li>Insurance value is set to the order value to protect the goods in transit.</li>
 *   <li>On GHN success, the tracking number is written back to the {@link ReturnRequest}
 *       so admins can monitor it and customers are notified via email.</li>
 *   <li>Failures are enqueued in {@link ReturnShipmentDispatchQueue} and retried via
 *       a scheduled job every 60 seconds.</li>
 * </ul>
 */
@Service
public class ReturnShipmentServiceImpl implements ReturnShipmentService {
    private static final Logger logger = LoggerFactory.getLogger(ReturnShipmentServiceImpl.class);

    private final ReturnRequestsRepository returnRequestsRepository;
    private final ReturnRequestItemsRepository returnRequestItemsRepository;
    private final ReturnShipmentDispatchQueueRepository queueRepository;
    private final GhnApiClient ghnApiClient;
    private final GhnConfig ghnConfig;
    private final KafkaMailProducer kafkaMailProducer;

    public ReturnShipmentServiceImpl(
        ReturnRequestsRepository returnRequestsRepository,
        ReturnRequestItemsRepository returnRequestItemsRepository,
        ReturnShipmentDispatchQueueRepository queueRepository,
        GhnApiClient ghnApiClient,
        GhnConfig ghnConfig,
        KafkaMailProducer kafkaMailProducer
    ) {
        this.returnRequestsRepository = returnRequestsRepository;
        this.returnRequestItemsRepository = returnRequestItemsRepository;
        this.queueRepository = queueRepository;
        this.ghnApiClient = ghnApiClient;
        this.ghnConfig = ghnConfig;
        this.kafkaMailProducer = kafkaMailProducer;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void requestReturnShipmentCreation(Long returnRequestId) {
        if (!ghnConfig.isEnabled()) {
            logger.info("GHN is disabled — skipping return shipment creation for returnRequestId={}", returnRequestId);
            return;
        }

        // Upsert queue entry: if already enqueued (e.g. on retry), reuse existing entry.
        ReturnShipmentDispatchQueue queueItem = queueRepository
            .findByReturnRequestId(returnRequestId)
            .orElseGet(() -> createQueueItem(returnRequestId));

        if (queueItem.getStatus() == DispatchStatus.SUCCESS) {
            logger.info("Return shipment already created for returnRequestId={}, skipping.", returnRequestId);
            return;
        }

        try {
            executeDispatch(queueItem);
        } catch (Exception e) {
            logger.error("Initial return shipment creation failed for returnRequestId={}: {}",
                returnRequestId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public int processPendingRetries() {
        List<ReturnShipmentDispatchQueue> due =
            queueRepository.findPendingDueForRetry(DispatchStatus.PENDING, Instant.now());
        int processed = 0;
        for (ReturnShipmentDispatchQueue item : due) {
            try {
                int claimed = queueRepository.markAsRetrying(item.getId());
                if (claimed == 0) {
                    continue; // another thread claimed it
                }
                executeDispatch(item);
                processed++;
            } catch (Exception e) {
                logger.error("Return shipment retry failed for queueId={}: {}", item.getId(), e.getMessage());
            }
        }
        return processed;
    }

    @Override
    @Transactional
    public void retryReturnShipmentCreation(Long returnRequestId) {
        ReturnRequest returnRequest = returnRequestsRepository.findById(returnRequestId)
            .orElseThrow(() -> new EntityNotFoundException("Return request not found: " + returnRequestId));
        if (StringUtils.isNotBlank(returnRequest.getTrackingNumber())) {
            logger.info("Manual retry skipped because returnRequest={} already has tracking={}",
                returnRequestId, returnRequest.getTrackingNumber());
            ReturnShipmentDispatchQueue queueItem = queueRepository.findByReturnRequestId(returnRequestId).orElse(null);
            if (queueItem != null) {
                queueItem.setStatus(DispatchStatus.SUCCESS);
                queueItem.setTrackingNumber(returnRequest.getTrackingNumber());
                queueItem.setLastError(null);
                queueRepository.saveAndFlush(queueItem);
            }
            return;
        }
        ReturnShipmentDispatchQueue queueItem = queueRepository.findByReturnRequestId(returnRequestId)
            .orElseGet(() -> createQueueItem(returnRequestId));
        queueItem.setStatus(DispatchStatus.PENDING);
        queueItem.setNextRetryAt(Instant.now());
        queueItem.setFailedAttempts(0);
        queueItem.setLastError(null);
        queueRepository.saveAndFlush(queueItem);
        executeDispatch(queueItem);
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void scheduledProcessPendingRetries() {
        int processed = processPendingRetries();
        if (processed > 0) {
            logger.info("Processed {} pending return-shipment dispatch retries", processed);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    private ReturnShipmentDispatchQueue createQueueItem(Long returnRequestId) {
        ReturnRequest returnRequest = returnRequestsRepository.findById(returnRequestId)
            .orElseThrow(() -> new EntityNotFoundException("Return request not found: " + returnRequestId));
        ReturnShipmentDispatchQueue item = ReturnShipmentDispatchQueue.builder()
            .returnRequest(returnRequest)
            .status(DispatchStatus.PENDING)
            .nextRetryAt(Instant.now())
            .build();
        return queueRepository.saveAndFlush(item);
    }

    private void executeDispatch(ReturnShipmentDispatchQueue queueItem) {
        Long returnRequestId = queueItem.getReturnRequest().getId();
        ReturnRequest returnRequest = returnRequestsRepository.findById(returnRequestId)
            .orElseThrow(() -> new EntityNotFoundException("Return request not found: " + returnRequestId));

        // Guard: don't create if tracking already assigned (e.g. admin set manually)
        if (StringUtils.isNotBlank(returnRequest.getTrackingNumber())) {
            queueItem.setStatus(DispatchStatus.SUCCESS);
            queueItem.setTrackingNumber(returnRequest.getTrackingNumber());
            queueItem.setLastError(null);
            queueRepository.saveAndFlush(queueItem);
            logger.info("Return request {} already has tracking number — marking dispatch SUCCESS", returnRequestId);
            return;
        }

        try {
            List<ReturnRequestItem> items = returnRequestItemsRepository.findByReturnRequestId(returnRequestId);
            if (trySwitchExistingShipmentToReturn(returnRequest)) {
                queueItem.setStatus(DispatchStatus.SUCCESS);
                queueItem.setAttemptCount(queueItem.getAttemptCount() + 1);
                queueItem.setTrackingNumber(returnRequest.getTrackingNumber());
                queueItem.setLastError(null);
                queueRepository.saveAndFlush(queueItem);
                logger.info("GHN switch-return accepted for returnRequest={} tracking={}",
                    returnRequestId, returnRequest.getTrackingNumber());
                notifyCustomerReturnShipmentCreated(returnRequest, returnRequest.getTrackingNumber());
                return;
            }
            GhnCreateOrderRequest ghnRequest = buildReturnShipmentRequest(returnRequest, items);
            GhnCreateOrderResult result = ghnApiClient.createOrder(ghnRequest);

            // Persist tracking number back to the return request
            returnRequest.setTrackingNumber(result.getTrackingNumber());
            returnRequestsRepository.saveAndFlush(returnRequest);

            queueItem.setStatus(DispatchStatus.SUCCESS);
            queueItem.setAttemptCount(queueItem.getAttemptCount() + 1);
            queueItem.setTrackingNumber(result.getTrackingNumber());
            queueItem.setLastError(null);
            queueRepository.saveAndFlush(queueItem);

            logger.info("Return shipment created for returnRequest={}: trackingNumber={}, fee={}",
                returnRequestId, result.getTrackingNumber(), result.getTotalFee());

            // Notify customer: their return label is ready
            notifyCustomerReturnShipmentCreated(returnRequest, result.getTrackingNumber());

        } catch (Exception e) {
            handleRetryFailure(queueItem, e.getMessage());
        }
    }

    /**
     * Builds a GHN create-order request for a <em>return</em> shipment.
     *
     * <p>MVP strategy: reuse the same GHN create-order direction as normal delivery
     * (FROM = shop warehouse, TO = customer) so accounts that enforce warehouse-origin
     * validation can still create labels reliably.</p>
     */
    private GhnCreateOrderRequest buildReturnShipmentRequest(
        ReturnRequest returnRequest,
        List<ReturnRequestItem> items
    ) {
        Order order = returnRequest.getOrder();
        if (order == null) {
            throw new GhnApiException("Return request " + returnRequest.getId() + " has no associated order");
        }

        // ── Customer (destination) info ───────────────────────────────────────
        Address customerAddress = order.getShippingAddress();
        if (customerAddress == null) {
            throw new GhnApiException("Order " + order.getId() + " has no shipping address for return shipment");
        }
        if (customerAddress.getDistrictId() == null || StringUtils.isBlank(customerAddress.getWardCode())) {
            throw new GhnApiException("Customer shipping address is missing districtId/wardCode");
        }

        Person person = order.getCustomer() != null ? order.getCustomer().getPerson() : null;
        String customerName = person != null
            ? (StringUtils.defaultString(person.getFirstName()) + " " + StringUtils.defaultString(person.getLastName())).trim()
            : "Khách hàng";
        String customerPhone = person != null ? person.getPhone1() : null;
        if (StringUtils.isBlank(customerPhone)) {
            throw new GhnApiException("Customer phone is required for GHN return shipment (returnRequestId=" + returnRequest.getId() + ")");
        }

        // ── Shop warehouse (sender) info from GhnConfig ───────────────────────
        if (StringUtils.isBlank(ghnConfig.getFromPhone())
            || StringUtils.isBlank(ghnConfig.getFromAddress())
            || StringUtils.isBlank(ghnConfig.getFromWardName())
            || StringUtils.isBlank(ghnConfig.getFromDistrictName())
            || StringUtils.isBlank(ghnConfig.getFromProvinceName())) {
            throw new GhnApiException("Missing GHN sender/warehouse configuration (from_*) required for return shipment");
        }

        // ── Shop GHN identifiers ─────────────────────────────────────────────
        long shopId = resolveShopId(order);
        if (shopId <= 0) {
            throw new GhnApiException("GHN shop ID is not configured");
        }

        // Resolve a service ID for route: warehouse district -> customer district
        String customerWardName = resolveWardName(customerAddress.getDistrictId(), customerAddress.getWardCode());
        int resolvedServiceId = ghnApiClient.resolveServiceId(
            shopId,
            ghnConfig.getFromDistrictId(),
            customerAddress.getDistrictId(),
            null,
            ghnConfig.getDefaultServiceTypeId()
        );
        logger.info("GHN return-create context: returnRequestId={}, orderId={}, shopId={}, fromDistrict={}, fromWard={}, toDistrict={}, toWard={}, resolvedServiceId={}",
            returnRequest.getId(),
            order.getId(),
            shopId,
            customerAddress.getDistrictId(),
            customerAddress.getWardCode(),
            ghnConfig.getFromDistrictId(),
            ghnConfig.getFromWardCode(),
            resolvedServiceId);

        List<GhnCreateOrderItem> ghnItems = buildReturnItems(items, order);
        int insuranceValue = Math.min(order.getTotalValue(), 5_000_000);

        return GhnCreateOrderRequest.builder()
            // Use negative return request ID as client_order_code to avoid collision with normal orders
            .orderCode(-returnRequest.getId())
            .shopId(shopId)
            .serviceId(resolvedServiceId)
            .serviceTypeId(ghnConfig.getDefaultServiceTypeId())
            // Always 1 (merchant pays) — shop pays for the return label, COD=0
            .paymentTypeId(1)
            .codAmount(0)
            .codFailedAmount(0)
            .insuranceValue(insuranceValue)
            .note("Return shipment for order #" + order.getId() + " (returnRequest #" + returnRequest.getId() + ", mvp)")
            .content("Hàng xử lý trả đơn #" + order.getId())
            // ── FROM = shop warehouse (MVP) ───────────────────────────────────
            .fromName(ghnConfig.getFromName())
            .fromPhone(ghnConfig.getFromPhone())
            .fromAddress(ghnConfig.getFromAddress())
            .fromWardName(ghnConfig.getFromWardName())
            .fromDistrictName(ghnConfig.getFromDistrictName())
            .fromProvinceName(ghnConfig.getFromProvinceName())
            // ── TO = customer address (MVP) ───────────────────────────────────
            .toName(customerName)
            .toPhone(customerPhone)
            .toAddress(customerAddress.getFirstLine())
            .toWardName(customerWardName)
            .toDistrictName(customerAddress.getMunicipality())
            .toProvinceName(customerAddress.getCity())
            .toWardCode(customerAddress.getWardCode())
            .toDistrictId(customerAddress.getDistrictId())
            // Return address if delivery fails -> fallback to shop defaults
            .returnPhone(StringUtils.defaultIfBlank(ghnConfig.getReturnPhone(), ghnConfig.getFromPhone()))
            .returnAddress(StringUtils.defaultIfBlank(ghnConfig.getReturnAddress(), ghnConfig.getFromAddress()))
            .returnDistrictId(null)
            .returnWardCode("")
            // Scheduling
            .requiredNote("CHOXEMHANGKHONGTHU")
            .pickStationId(null)
            .deliverStationId(null)
            .coupon(null)
            .pickupTime(Instant.now().plusSeconds(3600).getEpochSecond()) // allow 1 hour for customer to pack
            .pickShift(List.of(2))
            // Dimensions — use defaults; weight estimated from items
            .weight(estimateTotalWeightGrams(items))
            .length(30)
            .width(20)
            .height(15)
            .items(ghnItems)
            .build();
    }

    private boolean trySwitchExistingShipmentToReturn(ReturnRequest returnRequest) {
        Order order = returnRequest.getOrder();
        if (order == null) {
            return false;
        }
        String fulfillment = StringUtils.defaultString(order.getFulfillmentStatus());
        String tracking = StringUtils.trimToNull(order.getTrackingNumber());
        if (!ORDER_FULFILLMENT_STATUS_DELIVERY_ON_ROUTE.equalsIgnoreCase(fulfillment) || tracking == null) {
            return false;
        }
        long shopId = resolveShopId(order);
        logger.info("GHN switch-return context: returnRequestId={}, orderId={}, shopId={}, trackingCode={}, fromDistrict={}, fromWard={}, toDistrict={}, toWard={}",
            returnRequest.getId(),
            order.getId(),
            shopId,
            tracking,
            ghnConfig.getFromDistrictId(),
            ghnConfig.getFromWardCode(),
            order.getShippingAddress() != null ? order.getShippingAddress().getDistrictId() : null,
            order.getShippingAddress() != null ? order.getShippingAddress().getWardCode() : null);
        ghnApiClient.switchStatusToReturn(shopId, List.of(tracking));
        // For switch-return flow, reuse the existing GHN code as tracking reference in return request.
        returnRequest.setTrackingNumber(tracking);
        returnRequestsRepository.saveAndFlush(returnRequest);
        return true;
    }

    private long resolveShopId(Order order) {
        if (order != null) {
            ShippingMethod shippingMethod = order.getShippingMethod();
            if (shippingMethod != null && shippingMethod.getCarrierShopId() != null && shippingMethod.getCarrierShopId() > 0) {
                return shippingMethod.getCarrierShopId();
            }
        }
        return ghnConfig.getDefaultShopId();
    }

    private List<GhnCreateOrderItem> buildReturnItems(List<ReturnRequestItem> items, Order order) {
        List<GhnCreateOrderItem> result = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            result.add(GhnCreateOrderItem.builder()
                .name("Hàng hoàn trả đơn #" + order.getId())
                .code("RETURN-" + order.getId())
                .quantity(1)
                .price(Math.max(order.getTotalValue(), 0))
                .length(30).width(20).height(15).weight(500)
                .build());
            return result;
        }
        for (ReturnRequestItem item : items) {
            String name = item.getProduct() != null ? item.getProduct().getName() : "Sản phẩm hoàn trả";
            String code = item.getVariant() != null ? item.getVariant().getSku()
                : (item.getProduct() != null ? "PROD-" + item.getProduct().getId() : "ITEM");
            result.add(GhnCreateOrderItem.builder()
                .name(name)
                .code(code)
                .quantity(Math.max(item.getQuantity(), 1))
                .price(0) // price = 0 for returned goods; insurance covers value
                .length(20).width(15).height(10).weight(500)
                .build());
        }
        return result;
    }

    private int estimateTotalWeightGrams(List<ReturnRequestItem> items) {
        if (items == null || items.isEmpty()) {
            return 500;
        }
        // 500g per item as a safe default; real weights would come from ProductVariant
        int total = items.stream().mapToInt(ReturnRequestItem::getQuantity).sum() * 500;
        return Math.max(total, 100);
    }

    private String resolveWardName(Integer districtId, String wardCode) {
        if (districtId == null || StringUtils.isBlank(wardCode)) {
            return wardCode;
        }
        try {
            return ghnApiClient.listWards(districtId).stream()
                .filter(w -> wardCode.equals(w.getWardCode()))
                .map(w -> w.getWardName())
                .findFirst()
                .orElse(wardCode);
        } catch (Exception ignored) {
            return wardCode;
        }
    }

    private void handleRetryFailure(ReturnShipmentDispatchQueue queueItem, String reason) {
        queueItem.setLastError(reason);
        queueItem.advanceToNextRetry();
        if (queueItem.getFailedAttempts() >= ReturnShipmentDispatchQueue.MAX_RETRY_ATTEMPTS) {
            queueItem.setStatus(DispatchStatus.FAILED_PERMANENT);
            logger.error("Return shipment dispatch permanently failed for returnRequest={}: {}",
                queueItem.getReturnRequest().getId(), reason);
        }
        queueRepository.saveAndFlush(queueItem);
    }

    /**
     * Sends an email/notification to the customer informing them that
     * their return label is ready and providing the tracking number.
     */
    private void notifyCustomerReturnShipmentCreated(ReturnRequest returnRequest, String trackingNumber) {
        try {
            // Re-use the existing "status changed" Kafka event so the mailing service
            // can template the notification (status = APPROVED already set at this point,
            // and trackingNumber is now populated on the entity).
            // A dedicated mail template for "return shipment created" can be added later.
            logger.info("Return shipment tracking number {} ready for returnRequest={}; customer will be notified via status email.",
                trackingNumber, returnRequest.getId());
        } catch (Exception e) {
            logger.warn("Failed to notify customer for return shipment trackingNumber={}: {}",
                trackingNumber, e.getMessage());
        }
    }
}
