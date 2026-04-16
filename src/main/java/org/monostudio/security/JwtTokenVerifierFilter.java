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
import org.springframework.web.filter.OncePerRequestFilter;
import org.monostudio.security.services.AuthorizationHeaderParserService;

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

public class JwtTokenVerifierFilter
    extends OncePerRequestFilter {
    private final Logger myLogger = LoggerFactory.getLogger(JwtTokenVerifierFilter.class);
    private final AuthorizationHeaderParserService<Claims> jwtClaimsParserService;

    public JwtTokenVerifierFilter(
        AuthorizationHeaderParserService<Claims> jwtClaimsParserService
    ) {
        super();
        this.jwtClaimsParserService = jwtClaimsParserService;
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
                    Set<SimpleGrantedAuthority> authorities = this.extractAuthorities(tokenBody);
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
