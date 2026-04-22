package org.monostudio.api.services;

import java.util.List;
import org.monostudio.api.models.SupplierPojo;
import org.monostudio.api.models.inventory.SupplierUpsertRequest;

public interface SupplierManagementService {
    List<SupplierPojo> list(String keyword);
    SupplierPojo getById(Long id);
    SupplierPojo create(SupplierUpsertRequest request);
    SupplierPojo update(Long id, SupplierUpsertRequest request);
    void softDelete(Long id);
}
