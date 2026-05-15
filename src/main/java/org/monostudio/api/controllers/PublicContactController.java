package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.ContactInquiryCreatedResponse;
import org.monostudio.api.models.ContactInquiryRequest;
import org.monostudio.api.services.ContactInquiryService;

@RestController
@RequestMapping("/api/public/contact")
@Tag(name = "Contact")
public class PublicContactController {

    private final ContactInquiryService contactInquiryService;

    @Autowired
    public PublicContactController(ContactInquiryService contactInquiryService) {
        this.contactInquiryService = contactInquiryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a contact form message (public)")
    public ContactInquiryCreatedResponse submit(
        @Valid @RequestBody ContactInquiryRequest body,
        HttpServletRequest request
    ) {
        return contactInquiryService.submit(body, request);
    }
}
