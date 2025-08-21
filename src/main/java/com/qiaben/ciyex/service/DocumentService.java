package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.DocumentDto;
import com.qiaben.ciyex.entity.Document;
import com.qiaben.ciyex.provider.S3ClientProvider;
import com.qiaben.ciyex.repository.DocumentRepository;
import com.qiaben.ciyex.repository.PatientRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DocumentService {

    private final DocumentRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;
    private final PatientRepository patientRepository;
    private final S3ClientProvider s3ClientProvider;

    @Autowired
    public DocumentService(DocumentRepository repository, ExternalStorageResolver storageResolver,
                           OrgIntegrationConfigProvider configProvider, PatientRepository patientRepository,
                           S3ClientProvider s3ClientProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
        this.patientRepository = patientRepository;
        this.s3ClientProvider = s3ClientProvider;
    }

    @Transactional
    public DocumentDto create(Long patientId, DocumentDto dto, MultipartFile file) {
        Long orgId = getCurrentOrgId();
        if (orgId == null) {
            throw new SecurityException("No orgId available in request context");
        }
        dto.setPatientId(patientId);
        dto.setFileName(file.getOriginalFilename());
        dto.setContentType(file.getContentType());
        try {
            dto.setContent(file.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file content", e);
        }

        String externalId = null;
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<DocumentDto> externalStorage = storageResolver.resolve(DocumentDto.class);
                externalId = externalStorage.create(dto);
                log.info("Created in external storage: externalId={}, orgId={}", externalId, orgId);
            } catch (Exception e) {
                log.error("Failed to create in external storage: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        Document entity = mapToEntity(dto);
        entity.setOrgId(orgId);
        entity.setFhirExternalId(externalId);
        entity.setCreatedDate(LocalDateTime.now().toString());
        entity.setLastModifiedDate(LocalDateTime.now().toString());
        repository.save(entity);
        dto.setId(entity.getId());
        dto.setContent(null); // Clear sensitive content

        return dto;
    }

    @Transactional
    public void delete(Long documentId) {
        Long orgId = getCurrentOrgId();
        if (orgId == null) {
            throw new SecurityException("No orgId available in request context");
        }

        Document document = repository.findByIdAndOrgId(documentId, orgId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<DocumentDto> externalStorage = storageResolver.resolve(DocumentDto.class);
                externalStorage.delete(document.getFhirExternalId());
                log.info("Deleted from external storage: externalId={}, orgId={}", document.getFhirExternalId(), orgId);
            } catch (Exception e) {
                log.error("Failed to delete from external storage: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        repository.delete(document);
        log.info("Deleted from DB: id={}, orgId={}", documentId, orgId);
    }

    @Transactional(readOnly = true)
    public DownloadResult download(Long documentId) {
        Long orgId = getCurrentOrgId();
        if (orgId == null) {
            throw new SecurityException("No orgId available in request context");
        }

        Document document = repository.findByIdAndOrgId(documentId, orgId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        DocumentDto dto;
        if (storageType != null) {
            ExternalStorage<DocumentDto> externalStorage = storageResolver.resolve(DocumentDto.class);
            dto = externalStorage.get(document.getFhirExternalId());
        } else {
            throw new RuntimeException("No external storage configured");
        }

        S3Client s3Client = s3ClientProvider.getForCurrentOrg();
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(dto.getS3Bucket())
                .key(dto.getS3Key())
                .build();
        InputStream compressedIs = s3Client.getObject(getRequest);

        try {
            return new DownloadResult(
                    new GZIPInputStream(compressedIs),
                    document.getContentType(),
                    document.getFileName()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to uncompress file", e);
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<DocumentDto>> getAllForPatient(Long patientId) {
        Long orgId = getCurrentOrgId();
        if (orgId == null) {
            return ApiResponse.<List<DocumentDto>>builder()
                    .success(false)
                    .message("No orgId available in request context")
                    .build();
        }

        List<Document> documents = repository.findAllByOrgIdAndPatientId(orgId, patientId);
        List<DocumentDto> dtos = documents.stream().map(this::mapToDto).collect(Collectors.toList());
        return ApiResponse.<List<DocumentDto>>builder()
                .success(true)
                .message("Documents retrieved successfully")
                .data(dtos)
                .build();
    }

    private Document mapToEntity(DocumentDto dto) {
        Document entity = new Document();
        entity.setPatientId(dto.getPatientId());
        entity.setCategory(dto.getCategory());
        entity.setType(dto.getType());
        entity.setFileName(dto.getFileName());
        entity.setContentType(dto.getContentType());
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
        return dto;
    }

    private Long getCurrentOrgId() {
        return RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
    }

    public static class DownloadResult {
        private InputStream inputStream;
        private String contentType;
        private String fileName;

        public DownloadResult(InputStream inputStream, String contentType, String fileName) {
            this.inputStream = inputStream;
            this.contentType = contentType;
            this.fileName = fileName;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public String getContentType() {
            return contentType;
        }

        public String getFileName() {
            return fileName;
        }
    }
}