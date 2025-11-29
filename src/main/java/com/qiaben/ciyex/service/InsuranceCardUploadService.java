package com.qiaben.ciyex.service;

import com.qiaben.ciyex.provider.S3ClientProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class InsuranceCardUploadService {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "application/pdf"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final S3ClientProvider s3ClientProvider;

    public InsuranceCardUploadService(S3ClientProvider s3ClientProvider) {
        this.s3ClientProvider = s3ClientProvider;
    }

    /**
     * Upload insurance card image to S3 and return the URL
     *
     * @param file       The uploaded file
     * @param coverageId The coverage ID
     * @param side       "front" or "back"
     * @return The S3 URL of the uploaded file
     */
    public String uploadCard(MultipartFile file, Long coverageId, String side) throws IOException {
        validateFile(file);

        String bucket = s3ClientProvider.getBucketForCurrentOrg();
        S3Client s3Client = s3ClientProvider.getForCurrentTenant();

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String key = String.format("insurance-cards/%d/%s-%s.%s",
                coverageId,
                side,
                UUID.randomUUID().toString(),
                extension
        );

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));

            String url = String.format("https://%s.s3.amazonaws.com/%s", bucket, key);
            log.info("Uploaded insurance card {} for coverage {}: {}", side, coverageId, url);
            return url;

        } catch (Exception e) {
            log.error("Failed to upload insurance card to S3", e);
            throw new IOException("Failed to upload file to S3", e);
        }
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds maximum allowed size of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IOException("Invalid file type. Only PNG, JPEG, and PDF files are allowed");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
