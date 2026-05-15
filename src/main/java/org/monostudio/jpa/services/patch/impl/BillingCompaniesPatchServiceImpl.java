package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.BillingCompanyPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.BillingCompany;
import org.monostudio.jpa.services.patch.BillingCompaniesPatchService;

import java.util.Map;

@Service
@NoArgsConstructor
public class BillingCompaniesPatchServiceImpl
    implements BillingCompaniesPatchService {

    @Override
    public BillingCompany patchExistingEntity(Map<String, Object> changes, BillingCompany existing) throws BadInputException {
        BillingCompany target = new BillingCompany(existing);

        if (changes.containsKey("idNumber")) {
            String idNumber = (String) changes.get("idNumber");
            if (!StringUtils.isBlank(idNumber)) {
                target.setIdNumber(idNumber);
            }
        }

        if (changes.containsKey("name")) {
            String name = (String) changes.get("name");
            if (!StringUtils.isBlank(name)) {
                target.setName(name);
            }
        }

        return target;
    }

    @Override
    public BillingCompany patchExistingEntity(BillingCompanyPojo changes, BillingCompany target) throws BadInputException {
        throw new UnsupportedOperationException("This method has been deprecated");
    }
}
