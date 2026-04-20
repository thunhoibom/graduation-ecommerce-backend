package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.ParamPojo;
import org.monostudio.jpa.entities.Param;
import org.monostudio.jpa.services.ConverterService;

public interface ParamsConverterService
    extends ConverterService<ParamPojo, Param> {
}
