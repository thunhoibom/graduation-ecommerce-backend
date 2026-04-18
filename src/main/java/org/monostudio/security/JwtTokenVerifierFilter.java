package org.monostudio.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.monostudio.jpa.repositories.GuestSessionsRepository;
import org.monostudio.security.services.AuthorizationHeaderParserService;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.monostudio.config.Constants.JWT_CLAIM_AUTHORITIES;
import static org.monostudio.config.Constants.JWT_PREFIX;

@Component
public class JwtTokenVerifierFilter
    extends OncePerRequestFilter {
    private final Logger myLogger = LoggerFactory.getLogger(JwtTokenVerifierFilter.class);
    private final AuthorizationHeaderParserService<Claims> jwtClaimsParserService;
    private final GuestSessionsRepository guestSessionsRepository;

    @Autowired
    public JwtTokenVerifierFilter(
        AuthorizationHeaderParserService<Claims> jwtClaimsParserService,
        GuestSessionsRepository guestSessionsRepository
    ) {
        super();
        this.jwtClaimsParserService = jwtClaimsParserService;
        this.guestSessionsRepository = guestSessionsRepository;
    }

    private Set<SimpleGrantedAuthority> extractAuthorities(Claims tokenBody) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> jwsAuthorityMap = (List<Map<String, String>>) tokenBody.get(JWT_CLAIM_AUTHORITIES);
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        for (Map<String, String> authorityKeyValuePair : jwsAuthorityMap) {
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(authorityKeyValuePair.get("authority"));
            authorities.add(authority);
        }
        return authorities;
    }

    /**
     * Checks if a guest session UUID has been revoked.
     * Returns true (allow) if not a guest UUID, or if guest session is valid.
     * Returns false (deny) if guest session is revoked or expired.
     */
    private boolean isGuestSessionValid(String subject) {
        if (subject == null || subject.length() != 36 || !subject.contains("-")) {
            return true; // Not a guest session UUID — allow through
        }
        return guestSessionsRepository.findBySessionUuid(subject)
            .map(session -> !session.isRevoked() && !session.isExpired())
            .orElse(true); // Unknown session — let it fail on expiry check instead
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain)
        throws ServletException, IOException, IllegalStateException {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.isBlank(authorizationHeader)) {
            filterChain.doFilter(request, response);
        } else {
            String jwt = authorizationHeader.replace(JWT_PREFIX, "");
            try {
                Claims tokenBody = jwtClaimsParserService.parseToken(jwt);
                Instant expiration = tokenBody.getExpiration().toInstant();
                Instant now = Instant.now();
                if (expiration.isAfter(now)) {
                    String username = tokenBody.getSubject();
                    // Revocation check for guest session UUIDs
                    if (!isGuestSessionValid(username)) {
                        myLogger.info("Access denied: guest session '{}' is revoked or expired", username);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"code\":\"AUTH_02\",\"message\":\"Session revoked. Please start a new session.\",\"canRetry\":true}");
                        return;
                    }
                    Set<SimpleGrantedAuthority> authorities = this.extractAuthorities(tokenBody);
                    myLogger.info("JWT verified for user '{}' — authorities: {}", username, authorities);
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                filterChain.doFilter(request, response);
            } catch (JwtException | NullPointerException | IllegalStateException exc) {
                myLogger.info("Access denied: '{}' '{}' used an invalid token '{}': {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    jwt,
                    exc.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"AUTH_01\",\"message\":\"Invalid or expired token\",\"canRetry\":true}");
            }
        }
    }
}
