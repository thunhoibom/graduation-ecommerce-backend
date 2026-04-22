package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class SupplierPojo {
    private Long id;
    private String code;
    private String name;
    private String contactName;
    private String phone;
    private String email;
    private String address;
    private boolean active;
    private boolean deleted;
    private String createdAt;
    private String updatedAt;
}
