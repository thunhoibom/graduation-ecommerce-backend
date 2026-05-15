package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@JsonInclude
public class PaymentTypePojo {
    @NotBlank
    private String name;
}
