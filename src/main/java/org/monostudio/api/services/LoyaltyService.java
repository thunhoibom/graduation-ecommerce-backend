package org.monostudio.api.services;

import org.monostudio.api.models.LoyaltyProfilePojo;

import jakarta.persistence.EntityNotFoundException;

public interface LoyaltyService {

    void awardForPaidOrder(Long orderId);

    void reverseForOrder(Long orderId, String eventType);

    void syncRefundReversalForOrder(Long orderId);

    LoyaltyProfilePojo getLoyaltyProfileFromUserName(String userName) throws EntityNotFoundException;
}
