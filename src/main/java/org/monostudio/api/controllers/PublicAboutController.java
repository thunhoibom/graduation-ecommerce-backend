package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.CompanyDetailsPojo;
import org.monostudio.api.services.CompanyService;

@RestController
@RequestMapping("/public/about")
@Tag(name = "About")
public class PublicAboutController {

    private final CompanyService companyService;

    @Autowired
    public PublicAboutController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @Operation(summary = "View information useful to customers regarding the business.")
    public CompanyDetailsPojo readCompanyDetails() {
        return companyService.readDetails();
    }
}
