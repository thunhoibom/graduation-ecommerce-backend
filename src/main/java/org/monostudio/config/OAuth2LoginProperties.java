package org.monostudio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "monostudio.security.oauth2")
public class OAuth2LoginProperties {
    private String frontendCallbackUrl;
    private String frontendFailureUrl;
}
