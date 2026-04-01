package org.monostudio.jpa.services.conversion.impl;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ShipperPojo;
import org.monostudio.jpa.entities.Shipper;
import org.monostudio.jpa.services.conversion.ShippersConverterService;

@Service
@NoArgsConstructor
public class ShippersConverterServiceImpl
    implements ShippersConverterService {

    @Override
    public ShipperPojo convertToPojo(Shipper source) {
        return ShipperPojo.builder()
            .name(source.getName())
            .build();
    }

    @Override
    public Shipper convertToNewEntity(ShipperPojo source) {
        return Shipper.builder()
            .name(source.getName())
            .build();
    }

    @Override
    public Shipper applyChangesToExistingEntity(ShipperPojo source, Shipper target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
