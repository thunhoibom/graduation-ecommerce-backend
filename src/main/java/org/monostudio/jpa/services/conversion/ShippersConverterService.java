package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.ShipperPojo;
import org.monostudio.jpa.entities.Shipper;
import org.monostudio.jpa.services.ConverterService;

public interface ShippersConverterService
    extends ConverterService<ShipperPojo, Shipper> {
}
