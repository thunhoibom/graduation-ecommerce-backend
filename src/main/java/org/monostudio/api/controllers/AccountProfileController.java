package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.api.services.ProfileService;
import org.monostudio.common.exceptions.BadInputException;

import jakarta.persistence.EntityNotFoundException;
import java.security.Principal;

@RestController
@RequestMapping("/api/account/profile")
@Tag(name = "User Accounts")
@PreAuthorize("isAuthenticated()")
public class AccountProfileController {
    private final ProfileService userProfileService;

    @Autowired
    public AccountProfileController(
        ProfileService userProfileService
    ) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    @Operation(summary = "View stored profile information")
    public PersonPojo getProfile(Principal principal)
        throws EntityNotFoundException {
        String username = principal.getName();
        return userProfileService.getProfileFromUserName(username);
    }

    @PutMapping
    @Operation(summary = "Replace stored profile information")
    public void updateProfile(Principal principal, @RequestBody PersonPojo newProfile)
        throws EntityNotFoundException, BadInputException {
        String username = principal.getName();
        userProfileService.updateProfileForUserWithName(username, newProfile);
    }
}
