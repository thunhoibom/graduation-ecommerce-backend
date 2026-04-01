package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.BillingTypePojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.BillingType;
import org.monostudio.jpa.services.patch.BillingTypesPatchService;

import java.util.Map;

@Service
@NoArgsConstructor
public class BillingTypesPatchServiceImpl
    implements BillingTypesPatchService {

    @Override
    public BillingType patchExistingEntity(Map<String, Object> changes, BillingType existing) throws BadInputException {
        BillingType target = new BillingType(existing);

        if (changes.containsKey("name")) {
            String name = (String) changes.get("name");
            if (!StringUtils.isBlank(name)) {
                target.setName(name);
            }
        }

        return target;
    }

    @Override
    public BillingType patchExistingEntity(BillingTypePojo changes, BillingType existing) throws BadInputException {
        throw new UnsupportedOperationException("This method has been deprecated");
    }
}
