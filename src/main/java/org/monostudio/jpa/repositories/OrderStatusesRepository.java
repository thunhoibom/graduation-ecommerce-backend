package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.OrderStatus;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface OrderStatusesRepository
    extends Repository<OrderStatus> {

    Optional<OrderStatus> findByName(String name);
}
