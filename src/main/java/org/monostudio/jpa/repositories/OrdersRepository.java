package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.OrderStatus;

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
}
