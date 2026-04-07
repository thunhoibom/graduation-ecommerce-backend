package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.RegistrationPojo;
import org.monostudio.api.services.RegistrationService;
import org.monostudio.common.exceptions.BadInputException;

import jakarta.persistence.EntityExistsException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/public/register")
@Tag(name = "User Accounts")
public class PublicRegisterController {
    private final RegistrationService registrationService;

    @Autowired
    public PublicRegisterController(
        RegistrationService registrationService
    ) {
        this.registrationService = registrationService;
    }

    @PostMapping
    @Operation(summary = "Request creation of new user account.")
    public void register(@Valid @RequestBody RegistrationPojo userProfile)
        throws BadInputException, EntityExistsException {
        this.registrationService.register(userProfile);
    }
}
