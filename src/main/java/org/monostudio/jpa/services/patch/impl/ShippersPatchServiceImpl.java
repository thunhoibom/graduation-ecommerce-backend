package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ShipperPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Shipper;
import org.monostudio.jpa.services.patch.ShippersPatchService;

import java.util.Map;

@Service
@NoArgsConstructor
public class ShippersPatchServiceImpl
    implements ShippersPatchService {

    @Override
    public Shipper patchExistingEntity(Map<String, Object> changes, Shipper existing) throws BadInputException {
        Shipper target = new Shipper(existing);

        if (changes.containsKey("name")) {
            String name = (String) changes.get("name");
            if (!StringUtils.isBlank(name)) {
                target.setName(name);
            }
        }

        return target;
    }

    @Override
    public Shipper patchExistingEntity(ShipperPojo changes, Shipper existing) throws BadInputException {
        throw new UnsupportedOperationException("This method has been deprecated");
    }
}
