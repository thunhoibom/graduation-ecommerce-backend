package org.monostudio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.monostudio.api.models.LoginPojo;
import org.monostudio.config.RateLimitConfig;
import org.monostudio.config.SecurityProperties;

import javax.crypto.SecretKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtLoginAuthenticationFilter
    extends GenericJwtAuthenticationFilter {
    private final AuthenticationManager authenticationManager;
    private final RateLimitConfig rateLimitConfig;

    public JwtLoginAuthenticationFilter(
        SecurityProperties jwtProperties,
        SecretKey secretKey,
        AuthenticationManager authenticationManager,
        RateLimitConfig rateLimitConfig
    ) {
        super(jwtProperties, secretKey);
        this.authenticationManager = authenticationManager;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        } else {
            String clientIp = getClientIp(request);
            Bucket bucket = rateLimitConfig.loginBucketFor(clientIp);
            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/json");
                try {
                    response.getWriter().write(
                        "{\"code\":\"RATE_01\",\"message\":\"Too many login attempts. Please wait before trying again.\",\"canRetry\":true}");
                } catch (IOException e) {
                    // ignore
                }
                return null;
            }
            try {
                LoginPojo userData = new ObjectMapper().readValue(request.getInputStream(), LoginPojo.class);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userData.getName(),
                    userData.getPassword());
                return authenticationManager.authenticate(authentication);
            } catch (IOException e) {
                throw new BadCredentialsException("Request body is not a login request");
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
