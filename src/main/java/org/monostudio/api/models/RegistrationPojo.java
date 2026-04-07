package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;

@Data
@Builder
@JsonInclude
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationPojo {
    @NotBlank
    private String name;
    @NotBlank
    private String password;
    @Valid
    private PersonPojo profile;
}
