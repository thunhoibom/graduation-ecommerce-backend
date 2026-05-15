package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@JsonInclude
public class UserRolePojo {
    private Long id;
    @NotNull
    private String name;
}
