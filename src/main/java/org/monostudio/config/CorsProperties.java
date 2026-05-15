package org.monostudio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Data
@Component
@ConfigurationProperties(prefix = "monostudio.cors")
@Validated
public class CorsProperties {

    @NotBlank
    private String allowedHeaders;
    @NotBlank
    private String allowedOrigins;
    @NotBlank
    private String mappings;
    @NotBlank
    private String listDelimiter;

}
