package org.monostudio.api.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.monostudio.api.models.ShipmentTrackingWebhookPayload;
import org.monostudio.api.services.ShipmentTrackingService;
import org.monostudio.config.WebhookProperties;
import org.monostudio.jpa.entities.ShipmentTracking;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/shipping")
@Slf4j
@RequiredArgsConstructor
public class WebhookShippingController {

    private final ShipmentTrackingService trackingService;
    private final WebhookProperties webhookProperties;

    @PostMapping("/tracking")
    public ResponseEntity<String> receiveTracking(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Webhook-Token", required = false) String webhookToken,
            @RequestBody ShipmentTrackingWebhookPayload payload) {
        
        log.info("Received shipping webhook: {}", payload);

        // 1. Basic Auth Validation
        if (!isAuthorized(authHeader, webhookToken)) {
            log.warn("Unauthorized shipping webhook attempt with token: {}", authHeader);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid secret token");
        }

        // 2. Process
        try {
            trackingService.record(payload);
            return ResponseEntity.ok("Tracking saved");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error processing shipping webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error");
        }
    }

    private boolean isAuthorized(String authHeader, String webhookToken) {
        String defaultSecret = webhookProperties.getSecret();
        String ghnSecret = StringUtils.defaultIfBlank(webhookProperties.getGhnSecret(), defaultSecret);
        if (StringUtils.isBlank(ghnSecret)) {
            return false;
        }
        if (StringUtils.isNotBlank(authHeader) && authHeader.equals("Bearer " + ghnSecret)) {
            return true;
        }
        return StringUtils.isNotBlank(webhookToken) && webhookToken.equals(ghnSecret);
    }
}
