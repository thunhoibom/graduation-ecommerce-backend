package org.monostudio.api.services.impl;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.monostudio.api.models.ImagePojo;
import org.monostudio.api.services.StorageService;
import org.monostudio.config.MinioProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
public class MinioStorageServiceImpl implements StorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioStorageServiceImpl(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
            }
        } catch (Exception e) {
            log.error("Error ensuring bucket exists: {}", e.getMessage());
        }
    }

    @Override
    public ImagePojo uploadImage(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.length() > 210) {
            originalFilename = originalFilename.substring(originalFilename.length() - 210);
        }
        String filename = UUID.randomUUID().toString() + "_" + (originalFilename != null ? originalFilename : "unnamed");
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(filename)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            String url = minioProperties.getPublicUrl() + "/" + minioProperties.getBucketName() + "/" + filename;

            return ImagePojo.builder()
                    .filename(filename)
                    .url(url)
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .code(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            log.error("Error uploading file to MinIO: {}", e.getMessage());
            throw new RuntimeException("Could not upload file", e);
        }
    }

    @Override
    public void deleteImage(String filename) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(filename)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error deleting file from MinIO: {}", e.getMessage());
        }
    }
}
