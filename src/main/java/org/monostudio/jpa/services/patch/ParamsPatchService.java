package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.ParamPojo;
import org.monostudio.jpa.entities.Param;
import org.monostudio.jpa.services.PatchService;

public interface ParamsPatchService
    extends PatchService<ParamPojo, Param> {
}