package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.ReceiptPojo;
import org.monostudio.api.services.ReceiptService;
import org.monostudio.common.exceptions.BadInputException;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/public/receipt")
@Tag(name = "Checkout")
public class PublicReceiptController {
    private final ReceiptService receiptService;

    @Autowired
    public PublicReceiptController(
        ReceiptService receiptService
    ) {
        this.receiptService = receiptService;
    }

    /**
     * Fetch result of transaction after it has been confirmed and validated
     *
     * @param token The token used during the transaction
     * @return An object with all available data about the transaction
     * @throws BadInputException when an empty token is provided
     * @throws EntityNotFoundException when no transaction matches the provided token
     */
    @GetMapping("/{token}")
    @Operation(summary = "View a summary of an order once complete or rejected")
    public ReceiptPojo fetchReceiptById(@PathVariable("token") String token)
        throws BadInputException, EntityNotFoundException {
        if (StringUtils.isBlank(token)) {
            throw new BadInputException("An incorrect receipt token was provided");
        }
        return this.receiptService.fetchReceiptByTransactionToken(token);
    }
}
