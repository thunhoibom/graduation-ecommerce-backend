package org.monostudio.config;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.monostudio.config.exceptions.CorsMappingParseException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates an instance of CorsConfigurationSource using information from an instance of CorsProperties
 */
public class CorsConfigurationSourceBuilder {
    private static final Long MAX_AGE = 300L;
    private final CorsConfiguration config;
    private final String listDelimiter;
    private final Map<String, String> mappings;

    public CorsConfigurationSourceBuilder(String listDelimiter) {
        this.listDelimiter = listDelimiter;
        this.config = new CorsConfiguration();
        this.config.setAllowCredentials(true);
        this.config.setMaxAge(MAX_AGE);
        this.mappings = new HashMap<>();
    }

    public CorsConfigurationSourceBuilder allowedHeaders(String allowedHeadersString) {
        List<String> headersList = Arrays.asList(allowedHeadersString.split(this.listDelimiter));
        this.config.setAllowedHeaders(headersList);
        return this;
    }

    public CorsConfigurationSourceBuilder allowedOrigins(String allowedOriginsString) {
        List<String> originsList = Arrays.asList(allowedOriginsString.split(this.listDelimiter));
        this.config.setAllowedOrigins(originsList);
        return this;
    }

    public CorsConfigurationSourceBuilder corsMappings(String corsMappings) throws CorsMappingParseException {
        for (String chunk : corsMappings.split(this.listDelimiter)) {
            String[] mapping = chunk.split(" ");
            try {
                String method = mapping[0] + ",HEAD,OPTIONS";
                String path = mapping[1];
                this.mappings.put(path, method);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new CorsMappingParseException(chunk);
            }
        }
        return this;
    }

    public CorsConfigurationSource build() {
        UrlBasedCorsConfigurationSource cfg = new UrlBasedCorsConfigurationSource();
        for (Map.Entry<String, String> properties : mappings.entrySet()) {
            List<String> methods = Arrays.asList(properties.getValue().split(","));
            CorsConfiguration pathConfig = new CorsConfiguration(config);
            pathConfig.setAllowedMethods(methods);
            cfg.registerCorsConfiguration(properties.getKey(), pathConfig);
        }
        return cfg;
    }
}
