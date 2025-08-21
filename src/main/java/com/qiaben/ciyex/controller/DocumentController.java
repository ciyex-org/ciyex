package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.DocumentDto;
import com.qiaben.ciyex.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{patientId}/documents")
@Slf4j
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentDto>> upload(
            @PathVariable Long patientId,
            @RequestPart("dto") DocumentDto dto,
            @RequestPart(value = "file", required = true) MultipartFile file) {
        try {
            // Infer MIME type if application/octet-stream
            String contentType = file.getContentType();
            if ("application/octet-stream".equals(contentType) || contentType == null) {
                contentType = inferContentType(file.getOriginalFilename());
                log.debug("Inferred contentType: {} for file: {}", contentType, file.getOriginalFilename());
            }
            dto.setContentType(contentType);
            DocumentDto created = service.create(patientId, dto, file);
            return ResponseEntity.ok(ApiResponse.<DocumentDto>builder()
                    .success(true)
                    .message("Document uploaded successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Failed to upload document: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<DocumentDto>builder()
                    .success(false)
                    .message("Failed to upload document: " + e.getMessage())
                    .build());
        }
    }

    private String inferContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        String lowerCaseFileName = fileName.toLowerCase();
        if (lowerCaseFileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerCaseFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerCaseFileName.endsWith(".txt")) {
            return "text/plain";
        }
        return "application/octet-stream"; // Fallback
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long documentId) {
        try {
            service.delete(documentId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Document deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete document: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete document: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentDto>>> list(@PathVariable Long patientId) {
        try {
            return ResponseEntity.ok(service.getAllForPatient(patientId));
        } catch (Exception e) {
            log.error("Failed to list documents: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<List<DocumentDto>>builder()
                    .success(false)
                    .message("Failed to list documents: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long patientId,
            @PathVariable Long documentId) {
        try {
            DocumentService.DownloadResult result = service.download(documentId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(result.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getFileName() + "\"")
                    .body(new InputStreamResource(result.getInputStream()));
        } catch (Exception e) {
            log.error("Failed to download document: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}