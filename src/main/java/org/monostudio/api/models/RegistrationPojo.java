package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@JsonInclude
public class RegistrationPojo {
    @NotBlank
    private String name;
    @NotBlank
    private String password;
    @Valid
    private PersonPojo profile;
}
