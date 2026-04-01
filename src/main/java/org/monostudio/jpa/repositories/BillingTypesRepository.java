package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.BillingType;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface BillingTypesRepository
    extends Repository<BillingType> {

    Optional<BillingType> findByName(String name);
}
