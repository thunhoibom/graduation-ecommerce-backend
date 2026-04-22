package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class CartSessionPojo {

    private Long id;

    /** Session token — sent as X-Session-Token header */
    @NotBlank
    private String token;

    /** Items in this cart */
    private List<CartItemPojo> items;

    /** Computed subtotal = sum of (variant price × quantity) */
    private Integer subtotal;

    /** Number of unique items */
    private Integer itemCount;

    /** Number of total units */
    private Integer totalUnits;

    /** Applied discount code (if any) */
    private String appliedDiscountCode;

    /** Discount amount applied (in cents) */
    private Integer discountAmount;

    /** Final total after discount */
    private Integer totalAfterDiscount;

    /** JSON array of applied promotions (from server pricing engine) */
    private String appliedPromotionsJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Expiry timestamp — cart is considered abandoned after this */
    private LocalDateTime expiresAt;

    /** Whether the cart has expired */
    private Boolean expired;
}
