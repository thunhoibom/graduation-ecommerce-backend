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
import org.monostudio.jpa.entities.GuestSession;
import org.monostudio.jpa.repositories.GuestSessionsRepository;
import org.monostudio.security.services.JwtTokenService;

import jakarta.persistence.EntityExistsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

import static org.monostudio.config.Constants.JWT_PREFIX;

/**
 * Issues a UNIQUE JWT per guest session.
 *
 * Flow:
 * - Frontend sends X-Guest-Session-UUID header (existing token) or none (new guest)
 * - If existing session UUID is valid & not revoked → issue JWT with that UUID as subject
 * - If no UUID → create new GuestSession entity, use UUID as subject
 *
 * Each guest has their own JWT subject (session UUID) — no shared "guest" account.
 * If one guest is banned, only their specific session is revoked via GuestSession.revoked flag.
 */
public class JwtGuestAuthenticationFilter
    extends GenericJwtAuthenticationFilter {
    private final Logger myLogger = LoggerFactory.getLogger(JwtGuestAuthenticationFilter.class);
    private final AuthenticationManager authenticationManager;
    private final GuestSessionsRepository guestSessionsRepository;
    private final SecurityProperties securityProperties;
    private final RateLimitConfig rateLimitConfig;

    public JwtGuestAuthenticationFilter(
        SecurityProperties securityProperties,
        JwtTokenService jwtTokenService,
        AuthenticationManager authenticationManager,
        GuestSessionsRepository guestSessionsRepository,
        RateLimitConfig rateLimitConfig
    ) {
        super(jwtTokenService);
        this.securityProperties = securityProperties;
        this.authenticationManager = authenticationManager;
        this.guestSessionsRepository = guestSessionsRepository;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        }
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

        // 1. Read existing session UUID from header (sent from frontend localStorage)
        String existingSessionUuid = request.getHeader("X-Guest-Session-UUID");

        // 2. Determine or create unique guest session
        GuestSession guestSession;
        if (existingSessionUuid != null && !existingSessionUuid.isBlank()) {
            // Resume existing guest session
            guestSession = guestSessionsRepository.findBySessionUuid(existingSessionUuid).orElse(null);
            if (guestSession == null) {
                // UUID not found — treat as new session
                guestSession = createNewGuestSession();
            } else if (!guestSession.isValid()) {
                // Session expired or revoked — create new
                guestSession = createNewGuestSession();
            }
        } else {
            // No existing UUID — create brand new guest session
            guestSession = createNewGuestSession();
        }

        // 3. Save/update customer profile (if provided)
        try {
            PersonPojo guestData = new ObjectMapper()
                .readValue(request.getInputStream(), PersonPojo.class);
            // Update session UUID in response header so frontend can store it
            response.setHeader("X-Guest-Session-UUID", guestSession.getSessionUuid());
        } catch (IOException e) {
            // No body — OK for pure session creation
        }

        // 4. Use session UUID as JWT subject — NOT the shared "guest" username
        // This means each guest has a unique identity, not shared with others
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            guestSession.getSessionUuid(),
            null,
            java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_GUEST"))
        );
        return authenticationManager.authenticate(authentication);

    }

    private GuestSession createNewGuestSession() {
        GuestSession session = GuestSession.builder()
            .sessionUuid(GuestSession.generateSessionUuid())
            .revoked(false)
            .expiresAt(LocalDateTime.now().plusDays(30))
            .build();
        return guestSessionsRepository.saveAndFlush(session);
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
