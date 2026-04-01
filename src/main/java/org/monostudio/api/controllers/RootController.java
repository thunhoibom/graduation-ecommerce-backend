package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health check")
public class RootController {

    @GetMapping
    @Operation(summary = "Non-operating endpoint.")
    public ResponseEntity<Void> defaultMapping() {
        return ResponseEntity.ok().build();
    }
}
