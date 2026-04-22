package org.monostudio.api.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.LoyaltyLedgerItemPojo;
import org.monostudio.api.models.LoyaltyProfilePojo;
import org.monostudio.api.services.LoyaltyService;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.LoyaltyPointsLedger;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.jpa.repositories.LoyaltyPointsLedgerRepository;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.UsersRepository;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.monostudio.config.Constants.LOYALTY_EVENT_EARN_PAID;
import static org.monostudio.config.Constants.LOYALTY_EVENT_REVERSE_REFUND;
import static org.monostudio.config.Constants.LOYALTY_TIER_GOLD;
import static org.monostudio.config.Constants.LOYALTY_TIER_SILVER;

@Service
@Transactional
public class LoyaltyServiceImpl
    implements LoyaltyService {
    private final OrdersRepository ordersRepository;
    private final CustomersRepository customersRepository;
    private final UsersRepository usersRepository;
    private final LoyaltyPointsLedgerRepository loyaltyPointsLedgerRepository;
    private final int pointsPerAmount;
    private final int silverThreshold;
    private final int goldThreshold;

    public LoyaltyServiceImpl(
        OrdersRepository ordersRepository,
        CustomersRepository customersRepository,
        UsersRepository usersRepository,
        LoyaltyPointsLedgerRepository loyaltyPointsLedgerRepository,
        @Value("${monostudio.loyalty.points-per-unit-amount:1000}") int pointsPerAmount,
        @Value("${monostudio.loyalty.silver-threshold-points:500}") int silverThreshold,
        @Value("${monostudio.loyalty.gold-threshold-points:2000}") int goldThreshold
    ) {
        this.ordersRepository = ordersRepository;
        this.customersRepository = customersRepository;
        this.usersRepository = usersRepository;
        this.loyaltyPointsLedgerRepository = loyaltyPointsLedgerRepository;
        this.pointsPerAmount = Math.max(1, pointsPerAmount);
        this.silverThreshold = Math.max(0, silverThreshold);
        this.goldThreshold = Math.max(this.silverThreshold, goldThreshold);
    }

    @Override
    public void awardForPaidOrder(Long orderId) {
        Order order = fetchOrder(orderId);
        Customer customer = order.getCustomer();
        if (customer == null) {
            return;
        }
        String idempotencyKey = buildIdempotencyKey(orderId, LOYALTY_EVENT_EARN_PAID);
        if (loyaltyPointsLedgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        int baseAmount = loyaltyBaseAmount(order);
        int points = pointsFromAmount(baseAmount);
        if (points <= 0) {
            return;
        }

        loyaltyPointsLedgerRepository.saveAndFlush(LoyaltyPointsLedger.builder()
            .customer(customer)
            .order(order)
            .eventType(LOYALTY_EVENT_EARN_PAID)
            .pointsDelta(points)
            .sourceAmount(baseAmount)
            .idempotencyKey(idempotencyKey)
            .build());

        customer.setLoyaltyPointsBalance(customer.getLoyaltyPointsBalance() + points);
        customer.setLifetimePointsEarned(customer.getLifetimePointsEarned() + points);
        customer.setLoyaltyTier(resolveTier(customer.getLoyaltyPointsBalance()));
        customersRepository.saveAndFlush(customer);
    }

    @Override
    public void reverseForOrder(Long orderId, String eventType) {
        Order order = fetchOrder(orderId);
        Customer customer = order.getCustomer();
        if (customer == null || StringUtils.isBlank(eventType)) {
            return;
        }
        String idempotencyKey = buildIdempotencyKey(orderId, eventType);
        if (loyaltyPointsLedgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        int earnedPoints = getEarnedPoints(orderId);
        if (earnedPoints <= 0) {
            return;
        }
        int reversedPoints = loyaltyPointsLedgerRepository.sumReversedPointsByOrderId(orderId);
        int remainingToReverse = Math.max(0, earnedPoints - reversedPoints);
        if (remainingToReverse <= 0) {
            return;
        }

        applyReverseEvent(customer, order, eventType, -remainingToReverse,
            loyaltyBaseAmount(order), idempotencyKey);
    }

    @Override
    public void syncRefundReversalForOrder(Long orderId) {
        Order order = fetchOrder(orderId);
        Customer customer = order.getCustomer();
        if (customer == null) {
            return;
        }

        int earnedPoints = getEarnedPoints(orderId);
        if (earnedPoints <= 0) {
            return;
        }

        int loyaltyBaseAmount = loyaltyBaseAmount(order);
        if (loyaltyBaseAmount <= 0) {
            return;
        }

        int cappedRefundAmount = Math.min(Math.max(0, order.getTotalRefundedAmount()), loyaltyBaseAmount);
        int expectedRefundReversalPoints = (int) Math.floor((double) earnedPoints * cappedRefundAmount / loyaltyBaseAmount);
        int actualRefundReversalPoints = loyaltyPointsLedgerRepository
            .sumReversedPointsByOrderIdAndEventType(orderId, LOYALTY_EVENT_REVERSE_REFUND);
        int deltaToReverse = Math.max(0, expectedRefundReversalPoints - actualRefundReversalPoints);
        if (deltaToReverse <= 0) {
            return;
        }

        String idempotencyKey = buildIdempotencyKey(
            orderId,
            LOYALTY_EVENT_REVERSE_REFUND + ":" + expectedRefundReversalPoints
        );
        if (loyaltyPointsLedgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        applyReverseEvent(customer, order, LOYALTY_EVENT_REVERSE_REFUND, -deltaToReverse,
            cappedRefundAmount, idempotencyKey);
    }

    @Override
    @Transactional(readOnly = true)
    public LoyaltyProfilePojo getLoyaltyProfileFromUserName(String userName) throws EntityNotFoundException {
        User user = usersRepository.findByName(userName)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userName));
        if (user.getPerson() == null) {
            throw new EntityNotFoundException("User does not have associated profile");
        }

        Customer customer = customersRepository.findByPersonId(user.getPerson().getId())
            .orElseThrow(() -> new EntityNotFoundException("Customer profile not found for user: " + userName));

        List<LoyaltyLedgerItemPojo> recentEvents = loyaltyPointsLedgerRepository
            .findTop10ByCustomerIdOrderByCreatedAtDesc(customer.getId())
            .stream()
            .map(entry -> LoyaltyLedgerItemPojo.builder()
                .eventType(entry.getEventType())
                .pointsDelta(entry.getPointsDelta())
                .sourceAmount(entry.getSourceAmount())
                .orderId(entry.getOrder() != null ? entry.getOrder().getId() : null)
                .createdAt(entry.getCreatedAt())
                .build())
            .collect(Collectors.toList());

        return LoyaltyProfilePojo.builder()
            .tier(StringUtils.defaultString(customer.getLoyaltyTier()))
            .pointsBalance(customer.getLoyaltyPointsBalance())
            .lifetimePointsEarned(customer.getLifetimePointsEarned())
            .recentEvents(recentEvents)
            .build();
    }

    private void applyReverseEvent(
        Customer customer,
        Order order,
        String eventType,
        int pointsDelta,
        int sourceAmount,
        String idempotencyKey
    ) {
        loyaltyPointsLedgerRepository.saveAndFlush(LoyaltyPointsLedger.builder()
            .customer(customer)
            .order(order)
            .eventType(eventType)
            .pointsDelta(pointsDelta)
            .sourceAmount(Math.max(0, sourceAmount))
            .idempotencyKey(idempotencyKey)
            .build());

        customer.setLoyaltyPointsBalance(Math.max(0, customer.getLoyaltyPointsBalance() + pointsDelta));
        customer.setLoyaltyTier(resolveTier(customer.getLoyaltyPointsBalance()));
        customersRepository.saveAndFlush(customer);
    }

    private int getEarnedPoints(Long orderId) {
        Optional<LoyaltyPointsLedger> earned = loyaltyPointsLedgerRepository
            .findFirstByOrderIdAndEventTypeOrderByCreatedAtAsc(orderId, LOYALTY_EVENT_EARN_PAID);
        return earned.map(LoyaltyPointsLedger::getPointsDelta).orElse(0);
    }

    private int loyaltyBaseAmount(Order order) {
        return Math.max(0, order.getNetValue() + order.getTaxesValue());
    }

    private int pointsFromAmount(int amount) {
        if (amount <= 0) {
            return 0;
        }
        return amount / pointsPerAmount;
    }

    private String resolveTier(int pointsBalance) {
        if (pointsBalance >= goldThreshold) {
            return LOYALTY_TIER_GOLD;
        }
        if (pointsBalance >= silverThreshold) {
            return LOYALTY_TIER_SILVER;
        }
        return "";
    }

    private String buildIdempotencyKey(Long orderId, String eventType) {
        return "order:" + orderId + ":" + eventType;
    }

    private Order fetchOrder(Long orderId) {
        return ordersRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
    }
}
