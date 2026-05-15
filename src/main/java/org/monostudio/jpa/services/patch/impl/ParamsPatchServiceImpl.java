package org.monostudio.jpa.services.patch.impl;

import org.springframework.stereotype.Service;
import org.monostudio.api.models.ParamPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Param;
import org.monostudio.jpa.services.patch.ParamsPatchService;

import java.util.Map;

@Service
public class ParamsPatchServiceImpl
    implements ParamsPatchService {

    @Override
    public Param patchExistingEntity(Map<String, Object> changes, Param existing)
        throws BadInputException {
        // Only value is patchable — category + name form the unique key
        if (changes.containsKey("value")) {
            Object val = changes.get("value");
            if (val != null) {
                existing.setValue(String.valueOf(val));
            }
        }
        return existing;
    }

    @Override
    public Param patchExistingEntity(ParamPojo changes, Param existing)
        throws BadInputException {
        throw new UnsupportedOperationException("This method signature has been deprecated");
    }
}