package org.monostudio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.config.RateLimitConfig;
import org.monostudio.config.SecurityProperties;
import org.monostudio.jpa.services.crud.CustomersCrudService;

import javax.crypto.SecretKey;
import jakarta.persistence.EntityExistsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtGuestAuthenticationFilter
    extends GenericJwtAuthenticationFilter {
    private final Logger myLogger = LoggerFactory.getLogger(JwtGuestAuthenticationFilter.class);
    private final AuthenticationManager authenticationManager;
    private final CustomersCrudService customersService;
    private final SecurityProperties securityProperties;
    private final RateLimitConfig rateLimitConfig;

    public JwtGuestAuthenticationFilter(
        SecurityProperties securityProperties,
        SecretKey secretKey,
        AuthenticationManager authenticationManager,
        CustomersCrudService customersService,
        RateLimitConfig rateLimitConfig
    ) {
        super(securityProperties, secretKey);
        this.securityProperties = securityProperties;
        this.authenticationManager = authenticationManager;
        this.customersService = customersService;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        } else {
            String clientIp = getClientIp(request);
            Bucket bucket = rateLimitConfig.guestBucketFor(clientIp);
            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/json");
                try {
                    response.getWriter().write(
                        "{\"code\":\"RATE_01\",\"message\":\"Too many guest registration attempts. Please wait before trying again.\",\"canRetry\":true}");
                } catch (IOException e) {
                    // ignore
                }
                return null;
            }
            try {
                PersonPojo guestCustomerData = new ObjectMapper().readValue(request.getInputStream(), PersonPojo.class);
                this.saveCustomerData(guestCustomerData);
                final String credential = securityProperties.getGuestUserName();
                Authentication authentication = new UsernamePasswordAuthenticationToken(credential, credential);
                return authenticationManager.authenticate(authentication);
            } catch (IOException e) {
                throw new BadCredentialsException("Invalid request body for guest session");
            } catch (BadInputException e) {
                throw new BadCredentialsException("Insufficient or invalid profile data for guest");
            }
        }
    }

    private void saveCustomerData(PersonPojo guestData) throws BadInputException {
        try {
            customersService.create(guestData);
        } catch (EntityExistsException e) {
            myLogger.info("Guest with idNumber={} is already registered in the database", guestData.getIdNumber());
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
