package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.BillingTypePojo;
import org.monostudio.jpa.entities.BillingType;
import org.monostudio.jpa.services.ConverterService;

public interface BillingTypesConverterService
    extends ConverterService<BillingTypePojo, BillingType> {
}
