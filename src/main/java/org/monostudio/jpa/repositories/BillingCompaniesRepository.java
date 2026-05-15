package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.BillingCompany;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface BillingCompaniesRepository
    extends Repository<BillingCompany> {

    Optional<BillingCompany> findByIdNumber(String idNumber);
}
