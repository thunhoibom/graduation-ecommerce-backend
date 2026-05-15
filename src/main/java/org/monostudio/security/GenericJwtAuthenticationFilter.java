package org.monostudio.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.monostudio.security.services.JwtTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Abstract filter that writes a Bearer token to the response upon a succesful authentication call
 */
public abstract class GenericJwtAuthenticationFilter
    extends UsernamePasswordAuthenticationFilter {
    private final JwtTokenService jwtTokenService;

    protected GenericJwtAuthenticationFilter(
        JwtTokenService jwtTokenService
    ) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult)
        throws IOException {
        String headerValue = jwtTokenService.issueBearerToken(authResult.getName(), authResult.getAuthorities());
        response.addHeader(HttpHeaders.AUTHORIZATION, headerValue);
        response.getWriter().write(headerValue);
    }
}
