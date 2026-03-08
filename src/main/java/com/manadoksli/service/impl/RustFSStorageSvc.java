package com.manadoksli.service.impl;

import com.manadoksli.config.RustFsProperties;
import com.manadoksli.service.IStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RustFSStorageSvc implements IStorageService {

    private final S3Client amazonS3;
    private final RustFsProperties rustFsProperties;

    @Override
    public String upload(MultipartFile file) throws IOException {

        String extension = getExtension(file.getOriginalFilename());
        String key = UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(rustFsProperties.getBucket())
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        amazonS3.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        return rustFsProperties.getPublicUrl() + "/" + rustFsProperties.getBucket() + "/" + key;

    }

    @Override
    public void delete(String fileUrl) {
        // Extract key by stripping bucket prefix
        // e.g. http://localhost:9000/manadoksli/uuid.png → uuid.png
        String prefix = rustFsProperties.getPublicUrl() + "/" + rustFsProperties.getBucket() + "/";
        String key = fileUrl.startsWith(prefix)
                ? fileUrl.substring(prefix.length())
                : fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(rustFsProperties.getBucket())
                .key(key)
                .build();

        amazonS3.deleteObject(deleteRequest);
        log.info("Deleted file from storage: {}", key);

    }

    private String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return ".jpg";
    }
}
