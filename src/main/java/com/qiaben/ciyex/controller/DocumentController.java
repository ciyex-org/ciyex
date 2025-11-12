package com.qiaben.ciyex.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.DocumentDto;
import com.qiaben.ciyex.service.DocumentService;
import com.qiaben.ciyex.service.DocumentService.DownloadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents/upload")
@Slf4j
public class DocumentController {

    private final DocumentService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentDto>> upload(
            @RequestPart(value = "patientId", required = false) Long patientId,
            @RequestPart(value = "dto", required = false) String dtoJson,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            // Validate required fields
            java.util.List<String> missing = new java.util.ArrayList<>();
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
            DocumentDto created = service.create(patientId, dto, file);

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
            @RequestParam Long patientId) {
        return ResponseEntity.ok(service.getAllForPatient(patientId));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long documentId) {
        try {
            DownloadResult result = service.download(documentId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(result.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + result.getFileName() + "\"")
                    .body(new InputStreamResource(result.getInputStream()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Download failed", e);
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            log.error("Unexpected error", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long documentId) {
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