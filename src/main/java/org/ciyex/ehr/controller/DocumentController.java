package org.ciyex.ehr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.DocumentDto;
import org.ciyex.ehr.service.DocumentService;
import org.ciyex.ehr.service.DocumentService.DownloadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@PreAuthorize("hasAuthority('SCOPE_user/DocumentReference.read')")
@RestController
@RequestMapping("/api/documents/upload")
@Slf4j
public class DocumentController {

    private final DocumentService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    private String resolveTenant(String headerValue) {
        if (headerValue != null && !headerValue.isBlank()) return headerValue;
        return org.ciyex.ehr.util.TenantContextUtil.getTenantName();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SCOPE_user/DocumentReference.write')")
    public ResponseEntity<ApiResponse<DocumentDto>> upload(
            @RequestHeader(value = "X-Tenant-Name", required = false) String tenantName,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "dto", required = false) String dtoJson,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            tenantName = resolveTenant(tenantName);
            // Validate required fields
            java.util.List<String> missing = new java.util.ArrayList<>();
            if (tenantName == null || tenantName.isBlank()) missing.add("X-Tenant-Name header");
            if (patientId == null) missing.add("patientId");
            if (dtoJson == null || dtoJson.isBlank()) missing.add("dto");
            if (file == null || file.isEmpty()) missing.add("file");

            if (!missing.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.<DocumentDto>builder()
                        .success(false)
                        .message("Missing required fields: " + String.join(", ", missing))
                        .build());
            }

            DocumentDto dto = objectMapper.readValue(dtoJson, DocumentDto.class);
            DocumentDto created = service.create(tenantName, patientId, dto, file);

            return ResponseEntity.ok(ApiResponse.<DocumentDto>builder()
                    .success(true)
                    .message("Document uploaded successfully")
                    .data(created)
                    .build());
        } catch (IllegalArgumentException e) {
            // invalid input, file too large, wrong type
            return ResponseEntity.badRequest().body(ApiResponse.<DocumentDto>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Upload failed", e);
            return ResponseEntity.internalServerError().body(ApiResponse.<DocumentDto>builder()
                    .success(false)
                    .message("Server error: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentDto>>> list(
            @RequestHeader(value = "X-Tenant-Name", required = false) String tenantName) {
        return ResponseEntity.ok(service.getAllByTenantName(resolveTenant(tenantName)));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<DocumentDto>>> getByPatientId(
            @RequestHeader(value = "X-Tenant-Name", required = false) String tenantName,
            @PathVariable Long patientId) {
        return ResponseEntity.ok(service.getAllForPatient(resolveTenant(tenantName), patientId));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable String documentId) {
        try {
            log.info("Download request for documentId: {}", documentId);
            DownloadResult result = service.download(documentId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(result.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + result.getFileName() + "\"")
                    .body(new InputStreamResource(result.getInputStream()));
        } catch (IllegalArgumentException e) {
            log.error("Bad request for documentId {}: {}", documentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Download failed for documentId {}: {}", documentId, e.getMessage());
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            log.error("Unexpected error for documentId {}: {}", documentId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasAuthority('SCOPE_user/DocumentReference.write')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String documentId) {
        try {
            service.delete(documentId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Document deleted successfully")
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Delete failed", e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Server error: " + e.getMessage())
                    .build());
        }
    }
}