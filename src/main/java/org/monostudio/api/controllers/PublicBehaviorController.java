package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.monostudio.api.models.BehaviorEventRequestPojo;
import org.monostudio.api.services.UserBehaviorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/behavior")
@Tag(name = "Public behavior")
public class PublicBehaviorController {

    private final UserBehaviorService userBehaviorService;

    public PublicBehaviorController(UserBehaviorService userBehaviorService) {
        this.userBehaviorService = userBehaviorService;
    }

    @PostMapping("/events")
    @Operation(summary = "Record a lightweight behavior event (search, product view)")
    public ResponseEntity<Void> postEvent(@RequestBody BehaviorEventRequestPojo body) {
        userBehaviorService.recordBestEffort(body);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
