package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude
public class UserRoleWithPermissionsPojo {
    private Long id;
    private String name;
    private List<PermissionPojo> permissions;
}
