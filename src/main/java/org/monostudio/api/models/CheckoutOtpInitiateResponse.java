package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude
public class CheckoutOtpInitiateResponse {
    private Long orderId;
    private String maskedEmail;
    private Instant expiresAt;
    private int resendAfterSeconds;
}
