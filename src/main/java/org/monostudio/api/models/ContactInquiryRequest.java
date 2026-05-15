package org.monostudio.api.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactInquiryRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @Size(max = 40)
    @Pattern(regexp = "^$|^[0-9+\\s]{8,15}$", message = "Invalid phone format")
    private String phone;

    /**
     * Must match storefront {@code SUBJECTS} values.
     */
    @NotBlank
    @Pattern(regexp = "order|product|return|cooperation|feedback|other")
    private String subject;

    @NotBlank
    @Size(min = 10, max = 8000)
    private String message;
}
