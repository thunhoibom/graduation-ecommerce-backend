package org.monostudio.api.controllers;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.monostudio.api.services.AdminNotificationService;
import org.monostudio.security.services.AuthorizationHeaderParserService;

import java.util.Collection;
import java.util.Map;

import static org.monostudio.config.Constants.JWT_CLAIM_AUTHORITIES;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/data/notifications")
@Tag(name = "Admin Notifications")
public class AdminNotificationsController {
    private final AuthorizationHeaderParserService<Claims> jwtClaimsParserService;
    private final AdminNotificationService adminNotificationService;

    @Autowired
    public AdminNotificationsController(
        AuthorizationHeaderParserService<Claims> jwtClaimsParserService,
        AdminNotificationService adminNotificationService
    ) {
        this.jwtClaimsParserService = jwtClaimsParserService;
        this.adminNotificationService = adminNotificationService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Open SSE stream for admin notifications")
    public SseEmitter stream(@RequestParam("accessToken") String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Missing access token");
        }

        Claims claims;
        try {
            claims = jwtClaimsParserService.parseToken(accessToken);
        } catch (Exception ex) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid access token");
        }

        if (!hasAdminOrdersReadAccess(claims)) {
            throw new ResponseStatusException(FORBIDDEN, "Insufficient privileges");
        }

        String principalKey = claims.getSubject();
        return adminNotificationService.subscribe(principalKey);
    }

    @SuppressWarnings("unchecked")
    private boolean hasAdminOrdersReadAccess(Claims claims) {
        Object authoritiesObject = claims.get(JWT_CLAIM_AUTHORITIES);
        if (!(authoritiesObject instanceof Collection<?> authorityMaps)) {
            return false;
        }

        return authorityMaps.stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(map -> map.get("authority"))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .anyMatch(authority -> authority.equals("orders:read") || authority.equals("orders:update"));
    }
}
