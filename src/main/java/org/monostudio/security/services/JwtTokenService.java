package org.monostudio.security.services;

import io.jsonwebtoken.Jwts;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.monostudio.config.SecurityProperties;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.util.Collection;
import java.util.Date;

import static org.monostudio.config.Constants.JWT_CLAIM_AUTHORITIES;
import static org.monostudio.config.Constants.JWT_PREFIX;

@Service
public class JwtTokenService {
    private final SecurityProperties securityProperties;
    private final SecretKey secretKey;

    public JwtTokenService(SecurityProperties securityProperties, SecretKey secretKey) {
        this.securityProperties = securityProperties;
        this.secretKey = secretKey;
    }

    public String issueToken(String subject, Collection<? extends GrantedAuthority> authorities) {
        int minutesToExpire = securityProperties.getJwtExpirationAfterMinutes();
        int hoursToExpire = securityProperties.getJwtExpirationAfterHours();
        int daysToExpire = securityProperties.getJwtExpirationAfterDays();

        Instant now = Instant.now();
        Instant expiration = now.plus(Period.ofDays(daysToExpire))
            .plus(Duration.ofHours(hoursToExpire))
            .plus(Duration.ofMinutes(minutesToExpire));

        return Jwts.builder()
            .setSubject(subject)
            .claim(JWT_CLAIM_AUTHORITIES, authorities)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiration))
            .signWith(secretKey)
            .compact();
    }

    public String issueBearerToken(String subject, Collection<? extends GrantedAuthority> authorities) {
        return JWT_PREFIX + issueToken(subject, authorities);
    }
}
