package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.monostudio.common.exceptions.BadInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.AddressBookPojo;
import org.monostudio.jpa.repositories.UsersRepository;
import org.monostudio.jpa.services.crud.AddressBookCrudService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public/address-book")
@Tag(name = "Address Book")
@PreAuthorize("isAuthenticated()")
public class PublicAddressBookController {

    private final AddressBookCrudService addressBookCrudService;
    private final UsersRepository usersRepository;

    @Autowired
    public PublicAddressBookController(
        AddressBookCrudService addressBookCrudService,
        UsersRepository usersRepository
    ) {
        this.addressBookCrudService = addressBookCrudService;
        this.usersRepository = usersRepository;
    }

    /**
     * List all saved addresses for the current user.
     */
    @GetMapping
    @Operation(summary = "List all saved addresses for the authenticated user")
    public List<AddressBookPojo> list(Principal principal) {
        Long userId = resolveUserId(principal);
        return addressBookCrudService.readManyByUser(userId);
    }

    /**
     * Get a specific address entry by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a specific saved address entry")
    public AddressBookPojo get(@PathVariable Long id, Principal principal) {
        Long userId = resolveUserId(principal);
        return addressBookCrudService.readOneByIdAndUser(id, userId)
            .orElseThrow(() -> new EntityNotFoundException("Address not found"));
    }

    /**
     * Save a new address to the address book.
     */
    @PostMapping
    @Operation(summary = "Save a new address to the address book")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressBookPojo create(@Valid @RequestBody AddressBookPojo input, Principal principal) throws BadInputException {
        Long userId = resolveUserId(principal);
        return addressBookCrudService.createForUser(input, userId);
    }

    /**
     * Replace an existing address entry.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Replace an existing address entry")
    public AddressBookPojo replace(
        @PathVariable Long id,
        @Valid @RequestBody AddressBookPojo input,
        Principal principal
    ) throws BadInputException {
        Long userId = resolveUserId(principal);
        return addressBookCrudService.replace(input, id, userId);
    }

    /**
     * Partially update an address entry.
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Partially update an address entry")
    public AddressBookPojo partialUpdate(
        @PathVariable Long id,
        @RequestBody Map<String, Object> changes,
        Principal principal
    ) throws BadInputException {
        Long userId = resolveUserId(principal);
        return addressBookCrudService.partialUpdate(changes, id, userId);
    }

    /**
     * Delete an address entry.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an address from the address book")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal principal) {
        Long userId = resolveUserId(principal);
        addressBookCrudService.delete(id, userId);
    }

    private Long resolveUserId(Principal principal) {
        String username = principal.getName();
        return usersRepository.findByName(username)
            .orElseThrow(() -> new EntityNotFoundException("User not found"))
            .getId();
    }
}
