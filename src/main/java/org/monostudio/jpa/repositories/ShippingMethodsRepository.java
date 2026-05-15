package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.ShippingMethod;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface ShippingMethodsRepository
    extends Repository<ShippingMethod> {

    Optional<ShippingMethod> findByName(String name);

    List<ShippingMethod> findByActiveTrue();
}
