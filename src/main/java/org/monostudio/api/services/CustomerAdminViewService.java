package org.monostudio.api.services;

import jakarta.persistence.EntityNotFoundException;
import org.monostudio.api.models.CustomerAdminPojo;

public interface CustomerAdminViewService {

    CustomerAdminPojo buildAdminDetail(Long customerId) throws EntityNotFoundException;
}
