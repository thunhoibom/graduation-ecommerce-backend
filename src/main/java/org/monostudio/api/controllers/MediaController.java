package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.monostudio.api.models.ImagePojo;
import org.monostudio.api.services.StorageService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.services.crud.ImagesCrudService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@Tag(name = "Media Management")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MediaController {

    private final StorageService storageService;
    private final ImagesCrudService imagesCrudService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image to MinIO and save to database")
    @PreAuthorize("isAuthenticated()")
    public ImagePojo uploadImage(@RequestParam("file") MultipartFile file) throws BadInputException {
        ImagePojo uploaded = storageService.uploadImage(file);
        // Save to database
        return imagesCrudService.create(uploaded);
    }
}
