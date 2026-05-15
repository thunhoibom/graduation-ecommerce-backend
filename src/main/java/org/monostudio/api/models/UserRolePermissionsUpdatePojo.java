package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@JsonInclude
public class UserRolePermissionsUpdatePojo {
    @NotNull
    private List<String> permissionCodes;
}
