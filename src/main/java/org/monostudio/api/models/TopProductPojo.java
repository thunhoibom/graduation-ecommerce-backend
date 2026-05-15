package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * A product ranked by sales performance within a date range.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class TopProductPojo {
    private Long productId;
    private String productName;
    /** Total units sold across all order details. */
    private long unitsSold;
    /** Total revenue = sum of (unitValue × units) for this product, in cents. */
    private long revenue;
}
