package org.monostudio.security;

import io.jsonwebtoken.Jwts;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.monostudio.config.SecurityProperties;

import javax.crypto.SecretKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.util.Date;

import static org.monostudio.config.Constants.JWT_CLAIM_AUTHORITIES;
import static org.monostudio.config.Constants.JWT_PREFIX;

/**
 * Abstract filter that writes a Bearer token to the response upon a succesful authentication call
 */
public abstract class GenericJwtAuthenticationFilter
    extends UsernamePasswordAuthenticationFilter {
    private final SecurityProperties jwtProperties;
    private final SecretKey secretKey;

    protected GenericJwtAuthenticationFilter(
        SecurityProperties jwtProperties,
        SecretKey secretKey
    ) {
        this.jwtProperties = jwtProperties;
        this.secretKey = secretKey;
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult)
        throws IOException {
        int minutesToExpire = jwtProperties.getJwtExpirationAfterMinutes();
        int hoursToExpire = jwtProperties.getJwtExpirationAfterHours();
        int daysToExpire = jwtProperties.getJwtExpirationAfterDays();

        Instant now = Instant.now();
        Instant expiration = now.plus(Period.ofDays(daysToExpire))
            .plus(Duration.ofHours(hoursToExpire))
            .plus(Duration.ofMinutes(minutesToExpire));

        String token = Jwts.builder()
            .setSubject(authResult.getName())
            .claim(JWT_CLAIM_AUTHORITIES, authResult.getAuthorities())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiration))
            .signWith(secretKey)
            .compact();

        String headerValue = JWT_PREFIX + token;
        response.addHeader(HttpHeaders.AUTHORIZATION, headerValue);
        response.getWriter().write(headerValue);
    }
}
