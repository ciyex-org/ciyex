package org.ciyex.ehr.scanning.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.scanning.entity.ScannedDocument;
import org.ciyex.ehr.scanning.repository.ScannedDocumentRepository;
import org.ciyex.ehr.service.storage.FileStorageStrategy;
import org.ciyex.ehr.service.storage.FileStorageStrategyResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentScanningService {

    private final ScannedDocumentRepository repo;
    private final FileStorageStrategyResolver storageResolver;

    private String orgAlias() {
        RequestContext rc = RequestContext.get();
        return rc != null ? rc.getOrgName() : "default";
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }

    public Page<ScannedDocument> list(String q, String category, String ocrStatus, int page, int size) {
        String org = orgAlias();
        String qParam = (q == null || q.isBlank()) ? null : q;
        String catParam = (category == null || category.isBlank()) ? null : category;
        String statusParam = (ocrStatus == null || ocrStatus.isBlank()) ? null : ocrStatus;
        Page<ScannedDocument> results = repo.search(org, qParam, catParam, statusParam, PageRequest.of(page, size));
        // Enrich with presigned URLs
        results.forEach(doc -> {
            if (doc.getStorageKey() != null && doc.getFileUrl() == null) {
                try {
                    FileStorageStrategy strategy = storageResolver.resolve(org);
                    String url = strategy.getPresignedUrlByKey(doc.getStorageKey(), 3600);
                    doc.setFileUrl(url);
                } catch (Exception e) {
                    log.warn("Could not generate presigned URL for doc {}: {}", doc.getId(), e.getMessage());
                }
            }
        });
        return results;
    }

    public ScannedDocument upload(MultipartFile file, String category, Long patientId, String patientName) {
        String org = orgAlias();
        String storageKey = org + "/scanning/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        // Upload to storage
        try {
            FileStorageStrategy strategy = storageResolver.resolve(org);
            byte[] fileBytes = file.getBytes();
            strategy.uploadByKey(fileBytes, storageKey, file.getContentType(), org,
                    "document-scanning", patientId != null ? patientId.toString() : null,
                    file.getOriginalFilename());
        } catch (Exception e) {
            log.error("Failed to upload scanned document", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }

        ScannedDocument doc = ScannedDocument.builder()
                .fileName(file.getOriginalFilename())
                .originalFileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .storageKey(storageKey)
                .patientId(patientId)
                .patientName(patientName)
                .category(category != null ? category : "other")
                .ocrStatus("pending")
                .uploadedBy(currentUser())
                .orgAlias(org)
                .build();

        return repo.save(doc);
    }

    public ScannedDocument triggerOcr(Long id) {
        String org = orgAlias();
        ScannedDocument doc = repo.findById(id)
                .filter(d -> org.equals(d.getOrgAlias()))
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        // Mark as processing; actual OCR would be done asynchronously
        doc.setOcrStatus("processing");
        return repo.save(doc);
    }

    public void delete(Long id) {
        String org = orgAlias();
        ScannedDocument doc = repo.findById(id)
                .filter(d -> org.equals(d.getOrgAlias()))
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));

        if (doc.getStorageKey() != null) {
            try {
                FileStorageStrategy strategy = storageResolver.resolve(org);
                strategy.deleteByKey(doc.getStorageKey());
            } catch (Exception e) {
                log.warn("Could not delete file from storage for doc {}: {}", id, e.getMessage());
            }
        }
        repo.delete(doc);
    }
}
