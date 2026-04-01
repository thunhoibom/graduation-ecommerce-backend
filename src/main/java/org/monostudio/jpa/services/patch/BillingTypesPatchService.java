package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.BillingTypePojo;
import org.monostudio.jpa.entities.BillingType;
import org.monostudio.jpa.services.PatchService;

public interface BillingTypesPatchService
    extends PatchService<BillingTypePojo, BillingType> {
}
