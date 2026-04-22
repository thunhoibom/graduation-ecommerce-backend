package org.monostudio.api.models.inventory;

import lombok.Data;

@Data
public class SupplierUpsertRequest {
    private String code;
    private String name;
    private String contactName;
    private String phone;
    private String email;
    private String address;
    private Boolean active;
}
