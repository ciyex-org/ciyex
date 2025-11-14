package com.qiaben.ciyex.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.DocumentDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.dto.integration.StorageConfig;
import com.qiaben.ciyex.entity.Document;
import com.qiaben.ciyex.entity.DocumentSettings;
import com.qiaben.ciyex.repository.DocumentRepository;
import com.qiaben.ciyex.repository.DocumentSettingsRepo;
import com.qiaben.ciyex.util.EncryptionUtil;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DocumentService {

    private final DocumentRepository repository;
    private final DocumentSettingsRepo settingsRepo;
    private final OrgIntegrationConfigProvider configProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentService(DocumentRepository repository,
                           DocumentSettingsRepo settingsRepo,
                           OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.settingsRepo = settingsRepo;
        this.configProvider = configProvider;
    }

    @Transactional
    public DocumentDto create(String tenantName, Long patientId, DocumentDto dto, MultipartFile file) {
        // 1. Load document settings for org
        DocumentSettings settings = settingsRepo.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("Document settings not configured"));

        // 2. File size validation
        long fileSize = file.getSize();
        if (fileSize > settings.getMaxUploadBytes()) {
            throw new RuntimeException("File exceeds max upload size (" + settings.getMaxUploadBytes() + " bytes)");
        }

        // 3. File type validation (optional)
        if (settings.getAllowedFileTypesJson() != null && !settings.getAllowedFileTypesJson().isBlank()) {
            try {
                List<String> allowed = objectMapper.readValue(settings.getAllowedFileTypesJson(),
                        new TypeReference<List<String>>() {});
                String ext = getFileExtension(file.getOriginalFilename());
                boolean ok = allowed.stream().map(String::toLowerCase).anyMatch(a -> a.equals(ext));
                if (!ok) throw new RuntimeException("File type ." + ext + " not allowed");
            } catch (Exception e) {
                throw new RuntimeException("Invalid allowed_file_types_json format", e);
            }
        }

        // 4. Prepare file bytes
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file content", e);
        }

        // 5. Encrypt if enabled
        String base64Key = null;
        String base64Iv = null;
        if (settings.isEncryptionEnabled()) {
            try {
                SecretKey key = EncryptionUtil.generateKey();
                byte[] iv = new byte[12];
                new SecureRandom().nextBytes(iv);
                fileBytes = EncryptionUtil.encrypt(fileBytes, key, iv);
                base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
                base64Iv = Base64.getEncoder().encodeToString(iv);
                log.info("Applied encryption for file={}", file.getOriginalFilename());
            } catch (Exception e) {
                throw new RuntimeException("Encryption failed", e);
            }
        }

        // 6. Upload to S3
        StorageConfig.S3 s3Config = configProvider.getS3DocumentStorage();
        if (s3Config == null) {
            throw new RuntimeException("S3 document storage is not configured");
        }
        S3Client s3 = buildS3Client(s3Config);

        String key = tenantName + "/documents/" + patientId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Config.getBucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(fileBytes)
            );
            log.info("Uploaded to S3 bucket={} key={}", s3Config.getBucket(), key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }

        // 7. Save metadata in DB
        dto.setPatientId(patientId);
        dto.setFileName(file.getOriginalFilename());
        dto.setContentType(file.getContentType());
        dto.setS3Bucket(s3Config.getBucket());
        dto.setS3Key(key);

        Document entity = mapToEntity(dto);
        entity.setTenantName(tenantName); // Save tenant name from header
        entity.setEncryptionKey(base64Key);
        entity.setInitializationVector(base64Iv);

        repository.save(entity);
        dto.setId(entity.getId());
        dto.setContent(null);
        dto.setEncrypted(entity.getEncryptionKey() != null && entity.getInitializationVector() != null);
        return dto;
    }

    @Transactional
    public void delete(Long documentId) {
        Document document = repository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        StorageConfig.S3 s3Config = configProvider.getS3DocumentStorage();
        if (s3Config == null) {
            throw new RuntimeException("S3 document storage is not configured");
        }
        S3Client s3 = buildS3Client(s3Config);

        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(document.getS3Bucket())
                    .key(document.getS3Key())
                    .build());
            log.info("Deleted from S3: bucket={}, key={}", document.getS3Bucket(), document.getS3Key());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from S3", e);
        }

        repository.delete(document);
    }

    @Transactional(readOnly = true)
    public DownloadResult download(Long documentId) {
        Document document = repository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        StorageConfig.S3 s3Config = configProvider.getS3DocumentStorage();
        if (s3Config == null) {
            throw new RuntimeException("S3 document storage is not configured");
        }
        S3Client s3 = buildS3Client(s3Config);

        try (InputStream is = s3.getObject(GetObjectRequest.builder()
                .bucket(document.getS3Bucket())
                .key(document.getS3Key())
                .build())) {

            byte[] fileBytes = is.readAllBytes();

            // 🔑 decrypt if metadata exists
            if (document.getEncryptionKey() != null && document.getInitializationVector() != null) {
                try {
                    byte[] decodedKey = Base64.getDecoder().decode(document.getEncryptionKey());
                    SecretKey key = EncryptionUtil.fromBytes(decodedKey);
                    byte[] iv = Base64.getDecoder().decode(document.getInitializationVector());

                    fileBytes = EncryptionUtil.decrypt(fileBytes, key, iv);
                    log.info("Decrypted file before sending: {}", document.getFileName());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to decrypt file", e);
                }
            }

            return new DownloadResult(
                    new java.io.ByteArrayInputStream(fileBytes),
                    document.getContentType(),
                    document.getFileName()
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<DocumentDto>> getAllForPatient(Long patientId) {
        return getAllForPatient(null, patientId);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<DocumentDto>> getAllForPatient(String tenantName, Long patientId) {
        List<Document> documents = repository.findAllByPatientId(patientId);

        // Filter by tenantName if provided
        if (tenantName != null && !tenantName.isBlank()) {
            documents = documents.stream()
                    .filter(doc -> tenantName.equals(doc.getTenantName()))
                    .collect(Collectors.toList());
        }

        List<DocumentDto> dtos = documents.stream().map(this::mapToDto).collect(Collectors.toList());
        return ApiResponse.<List<DocumentDto>>builder()
                .success(true)
                .message("Documents retrieved successfully")
                .data(dtos)
                .build();
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<DocumentDto>> getAllByTenantName(String tenantName) {
        List<Document> documents;

        if (tenantName != null && !tenantName.isBlank()) {
            // Get all documents and filter by tenantName
            documents = repository.findAll().stream()
                    .filter(doc -> tenantName.equals(doc.getTenantName()))
                    .collect(Collectors.toList());
        } else {
            // If no tenant name provided, return all documents
            documents = repository.findAll();
        }

        List<DocumentDto> dtos = documents.stream().map(this::mapToDto).collect(Collectors.toList());
        return ApiResponse.<List<DocumentDto>>builder()
                .success(true)
                .message("Documents retrieved successfully")
                .data(dtos)
                .build();
    }

    private S3Client buildS3Client(StorageConfig.S3 cfg) {
        return S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.of(cfg.getRegion()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(cfg.getAccessKey(), cfg.getSecretKey())
                        )
                )
                .build();
    }

    private Document mapToEntity(DocumentDto dto) {
        Document entity = new Document();
        entity.setPatientId(dto.getPatientId());
        entity.setCategory(dto.getCategory());
        entity.setType(dto.getType());
        entity.setFileName(dto.getFileName());
        entity.setContentType(dto.getContentType());
        entity.setDescription(dto.getDescription());
        entity.setS3Bucket(dto.getS3Bucket());
        entity.setS3Key(dto.getS3Key());
        return entity;
    }

    private DocumentDto mapToDto(Document entity) {
        DocumentDto dto = new DocumentDto();
        dto.setId(entity.getId());
        dto.setPatientId(entity.getPatientId());
        dto.setCategory(entity.getCategory());
        dto.setType(entity.getType());
        dto.setFileName(entity.getFileName());
        dto.setContentType(entity.getContentType());
        dto.setDescription(entity.getDescription());
        dto.setS3Bucket(entity.getS3Bucket());
        dto.setS3Key(entity.getS3Key());
        // infer encryption flag
        dto.setEncrypted(entity.getEncryptionKey() != null && entity.getInitializationVector() != null);
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

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0) ? fileName.substring(dot + 1).toLowerCase() : "";
    }
}