package org.ciyex.ehr.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.scanning.entity.ScannedDocument;
import org.ciyex.ehr.scanning.service.DocumentScanningService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/document-scanning")
@RequiredArgsConstructor
@Slf4j
public class DocumentScanningController {

    private final DocumentScanningService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ScannedDocument>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String ocrStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<ScannedDocument> result = service.list(q, category, ocrStatus, page, size);
            return ResponseEntity.ok(ApiResponse.<Page<ScannedDocument>>builder()
                    .success(true)
                    .data(result)
                    .build());
        } catch (Exception e) {
            log.error("Error listing scanned documents", e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Page<ScannedDocument>>builder()
                    .success(false)
                    .message("Error listing documents: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ScannedDocument>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String patientName) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.<ScannedDocument>builder()
                        .success(false)
                        .message("File is required")
                        .build());
            }
            ScannedDocument doc = service.upload(file, category, patientId, patientName);
            return ResponseEntity.ok(ApiResponse.<ScannedDocument>builder()
                    .success(true)
                    .message("Document uploaded successfully")
                    .data(doc)
                    .build());
        } catch (Exception e) {
            log.error("Error uploading scanned document", e);
            return ResponseEntity.internalServerError().body(ApiResponse.<ScannedDocument>builder()
                    .success(false)
                    .message("Upload failed: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/{id}/ocr")
    public ResponseEntity<ApiResponse<ScannedDocument>> triggerOcr(@PathVariable Long id) {
        try {
            ScannedDocument doc = service.triggerOcr(id);
            return ResponseEntity.ok(ApiResponse.<ScannedDocument>builder()
                    .success(true)
                    .message("OCR processing initiated")
                    .data(doc)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.<ScannedDocument>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error triggering OCR for document {}", id, e);
            return ResponseEntity.internalServerError().body(ApiResponse.<ScannedDocument>builder()
                    .success(false)
                    .message("OCR trigger failed: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
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
            log.error("Error deleting scanned document {}", id, e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Delete failed: " + e.getMessage())
                    .build());
        }
    }
}
