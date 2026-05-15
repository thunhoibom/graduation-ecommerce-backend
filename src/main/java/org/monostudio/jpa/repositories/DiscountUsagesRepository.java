package org.monostudio.jpa.repositories;

import org.monostudio.jpa.entities.DiscountUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscountUsagesRepository
    extends org.monostudio.jpa.Repository<DiscountUsage> {

    /**
     * Count how many times a specific customer has used a specific discount code.
     */
    @Query("SELECT COUNT(u) FROM DiscountUsage u "
        + "WHERE u.discountCode.id = :discountId "
        + "AND u.customer.id = :customerId")
    int countByDiscountIdAndCustomerId(
        @Param("discountId") Long discountId,
        @Param("customerId") Long customerId);

    /**
     * Find a usage record by discount ID and customer ID.
     */
    Optional<DiscountUsage> findByDiscountCodeIdAndCustomerId(Long discountCodeId, Long customerId);
}
