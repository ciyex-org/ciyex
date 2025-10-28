package com.qiaben.ciyex.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.MessageAttachmentDto;
import com.qiaben.ciyex.entity.MessageAttachment;
import com.qiaben.ciyex.repository.MessageAttachmentRepository;
import com.qiaben.ciyex.util.EncryptionUtil;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider.S3Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessageAttachmentService {

    private final MessageAttachmentRepository repository;
    private final OrgIntegrationConfigProvider configProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessageAttachmentService(MessageAttachmentRepository repository,
                                    OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.configProvider = configProvider;
    }

    @Transactional
    public MessageAttachmentDto create(Long messageId, MessageAttachmentDto dto, MultipartFile file) {
        // 1. File size validation (using default limits for now - could be configurable later)
        long fileSize = file.getSize();
        long maxSize = 10 * 1024 * 1024; // 10MB default
        if (fileSize > maxSize) {
            throw new RuntimeException("File exceeds max upload size (" + maxSize + " bytes)");
        }

        // 2. File type validation (basic check for now)
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new RuntimeException("Invalid file name");
        }

        // 3. Prepare file bytes
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file content", e);
        }

        // 4. Encrypt if enabled (for now, default to false - could be configurable later)
        boolean encryptionEnabled = false; // TODO: Make this configurable
        String base64Key = null;
        String base64Iv = null;
        if (encryptionEnabled) {
            try {
                SecretKey key = EncryptionUtil.generateKey();
                byte[] iv = new byte[12];
                new SecureRandom().nextBytes(iv);
                fileBytes = EncryptionUtil.encrypt(fileBytes, key, iv);
                base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
                base64Iv = Base64.getEncoder().encodeToString(iv);
                log.info("Applied encryption for message attachment file={} Tenant={}", file.getOriginalFilename());
            } catch (Exception e) {
                throw new RuntimeException("Encryption failed", e);
            }
        }

        // 5. Upload to S3 (or store locally for testing)
        S3Config s3Config;
        try {
            s3Config = configProvider.getS3DocumentStorage();
        } catch (Exception e) {
            // For testing/development, create a mock S3 config
            log.warn("S3 config not found for Tenant={}, using local storage for testing");
            s3Config = new S3Config();
/*            s3Config.setBucket("test-bucket");
            s3Config.setRegion("us-east-1");
            s3Config.setAccessKey("test-key");
            s3Config.setSecretKey("test-secret");*/
        }

        String key = "message-attachments/" + "/" + messageId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        // For testing, we'll skip actual S3 upload and just store the file path
        if (s3Config.getBucket().equals("test-bucket")) {
            log.info("Skipping S3 upload for testing - would upload to bucket={} key={}", s3Config.getBucket(), key);
        } else {
            S3Client s3 = buildS3Client(s3Config);
            try {
                s3.putObject(
                        PutObjectRequest.builder()
                                .bucket(s3Config.getBucket())
                                .key(key)
                                .contentType(file.getContentType())
                                .build(),
                        RequestBody.fromBytes(fileBytes)
                );
                log.info("Uploaded message attachment to S3 bucket={} key={}", s3Config.getBucket(), key);
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload file to S3", e);
            }
        }

        // 6. Save metadata in DB
        dto.setMessageId(messageId);
        dto.setFileName(file.getOriginalFilename());
        dto.setContentType(file.getContentType());
        dto.setS3Bucket(s3Config.getBucket());
        dto.setS3Key(key);

        MessageAttachment entity = mapToEntity(dto);
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedDate(LocalDateTime.now());
        entity.setEncryptionKey(base64Key);
        entity.setIv(base64Iv);
        entity.setFileSize(fileSize);

        repository.save(entity);
        dto.setId(entity.getId());
        dto.setContent(null);
        dto.setEncrypted(entity.getEncryptionKey() != null && entity.getIv() != null);
        return dto;
    }

    @Transactional
    public void delete(Long attachmentId) {
        MessageAttachment attachment = repository.findByIdAndOrgId(attachmentId)
                .orElseThrow(() -> new RuntimeException("Message attachment not found"));

        S3Config s3Config;
        try {
            s3Config = configProvider.getS3DocumentStorage();
        } catch (Exception e) {
            // For testing/development, create a mock S3 config
            log.warn("S3 config not found for Tenant={}, using local storage for testing");
            s3Config = new S3Config();
    /*        s3Config.setBucket("test-bucket");
            s3Config.setRegion("us-east-1");
            s3Config.setAccessKey("test-key");
            s3Config.setSecretKey("test-secret");*/
        }

        // For testing, we'll skip actual S3 delete
        if (s3Config.getBucket().equals("test-bucket")) {
            log.info("Skipping S3 delete for testing - would delete from bucket={}, key={}", attachment.getS3Bucket(), attachment.getS3Key());
        } else {
            S3Client s3 = buildS3Client(s3Config);
            try {
                s3.deleteObject(DeleteObjectRequest.builder()
                        .bucket(attachment.getS3Bucket())
                        .key(attachment.getS3Key())
                        .build());
                log.info("Deleted message attachment from S3: bucket={}, key={}", attachment.getS3Bucket(), attachment.getS3Key());
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete file from S3", e);
            }
        }

        repository.delete(attachment);
    }

    @Transactional(readOnly = true)
    public DownloadResult download(Long attachmentId) {
        MessageAttachment attachment = repository.findByIdAndOrgId(attachmentId)
                .orElseThrow(() -> new RuntimeException("Message attachment not found"));

        S3Config s3Config;
        try {
            s3Config = configProvider.getS3DocumentStorage();
        } catch (Exception e) {
            // For testing/development, create a mock S3 config
            log.warn("S3 config not found for Tenant={}, using local storage for testing");
            s3Config = new S3Config();
/*            s3Config.setBucket("test-bucket");
            s3Config.setRegion("us-east-1");
            s3Config.setAccessKey("test-key");
            s3Config.setSecretKey("test-secret");*/
        }

        // For testing, we'll return a dummy file since we didn't actually upload to S3
        if (s3Config.getBucket().equals("test-bucket")) {
            log.info("Returning dummy file for testing - would download from bucket={}, key={}", attachment.getS3Bucket(), attachment.getS3Key());
            // Return a dummy file for testing
            String dummyContent = "This is a dummy file for testing purposes. The actual file would be downloaded from S3.";
            return new DownloadResult(
                    new java.io.ByteArrayInputStream(dummyContent.getBytes()),
                    attachment.getContentType() != null ? attachment.getContentType() : "text/plain",
                    attachment.getFileName()
            );
        } else {
            S3Client s3 = buildS3Client(s3Config);
            try (InputStream is = s3.getObject(GetObjectRequest.builder()
                    .bucket(attachment.getS3Bucket())
                    .key(attachment.getS3Key())
                    .build())) {

                byte[] fileBytes = is.readAllBytes();

                // 🔑 decrypt if metadata exists
                if (attachment.getEncryptionKey() != null && attachment.getIv() != null) {
                    try {
                        byte[] decodedKey = Base64.getDecoder().decode(attachment.getEncryptionKey());
                        SecretKey key = EncryptionUtil.fromBytes(decodedKey);
                        byte[] iv = Base64.getDecoder().decode(attachment.getIv());

                        fileBytes = EncryptionUtil.decrypt(fileBytes, key, iv);
                        log.info("Decrypted message attachment file before sending: {}", attachment.getFileName());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to decrypt file", e);
                    }
                }

                return new DownloadResult(
                        new java.io.ByteArrayInputStream(fileBytes),
                        attachment.getContentType(),
                        attachment.getFileName()
                );

            } catch (Exception e) {
                throw new RuntimeException("Failed to download file from S3", e);
            }
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<MessageAttachmentDto>> getAllForMessage(Long messageId) {
        List<MessageAttachment> attachments = repository.findAllByMessageId(messageId);
        List<MessageAttachmentDto> dtos = attachments.stream().map(this::mapToDto).collect(Collectors.toList());
        return ApiResponse.<List<MessageAttachmentDto>>builder()
                .success(true)
                .message("Message attachments retrieved successfully")
                .data(dtos)
                .build();
    }

    @Transactional(readOnly = true)
    public MessageAttachmentDto getById(Long attachmentId) {
        MessageAttachment attachment = repository.findByIdAndOrgId(attachmentId)
                .orElseThrow(() -> new RuntimeException("Message attachment not found"));
        return mapToDto(attachment);
    }

    private S3Client buildS3Client(S3Config cfg) {
        return S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.of(cfg.getRegion()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(cfg.getAccessKey(), cfg.getSecretKey())
                        )
                )
                .build();
    }

    private MessageAttachment mapToEntity(MessageAttachmentDto dto) {
        MessageAttachment entity = new MessageAttachment();
        entity.setMessageId(dto.getMessageId());
        entity.setCategory(dto.getCategory());
        entity.setType(dto.getType());
        entity.setFileName(dto.getFileName());
        entity.setContentType(dto.getContentType());
        entity.setDescription(dto.getDescription());
        entity.setS3Bucket(dto.getS3Bucket());
        entity.setS3Key(dto.getS3Key());
        return entity;
    }

    private MessageAttachmentDto mapToDto(MessageAttachment entity) {
        MessageAttachmentDto dto = new MessageAttachmentDto();
        dto.setId(entity.getId());
        dto.setMessageId(entity.getMessageId());
        dto.setOrgId(entity.getOrgId());
        dto.setCategory(entity.getCategory());
        dto.setType(entity.getType());
        dto.setFileName(entity.getFileName());
        dto.setContentType(entity.getContentType());
        dto.setDescription(entity.getDescription());
        dto.setS3Bucket(entity.getS3Bucket());
        dto.setS3Key(entity.getS3Key());
        // infer encryption flag
        dto.setEncrypted(entity.getEncryptionKey() != null && entity.getIv() != null);
        return dto;
    }

    public static class DownloadResult {
        private final InputStream inputStream;
        private final String contentType;
        private final String fileName;
        public DownloadResult(InputStream inputStream, String contentType, String fileName) {
            this.inputStream = inputStream;
            this.contentType = contentType;
            this.fileName = fileName;
        }
        public InputStream getInputStream() { return inputStream; }
        public String getContentType() { return contentType; }
        public String getFileName() { return fileName; }
    }
}