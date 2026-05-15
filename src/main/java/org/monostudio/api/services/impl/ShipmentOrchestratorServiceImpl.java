package org.monostudio.api.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.monostudio.api.services.ShipmentOrchestratorService;
import org.monostudio.jpa.entities.Address;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.OrderDetail;
import org.monostudio.jpa.entities.ShipmentDispatchQueue;
import org.monostudio.jpa.entities.ShipmentDispatchQueue.DispatchStatus;
import org.monostudio.jpa.entities.ShipmentTracking;
import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.ShipmentDispatchQueueRepository;
import org.monostudio.jpa.repositories.ShipmentTrackingRepository;
import org.monostudio.shipping.ShippingCarrierCodes;
import org.monostudio.shipping.ghn.GhnApiClient;
import org.monostudio.shipping.ghn.GhnConfig;
import org.monostudio.shipping.ghn.GhnApiException;
import org.monostudio.shipping.ghn.dto.GhnCreateOrderRequest;
import org.monostudio.shipping.ghn.dto.GhnCreateOrderResult;
import org.monostudio.shipping.ghn.dto.GhnCreateOrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ShipmentOrchestratorServiceImpl implements ShipmentOrchestratorService {
    private static final Logger logger = LoggerFactory.getLogger(ShipmentOrchestratorServiceImpl.class);

    private final OrdersRepository ordersRepository;
    private final ShipmentDispatchQueueRepository queueRepository;
    private final ShipmentTrackingRepository shipmentTrackingRepository;
    private final GhnApiClient ghnApiClient;
    private final GhnConfig ghnConfig;

    public ShipmentOrchestratorServiceImpl(
        OrdersRepository ordersRepository,
        ShipmentDispatchQueueRepository queueRepository,
        ShipmentTrackingRepository shipmentTrackingRepository,
        GhnApiClient ghnApiClient,
        GhnConfig ghnConfig
    ) {
        this.ordersRepository = ordersRepository;
        this.queueRepository = queueRepository;
        this.shipmentTrackingRepository = shipmentTrackingRepository;
        this.ghnApiClient = ghnApiClient;
        this.ghnConfig = ghnConfig;
    }

    @Override
    @Transactional
    public void requestShipmentCreation(Long orderId) {
        logger.debug("Shipment creation requested for orderId={}", orderId);
        ShipmentDispatchQueue queueItem = queueRepository.findByOrderId(orderId)
            .orElseGet(() -> createQueueItem(orderId));
        if (queueItem.getStatus() == DispatchStatus.SUCCESS) {
            logger.debug(
                "Shipment dispatch queue already successful for orderId={}, queueId={}, trackingNumber={}",
                orderId,
                queueItem.getId(),
                queueItem.getTrackingNumber()
            );
            return;
        }
        try {
            executeRetry(queueItem);
        } catch (Exception e) {
            logger.error("Initial shipment creation failed for order {}: {}", orderId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public int processPendingRetries() {
        List<ShipmentDispatchQueue> due = queueRepository.findPendingDueForRetry(DispatchStatus.PENDING, Instant.now());
        int processed = 0;
        for (ShipmentDispatchQueue queueItem : due) {
            try {
                int claimed = queueRepository.markAsRetrying(queueItem.getId());
                if (claimed == 0) {
                    continue;
                }
                executeRetry(queueItem);
                processed++;
            } catch (Exception e) {
                logger.error("Shipment queue retry failed for queueId={}: {}", queueItem.getId(), e.getMessage());
            }
        }
        return processed;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void scheduledProcessPendingRetries() {
        int processed = processPendingRetries();
        if (processed > 0) {
            logger.info("Processed {} pending shipment dispatch retries", processed);
        }
    }

    private ShipmentDispatchQueue createQueueItem(Long orderId) {
        Order order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        ShipmentDispatchQueue queueItem = ShipmentDispatchQueue.builder()
            .order(order)
            .status(DispatchStatus.PENDING)
            .nextRetryAt(Instant.now())
            .build();
        ShipmentDispatchQueue saved = queueRepository.saveAndFlush(queueItem);
        logger.debug("Created shipment dispatch queue item queueId={} for orderId={}", saved.getId(), orderId);
        return saved;
    }

    private void executeRetry(ShipmentDispatchQueue queueItem) {
        Long orderId = queueItem.getOrder().getId();
        logger.debug(
            "Executing shipment dispatch queueId={} orderId={} status={} attemptCount={} failedAttempts={}",
            queueItem.getId(),
            orderId,
            queueItem.getStatus(),
            queueItem.getAttemptCount(),
            queueItem.getFailedAttempts()
        );

        Order order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        ShippingMethod shippingMethod = order.getShippingMethod();
        if (shippingMethod == null) {
            markPermanentFailure(queueItem, "Order has no shipping method");
            return;
        }
        String carrierCode = StringUtils.defaultIfBlank(shippingMethod.getCarrierCode(), ShippingCarrierCodes.LOCAL)
            .trim()
            .toUpperCase();
        logger.debug(
            "Shipment dispatch context orderId={} shippingMethodId={} carrierCode={} carrierShopId={} existingTracking={}",
            orderId,
            shippingMethod.getId(),
            carrierCode,
            shippingMethod.getCarrierShopId(),
            order.getTrackingNumber()
        );
        if (!ShippingCarrierCodes.GHN.equals(carrierCode)) {
            logger.debug(
                "Skipping GHN create-order for orderId={} because carrierCode={} — no tracking number will be written",
                orderId,
                carrierCode
            );
            queueItem.setStatus(DispatchStatus.SUCCESS);
            queueItem.setLastError(null);
            queueRepository.saveAndFlush(queueItem);
            return;
        }
        if (StringUtils.isNotBlank(order.getTrackingNumber())) {
            logger.debug(
                "OrderId={} already has trackingNumber={} — marking dispatch queue successful without GHN create-order",
                orderId,
                order.getTrackingNumber()
            );
            queueItem.setStatus(DispatchStatus.SUCCESS);
            queueItem.setTrackingNumber(order.getTrackingNumber());
            queueItem.setLastError(null);
            queueRepository.saveAndFlush(queueItem);
            return;
        }

        try {
            GhnCreateOrderRequest request = buildCreateOrderRequest(order, shippingMethod);
            GhnCreateOrderResult result = ghnApiClient.createOrder(request);
            ordersRepository.setTracking(order.getId(), result.getTrackingNumber(), ShippingCarrierCodes.GHN);
            saveInitialTrackingEvent(order, result);

            queueItem.setStatus(DispatchStatus.SUCCESS);
            queueItem.setAttemptCount(queueItem.getAttemptCount() + 1);
            queueItem.setTrackingNumber(result.getTrackingNumber());
            queueItem.setLastError(null);
            queueRepository.saveAndFlush(queueItem);
            logger.debug(
                "GHN shipment created for orderId={} trackingNumber={} orderCode={}",
                orderId,
                result.getTrackingNumber(),
                result.getOrderCode()
            );
        } catch (Exception e) {
            handleRetryFailure(queueItem, e.getMessage());
        }
    }

    private void handleRetryFailure(ShipmentDispatchQueue queueItem, String reason) {
        queueItem.setLastError(reason);
        queueItem.advanceToNextRetry();
        if (queueItem.getFailedAttempts() >= ShipmentDispatchQueue.MAX_RETRY_ATTEMPTS) {
            queueItem.setStatus(DispatchStatus.FAILED_PERMANENT);
            logger.error("Shipment dispatch permanently failed for order {}: {}",
                queueItem.getOrder().getId(), reason);
        } else {
            logger.debug(
                "Shipment dispatch retry scheduled for orderId={} queueId={} failedAttempts={} nextRetryAt={} reason={}",
                queueItem.getOrder().getId(),
                queueItem.getId(),
                queueItem.getFailedAttempts(),
                queueItem.getNextRetryAt(),
                reason
            );
        }
        queueRepository.saveAndFlush(queueItem);
    }

    private void markPermanentFailure(ShipmentDispatchQueue queueItem, String reason) {
        queueItem.setStatus(DispatchStatus.FAILED_PERMANENT);
        queueItem.setLastError(reason);
        queueRepository.saveAndFlush(queueItem);
        logger.error("Shipment dispatch permanently failed for order {}: {}",
            queueItem.getOrder().getId(), reason);
    }

    private GhnCreateOrderRequest buildCreateOrderRequest(Order order, ShippingMethod shippingMethod) {
        Address address = order.getShippingAddress();
        if (address == null) {
            throw new GhnApiException("Order does not have shipping address");
        }
        if (address.getDistrictId() == null || StringUtils.isBlank(address.getWardCode())) {
            throw new GhnApiException("Shipping address missing districtId/wardCode");
        }
        Integer configuredServiceId = parseNullableInt(shippingMethod.getCarrierServiceCode());
        long shopId = shippingMethod.getCarrierShopId() != null ? shippingMethod.getCarrierShopId() : 0L;
        if (shopId <= 0) {
            throw new GhnApiException("Shipping method missing GHN shop id");
        }
        int resolvedServiceId = ghnApiClient.resolveServiceId(
            shopId,
            ghnConfig.getFromDistrictId(),
            address.getDistrictId(),
            configuredServiceId,
            ghnConfig.getDefaultServiceTypeId()
        );
        logger.info("GHN create-order context: orderId={}, shopId={}, fromDistrict={}, fromWard={}, toDistrict={}, toWard={}, configuredServiceId={}, resolvedServiceId={}, paymentType={}",
            order.getId(), shopId, ghnConfig.getFromDistrictId(), ghnConfig.getFromWardCode(), address.getDistrictId(),
            address.getWardCode(), configuredServiceId, resolvedServiceId, order.getPaymentType() != null ? order.getPaymentType().getName() : null);
        String paymentTypeName = order.getPaymentType() != null ? order.getPaymentType().getName() : "COD";
        int codAmount = "COD".equalsIgnoreCase(paymentTypeName) ? order.getTotalValue() : 0;
        String customerName = order.getCustomer() != null && order.getCustomer().getPerson() != null
            ? (StringUtils.defaultString(order.getCustomer().getPerson().getFirstName()) + " "
            + StringUtils.defaultString(order.getCustomer().getPerson().getLastName())).trim()
            : "Customer";
        String phone = order.getCustomer() != null && order.getCustomer().getPerson() != null
            ? order.getCustomer().getPerson().getPhone1()
            : null;
        if (StringUtils.isBlank(phone)) {
            throw new GhnApiException("Customer phone is required for GHN create-order");
        }
        if (StringUtils.isBlank(ghnConfig.getFromPhone())
            || StringUtils.isBlank(ghnConfig.getFromAddress())
            || StringUtils.isBlank(ghnConfig.getFromWardName())
            || StringUtils.isBlank(ghnConfig.getFromDistrictName())
            || StringUtils.isBlank(ghnConfig.getFromProvinceName())) {
            throw new GhnApiException("Missing GHN sender configuration (from_*)");
        }
        String toWardName = resolveWardName(address.getDistrictId(), address.getWardCode());
        List<GhnCreateOrderItem> items = buildOrderItems(order);

        return GhnCreateOrderRequest.builder()
            .orderCode(order.getId())
            .shopId(shopId)
            .serviceId(resolvedServiceId)
            .serviceTypeId(ghnConfig.getDefaultServiceTypeId())
            .paymentTypeId(codAmount > 0 ? 2 : 1)
            .note("Mono Studio order #" + order.getId())
            .codAmount(codAmount)
            .codFailedAmount(0)
            .insuranceValue(Math.min(order.getTotalValue(), 5_000_000))
            .returnPhone(StringUtils.defaultIfBlank(ghnConfig.getReturnPhone(), ghnConfig.getFromPhone()))
            .returnAddress(StringUtils.defaultIfBlank(ghnConfig.getReturnAddress(), ghnConfig.getFromAddress()))
            .returnDistrictId(null)
            .returnWardCode("")
            .fromName(ghnConfig.getFromName())
            .fromPhone(ghnConfig.getFromPhone())
            .fromAddress(ghnConfig.getFromAddress())
            .fromWardName(ghnConfig.getFromWardName())
            .fromDistrictName(ghnConfig.getFromDistrictName())
            .fromProvinceName(ghnConfig.getFromProvinceName())
            .requiredNote("KHONGCHOXEMHANG")
            .content("Mono Studio order #" + order.getId())
            .toName(customerName)
            .toPhone(phone)
            .toAddress(address.getFirstLine())
            .toWardName(toWardName)
            .toDistrictName(address.getMunicipality())
            .toProvinceName(address.getCity())
            .toWardCode(address.getWardCode())
            .toDistrictId(address.getDistrictId())
            .pickStationId(null)
            .deliverStationId(null)
            .coupon(null)
            .pickupTime(Instant.now().plusSeconds(1800).getEpochSecond())
            .pickShift(List.of(2))
            .weight(1000)
            .length(20)
            .width(20)
            .height(10)
            .items(items)
            .build();
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

    private List<GhnCreateOrderItem> buildOrderItems(Order order) {
        List<GhnCreateOrderItem> items = new ArrayList<>();
        if (order.getDetails() == null || order.getDetails().isEmpty()) {
            items.add(GhnCreateOrderItem.builder()
                .name("Order #" + order.getId())
                .code("ORDER-" + order.getId())
                .quantity(1)
                .price(Math.max(order.getTotalValue(), 0))
                .length(20)
                .width(20)
                .height(10)
                .weight(1000)
                .build());
            return items;
        }
        for (OrderDetail detail : order.getDetails()) {
            String name = detail.getProduct() != null ? detail.getProduct().getName() : "Item";
            String code = detail.getProductVariant() != null ? detail.getProductVariant().getSku()
                : (detail.getProduct() != null ? detail.getProduct().getBarcode() : "ITEM");
            items.add(GhnCreateOrderItem.builder()
                .name(name)
                .code(code)
                .quantity(Math.max(detail.getUnits(), 1))
                .price(Math.max(detail.getUnitValue(), 0))
                .length(20)
                .width(20)
                .height(10)
                .weight(1000)
                .build());
        }
        return items;
    }

    private Integer parseNullableInt(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void saveInitialTrackingEvent(Order order, GhnCreateOrderResult result) {
        String trackingNumber = StringUtils.defaultIfBlank(result.getTrackingNumber(), result.getOrderCode());
        StringBuilder description = new StringBuilder("GHN order created");
        if (StringUtils.isNotBlank(result.getOrderCode())) {
            description.append(" (orderCode=").append(result.getOrderCode()).append(")");
        }
        if (result.getExpectedDeliveryTime() != null) {
            description.append(", expectedDelivery=").append(result.getExpectedDeliveryTime());
        }
        if (result.getTotalFee() != null) {
            description.append(", totalFee=").append(result.getTotalFee());
        }
        ShipmentTracking tracking = ShipmentTracking.builder()
            .order(order)
            .trackingNumber(trackingNumber)
            .shipperCode(ShippingCarrierCodes.GHN)
            .status("ORDER_CREATED")
            .location(null)
            .description(description.toString())
            .eventTime(Instant.now())
            .build();
        shipmentTrackingRepository.saveAndFlush(tracking);
    }
}
