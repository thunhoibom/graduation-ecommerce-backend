package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.OrderStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface OrdersRepository
    extends Repository<Order> {

    Optional<Order> findByTransactionToken(String token);

    @Query(value = "SELECT s FROM Order s "
        + "JOIN FETCH s.details "
        + "WHERE s.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Order s "
        + "SET s.status = :status "
        + "WHERE s.id = :id")
    int setStatus(@Param("id") Long id, @Param("status") OrderStatus status);

    @Modifying
    @Query("UPDATE Order s "
        + "SET s.transactionToken = :token "
        + "WHERE s.id = :id")
    int setTransactionToken(@Param("id") Long id, @Param("token") String token);

    @Modifying
    @Query("UPDATE Order s "
        + "SET s.trackingNumber = :trackingNumber, s.shipperCode = :shipperCode "
        + "WHERE s.id = :id")
    int setTracking(@Param("id") Long id, @Param("trackingNumber") String trackingNumber, @Param("shipperCode") String shipperCode);

    // ─── Admin Dashboard Queries ──────────────────────────────────────────────

    /**
     * Count orders by status (all-time or within a date range).
     */
    @Query("SELECT s.status.name AS status, COUNT(s) AS count "
        + "FROM Order s "
        + "WHERE s.date >= :from AND s.date <= :to "
        + "GROUP BY s.status.name")
    List<OrderStatusCountProjection> countByStatusGrouped(
        @Param("from") Instant from,
        @Param("to") Instant to);

    /**
     * Total revenue (sum of totalValue) within a date range.
     */
    @Query("SELECT COALESCE(SUM(s.totalValue), 0) "
        + "FROM Order s "
        + "WHERE s.status.name = :completedStatus "
        + "AND s.date >= :from AND s.date <= :to")
    long sumRevenueByStatusAndDateBetween(
        @Param("completedStatus") String completedStatus,
        @Param("from") Instant from,
        @Param("to") Instant to);

    /**
     * Total order count within a date range (any status).
     */
    @Query("SELECT COUNT(s) "
        + "FROM Order s "
        + "WHERE s.date >= :from AND s.date <= :to")
    long countByDateBetween(
        @Param("from") Instant from,
        @Param("to") Instant to);

    /**
     * Revenue broken down by day.
     * Uses native SQL for DATE() truncation — PostgreSQL compatible.
     */
    @Query(value = """
        SELECT DATE(o.order_date)               AS periodDate,
               COALESCE(SUM(o.order_total_value), 0) AS revenue,
               COUNT(o.order_id)                AS orderCount
        FROM   orders o
        WHERE  o.order_date >= :from
          AND  o.order_date <= :to
        GROUP  BY DATE(o.order_date)
        ORDER  BY periodDate ASC
        """, nativeQuery = true)
    List<RevenueByPeriodProjection> revenueByDay(
        @Param("from") Instant from,
        @Param("to") Instant to);

    /**
     * Revenue broken down by week (ISO week, starts Monday).
     * PostgreSQL compatible.
     */
    @Query(value = """
        SELECT DATE(DATE_TRUNC('week', o.order_date)) AS periodDate,
               COALESCE(SUM(o.order_total_value), 0)  AS revenue,
               COUNT(o.order_id)                      AS orderCount
        FROM   orders o
        WHERE  o.order_date >= :from
          AND  o.order_date <= :to
        GROUP  BY DATE(DATE_TRUNC('week', o.order_date))
        ORDER  BY periodDate ASC
        """, nativeQuery = true)
    List<RevenueByPeriodProjection> revenueByWeek(
        @Param("from") Instant from,
        @Param("to") Instant to);

    /**
     * Revenue broken down by month.
     * PostgreSQL compatible.
     */
    @Query(value = """
        SELECT DATE(DATE_TRUNC('month', o.order_date)) AS periodDate,
               COALESCE(SUM(o.order_total_value), 0)   AS revenue,
               COUNT(o.order_id)                       AS orderCount
        FROM   orders o
        WHERE  o.order_date >= :from
          AND  o.order_date <= :to
        GROUP  BY DATE(DATE_TRUNC('month', o.order_date))
        ORDER  BY periodDate ASC
        """, nativeQuery = true)
    List<RevenueByPeriodProjection> revenueByMonth(
        @Param("from") Instant from,
        @Param("to") Instant to);

    /**
     * Top products by units sold, within a date range.
     * Joins orders → order_details → products.
     * Only counts PAID/CONFIRMED/COMPLETED orders to reflect actual revenue.
     */
    @Query(value = """
        SELECT p.product_id                                          AS productId,
               p.product_name                                        AS productName,
               COALESCE(SUM(od.order_detail_units), 0)              AS unitsSold,
               COALESCE(SUM(od.order_detail_units * od.order_detail_unit_value), 0) AS revenue
        FROM   order_details od
        JOIN   orders o ON od.order_id = o.order_id
        JOIN   products p ON od.product_id = p.product_id
        WHERE  o.order_date >= :from
          AND  o.order_date <= :to
          AND  o.order_status_id IN (
                   SELECT os.order_status_id
                   FROM   order_statuses os
                   WHERE  os.order_status_name IN ('Paid, Confirmed', 'Delivery Complete')
               )
        GROUP  BY p.product_id, p.product_name
        ORDER  BY unitsSold DESC
        LIMIT  :limit
        """, nativeQuery = true)
    List<TopProductProjection> findTopProductsByUnitsSold(
        @Param("from") Instant from,
        @Param("to") Instant to,
        @Param("limit") int limit);

    /**
     * Check whether a customer has at least one completed (paid/confirmed) order containing a specific product.
     * Used to determine verified purchase status for product reviews.
     */
    @Query("""
        SELECT COUNT(od) > 0
        FROM   OrderDetail od
        JOIN   od.order o
        JOIN   o.status os
        WHERE  o.customer.id = :customerId
          AND  od.product.id = :productId
          AND  os.name IN ('Paid, Confirmed', 'Delivery Complete')
        """)
    boolean hasCompletedOrderWithProduct(
        @Param("customerId") Long customerId,
        @Param("productId") Long productId);

    /**
     * Finds all orders in a given status older than the cutoff timestamp.
     * Used by the stale-payment-session expiry job to find and cancel abandoned checkouts.
     *
     * @param statusName The exact name of the status to match.
     * @param cutoff     Orders created before this time will be returned.
     */
    @Query("SELECT o FROM Order o WHERE o.status.name = :statusName AND o.date < :cutoff")
    List<Order> findByStatusNameAndDateBefore(
        @Param("statusName") String statusName,
        @Param("cutoff") Instant cutoff);

    /**
     * Finds all orders belonging to a specific customer.
     * Uses JOIN FETCH to eagerly load details for display.
     *
     * @param customerId The customer's ID
     * @return List of orders sorted by date descending
     */
    @Query("SELECT o FROM Order o "
        + "LEFT JOIN FETCH o.details "
        + "WHERE o.customer.id = :customerId "
        + "ORDER BY o.date DESC")
    List<Order> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT o FROM Order o "
        + "LEFT JOIN FETCH o.details "
        + "JOIN o.customer c "
        + "JOIN c.person p "
        + "WHERE p.email = :email "
        + "ORDER BY o.date DESC")
    List<Order> findByCustomerPersonEmail(@Param("email") String email);
}
