package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.DiscountCodePojo;
import org.monostudio.jpa.entities.DiscountCode;
import org.monostudio.jpa.services.ConverterService;

public interface DiscountCodesConverterService
    extends ConverterService<DiscountCodePojo, DiscountCode> {
}
