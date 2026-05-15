package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.PaymentType;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface PaymentTypesRepository
    extends Repository<PaymentType> {

    Optional<PaymentType> findByName(String name);
}
