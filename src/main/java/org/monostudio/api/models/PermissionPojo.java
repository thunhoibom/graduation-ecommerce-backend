package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude
public class PermissionPojo {
    private String code;
    private String description;
}
