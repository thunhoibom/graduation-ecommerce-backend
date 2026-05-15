package org.monostudio.api.services;

import org.springframework.web.multipart.MultipartFile;
import org.monostudio.api.models.ImagePojo;

public interface StorageService {
    ImagePojo uploadImage(MultipartFile file);
    void deleteImage(String filename);
}
