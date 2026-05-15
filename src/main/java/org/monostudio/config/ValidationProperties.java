package org.monostudio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Data
@Component
@ConfigurationProperties(prefix = "monostudio.validation")
@Validated
public class ValidationProperties {
    @NotBlank
    private String idNumberRegexp;
    @NotBlank
    private String phoneNumberRegexp;
}
