package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class AddressBookPojo {

    private Long id;

    @NotBlank
    private String label;

    @Builder.Default
    private Boolean defaultShipping = false;

    @Builder.Default
    private Boolean defaultBilling = false;

    @Valid
    @NotNull
    private AddressPojo address;

    /** Read-only: creation timestamp */
    private LocalDateTime createdAt;

    /** Read-only: last update timestamp */
    private LocalDateTime updatedAt;
}
