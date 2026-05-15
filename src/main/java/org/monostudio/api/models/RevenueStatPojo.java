package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Revenue aggregated for a single time period (day, week, or month).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class RevenueStatPojo {
    /** ISO-8601 date string (yyyy-MM-dd). */
    private LocalDate date;
    /** Total revenue (sum of order totalValue) for this period, in cents. */
    private long revenue;
    /** Number of orders in this period. */
    private long orderCount;
}
