package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.DocumentDto;
import com.qiaben.ciyex.dto.DocumentSettingsDto;
import com.qiaben.ciyex.dto.integration.StorageConfig;
import com.qiaben.ciyex.fhir.FhirClientService;
import com.qiaben.ciyex.util.EncryptionUtil;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FHIR-only Document Service.
 * Uses FHIR DocumentReference for metadata, S3 for actual content.
 * No local database storage - metadata stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final OrgIntegrationConfigProvider configProvider;
    private final DocumentSettingsService documentSettingsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Extension URLs for custom fields
    private static final String EXT_S3_BUCKET = "http://ciyex.com/fhir/StructureDefinition/s3-bucket";
    private static final String EXT_S3_KEY = "http://ciyex.com/fhir/StructureDefinition/s3-key";
    private static final String EXT_ENCRYPTION_KEY = "http://ciyex.com/fhir/StructureDefinition/encryption-key";
    private static final String EXT_ENCRYPTION_IV = "http://ciyex.com/fhir/StructureDefinition/encryption-iv";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public DocumentDto create(String tenantName, Long patientId, DocumentDto dto, MultipartFile file) {
        // 1. Load document settings
        DocumentSettingsDto settingsDto = documentSettingsService.get();
        if (settingsDto == null) {
            throw new RuntimeException("Document settings not configured");
        }

        long maxUploadBytes = (long) settingsDto.getMaxUploadSizeMB() * 1024L * 1024L;

        // 2. File size validation
        long fileSize = file.getSize();
        if (fileSize > maxUploadBytes) {
            throw new RuntimeException("File exceeds max upload size (" + maxUploadBytes + " bytes)");
        }

        // 3. File type validation
        if (settingsDto.getAllowedFileTypes() != null && !settingsDto.getAllowedFileTypes().isEmpty()) {
            String ext = getFileExtension(file.getOriginalFilename());
            boolean ok = settingsDto.getAllowedFileTypes().stream()
                    .map(String::toLowerCase)
                    .anyMatch(a -> a.equals(ext));
            if (!ok) throw new RuntimeException("File type ." + ext + " not allowed");
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
        if (settingsDto.isEncryptionEnabled()) {
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

        String s3Key = tenantName + "/documents/" + patientId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Config.getBucket())
                            .key(s3Key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(fileBytes)
            );
            log.info("Uploaded to S3 bucket={} key={}", s3Config.getBucket(), s3Key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }

        // 7. Create FHIR DocumentReference
        dto.setPatientId(patientId);
        dto.setFileName(file.getOriginalFilename());
        dto.setContentType(file.getContentType());
        dto.setS3Bucket(s3Config.getBucket());
        dto.setS3Key(s3Key);

        DocumentReference docRef = toFhirDocumentReference(dto, base64Key, base64Iv);
        var outcome = fhirClientService.create(docRef, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId.replaceAll("[^0-9]", "").isEmpty() ? "0" : fhirId.hashCode() + ""));
        dto.setContent(null);
        dto.setEncrypted(base64Key != null);
        log.info("Created FHIR DocumentReference with id: {}", fhirId);

        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        // Get document reference to find S3 location
        DocumentReference docRef = fhirClientService.read(DocumentReference.class, fhirId, getPracticeId());
        DocumentDto dto = fromFhirDocumentReference(docRef);

        // Delete from S3
        if (dto.getS3Bucket() != null && dto.getS3Key() != null) {
            StorageConfig.S3 s3Config = configProvider.getS3DocumentStorage();
            if (s3Config != null) {
                S3Client s3 = buildS3Client(s3Config);
                try {
                    s3.deleteObject(DeleteObjectRequest.builder()
                            .bucket(dto.getS3Bucket())
                            .key(dto.getS3Key())
                            .build());
                    log.info("Deleted from S3: bucket={}, key={}", dto.getS3Bucket(), dto.getS3Key());
                } catch (Exception e) {
                    log.warn("Failed to delete file from S3: {}", e.getMessage());
                }
            }
        }

        // Delete FHIR DocumentReference
        fhirClientService.delete(DocumentReference.class, fhirId, getPracticeId());
    }

    // DOWNLOAD
    public DownloadResult download(String fhirId) {
        DocumentReference docRef = fhirClientService.read(DocumentReference.class, fhirId, getPracticeId());
        
        // Get S3 location from extensions
        String s3Bucket = getExtensionString(docRef, EXT_S3_BUCKET);
        String s3Key = getExtensionString(docRef, EXT_S3_KEY);
        String encryptionKey = getExtensionString(docRef, EXT_ENCRYPTION_KEY);
        String encryptionIv = getExtensionString(docRef, EXT_ENCRYPTION_IV);

        if (s3Bucket == null || s3Key == null) {
            throw new RuntimeException("Document S3 location not found");
        }

        StorageConfig.S3 s3Config = configProvider.getS3DocumentStorage();
        if (s3Config == null) {
            throw new RuntimeException("S3 document storage is not configured");
        }
        S3Client s3 = buildS3Client(s3Config);

        try (InputStream is = s3.getObject(GetObjectRequest.builder()
                .bucket(s3Bucket)
                .key(s3Key)
                .build())) {

            byte[] fileBytes = is.readAllBytes();

            // Decrypt if encrypted
            if (encryptionKey != null && encryptionIv != null) {
                try {
                    byte[] decodedKey = Base64.getDecoder().decode(encryptionKey);
                    SecretKey key = EncryptionUtil.fromBytes(decodedKey);
                    byte[] iv = Base64.getDecoder().decode(encryptionIv);
                    fileBytes = EncryptionUtil.decrypt(fileBytes, key, iv);
                    log.info("Decrypted file before sending");
                } catch (Exception e) {
                    throw new RuntimeException("Failed to decrypt file", e);
                }
            }

            // Get filename and content type from DocumentReference
            String fileName = "document";
            String contentType = "application/octet-stream";
            if (docRef.hasContent()) {
                Attachment att = docRef.getContentFirstRep().getAttachment();
                if (att.hasTitle()) fileName = att.getTitle();
                if (att.hasContentType()) contentType = att.getContentType();
            }

            return new DownloadResult(new ByteArrayInputStream(fileBytes), contentType, fileName);

        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    // GET ALL FOR PATIENT
    public ApiResponse<List<DocumentDto>> getAllForPatient(Long patientId) {
        return getAllForPatient(null, patientId);
    }

    public ApiResponse<List<DocumentDto>> getAllForPatient(String tenantName, Long patientId) {
        log.debug("Getting FHIR DocumentReferences for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(DocumentReference.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<DocumentReference> docs = fhirClientService.extractResources(bundle, DocumentReference.class);
        List<DocumentDto> dtos = docs.stream().map(this::fromFhirDocumentReference).collect(Collectors.toList());

        return ApiResponse.<List<DocumentDto>>builder()
                .success(true)
                .message("Documents retrieved successfully")
                .data(dtos)
                .build();
    }

    // GET ALL BY TENANT
    public ApiResponse<List<DocumentDto>> getAllByTenantName(String tenantName) {
        log.debug("Getting all FHIR DocumentReferences");

        Bundle bundle = fhirClientService.search(DocumentReference.class, getPracticeId());
        List<DocumentReference> docs = fhirClientService.extractResources(bundle, DocumentReference.class);
        List<DocumentDto> dtos = docs.stream().map(this::fromFhirDocumentReference).collect(Collectors.toList());

        return ApiResponse.<List<DocumentDto>>builder()
                .success(true)
                .message("Documents retrieved successfully")
                .data(dtos)
                .build();
    }

    // -------- FHIR Mapping --------

    private DocumentReference toFhirDocumentReference(DocumentDto dto, String encryptionKey, String encryptionIv) {
        DocumentReference dr = new DocumentReference();
        dr.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);

        // Subject (Patient)
        if (dto.getPatientId() != null) {
            dr.setSubject(new Reference("Patient/" + dto.getPatientId()));
        }

        // Category
        if (dto.getCategory() != null) {
            dr.addCategory().setText(dto.getCategory());
        }

        // Type
        if (dto.getType() != null) {
            dr.getType().setText(dto.getType());
        }

        // Description
        if (dto.getDescription() != null) {
            dr.setDescription(dto.getDescription());
        }

        // Content (attachment metadata)
        DocumentReference.DocumentReferenceContentComponent content = dr.addContent();
        Attachment attachment = new Attachment();
        if (dto.getFileName() != null) attachment.setTitle(dto.getFileName());
        if (dto.getContentType() != null) attachment.setContentType(dto.getContentType());
        // URL points to our download endpoint (not direct S3)
        content.setAttachment(attachment);

        // S3 location extensions
        if (dto.getS3Bucket() != null) {
            dr.addExtension(new Extension(EXT_S3_BUCKET, new StringType(dto.getS3Bucket())));
        }
        if (dto.getS3Key() != null) {
            dr.addExtension(new Extension(EXT_S3_KEY, new StringType(dto.getS3Key())));
        }

        // Encryption extensions
        if (encryptionKey != null) {
            dr.addExtension(new Extension(EXT_ENCRYPTION_KEY, new StringType(encryptionKey)));
        }
        if (encryptionIv != null) {
            dr.addExtension(new Extension(EXT_ENCRYPTION_IV, new StringType(encryptionIv)));
        }

        return dr;
    }

    private DocumentDto fromFhirDocumentReference(DocumentReference dr) {
        DocumentDto dto = new DocumentDto();
        
        // Use FHIR ID hash as numeric ID for compatibility
        String fhirId = dr.getIdElement().getIdPart();
        dto.setId((long) Math.abs(fhirId.hashCode()));

        // Subject -> patientId
        if (dr.hasSubject() && dr.getSubject().hasReference()) {
            String ref = dr.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring("Patient/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Category
        if (dr.hasCategory()) {
            dto.setCategory(dr.getCategoryFirstRep().getText());
        }

        // Type
        if (dr.hasType()) {
            dto.setType(dr.getType().getText());
        }

        // Description
        if (dr.hasDescription()) {
            dto.setDescription(dr.getDescription());
        }

        // Content
        if (dr.hasContent()) {
            Attachment att = dr.getContentFirstRep().getAttachment();
            if (att.hasTitle()) dto.setFileName(att.getTitle());
            if (att.hasContentType()) dto.setContentType(att.getContentType());
        }

        // S3 extensions
        dto.setS3Bucket(getExtensionString(dr, EXT_S3_BUCKET));
        dto.setS3Key(getExtensionString(dr, EXT_S3_KEY));

        // Encryption flag
        dto.setEncrypted(getExtensionString(dr, EXT_ENCRYPTION_KEY) != null);

        return dto;
    }

    // -------- Helpers --------

    private String getExtensionString(DocumentReference dr, String url) {
        Extension ext = dr.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
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

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0) ? fileName.substring(dot + 1).toLowerCase() : "";
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
