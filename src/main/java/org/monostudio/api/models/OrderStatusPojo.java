package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Data
@Builder
@JsonInclude
public class OrderStatusPojo {
    @NotBlank
    private Integer code;
    @JsonInclude(NON_EMPTY)
    @NotBlank
    private String name;
}
