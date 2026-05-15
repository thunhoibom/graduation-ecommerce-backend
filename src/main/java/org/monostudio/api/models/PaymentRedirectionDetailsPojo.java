package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper class for data needed to redirect towards payment page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude
public class PaymentRedirectionDetailsPojo {
    private String url;
    private String token;
}
