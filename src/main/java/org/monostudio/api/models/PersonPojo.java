package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class PersonPojo {
    /**
     * Set when this POJO is produced from a {@link org.monostudio.jpa.entities.Customer} (list/read).
     */
    @JsonProperty(access = Access.READ_ONLY)
    private Long customerId;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String idNumber;
    @NotBlank
    @jakarta.validation.constraints.Email(message = "Invalid email format")
    private String email;
    @JsonProperty("phone1")
    @JsonAlias("phone")
    private String phone1;
    // @Pattern(regexp = "^(((\\(\\+?[0-9]{3}\\))|(\\+?[0-9]{3})) ?)?[0-9]{3,4}[ -]?[0-9]{4}$")
    private String phone2;

    /** Filled when this POJO is built from a {@link org.monostudio.jpa.entities.Customer} (list/read). */
    @JsonProperty(access = Access.READ_ONLY)
    private Long orderCount;

    @JsonProperty(access = Access.READ_ONLY)
    private String loyaltyTier;

    @JsonProperty(access = Access.READ_ONLY)
    private Integer loyaltyPointsBalance;

    /** Rolling monthly spend in the same minor unit as {@code customer_monthly_spend_cents} in DB (typically VND đồng as integer). */
    @JsonProperty(access = Access.READ_ONLY)
    private Integer monthlySpendCents;

    /** True if a {@code User} row links to the same {@code Person} (registered login). */
    @JsonProperty(access = Access.READ_ONLY)
    private Boolean linkedAccount;
}
