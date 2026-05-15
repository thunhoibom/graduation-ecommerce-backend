package org.monostudio.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class JwtKeyConfig {

    @Bean
    public SecretKey secretKey(SecurityProperties jwtProperties) {
        return Keys.hmacShaKeyFor(jwtProperties.getJwtSecretKey().getBytes());
    }
}
