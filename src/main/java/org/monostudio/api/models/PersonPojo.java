package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String idNumber;
    @NotBlank
    @jakarta.validation.constraints.Email(message = "Invalid email format")
    private String email;
    // @Pattern(regexp = "^(((\\(\\+?[0-9]{3}\\))|(\\+?[0-9]{3})) ?)?[0-9]{3,4}[ -]?[0-9]{4}$")
    private String phone1;
    // @Pattern(regexp = "^(((\\(\\+?[0-9]{3}\\))|(\\+?[0-9]{3})) ?)?[0-9]{3,4}[ -]?[0-9]{4}$")
    private String phone2;
}
