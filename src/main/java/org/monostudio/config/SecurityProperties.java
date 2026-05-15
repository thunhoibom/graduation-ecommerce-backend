package org.monostudio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Data
@Component
@ConfigurationProperties(prefix = "monostudio.security")
@Validated
public class SecurityProperties {

    @NotBlank
    private String jwtSecretKey;
    @PositiveOrZero
    private int jwtExpirationAfterMinutes;
    @PositiveOrZero
    private int jwtExpirationAfterHours;
    @PositiveOrZero
    private int jwtExpirationAfterDays;
    @Min(6)
    private int bcryptEncoderStrength;
    private boolean guestUserEnabled;
    private String guestUserName;
    private boolean accountProtectionEnabled;
    private long protectedAccountId;
    private long guestUserRoleId;
}
