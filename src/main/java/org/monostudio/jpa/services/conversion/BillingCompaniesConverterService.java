package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.BillingCompanyPojo;
import org.monostudio.jpa.entities.BillingCompany;
import org.monostudio.jpa.services.ConverterService;

public interface BillingCompaniesConverterService
    extends ConverterService<BillingCompanyPojo, BillingCompany> {
}
