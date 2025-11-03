package com.qiaben.ciyex.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.MessageAttachmentDto;
import com.qiaben.ciyex.service.MessageAttachmentService;
import com.qiaben.ciyex.service.MessageAttachmentService.DownloadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/messages/{messageId}/attachments")
@Slf4j
public class MessageAttachmentController {

    private final MessageAttachmentService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessageAttachmentController(MessageAttachmentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MessageAttachmentDto>> upload(
            
            @PathVariable Long messageId,
            @RequestPart("dto") String dtoJson,
            @RequestPart("file") MultipartFile file) {
        try {
            MessageAttachmentDto dto = objectMapper.readValue(dtoJson, MessageAttachmentDto.class);
            MessageAttachmentDto created = service.create(messageId, dto, file);

            return ResponseEntity.ok(ApiResponse.<MessageAttachmentDto>builder()
                    .success(true)
                    .message("Message attachment uploaded successfully")
                    .data(created)
                    .build());
        } catch (IllegalArgumentException e) {
            // invalid input, file too large, wrong type
            return ResponseEntity.badRequest().body(ApiResponse.<MessageAttachmentDto>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Upload failed", e);
            return ResponseEntity.internalServerError().body(ApiResponse.<MessageAttachmentDto>builder()
                    .success(false)
                    .message("Server error: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MessageAttachmentDto>>> list(
            
            @PathVariable Long messageId) {
        return ResponseEntity.ok(service.getAllForMessage(messageId));
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<MessageAttachmentDto> getById(
            
            @PathVariable Long messageId,
            @PathVariable Long attachmentId) {
        try {
            MessageAttachmentDto attachment = service.getById(attachmentId);
            return ResponseEntity.ok(attachment);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> download(
            
            @PathVariable Long messageId,
            @PathVariable Long attachmentId) {
        try {
            DownloadResult result = service.download(attachmentId);
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

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            
            @PathVariable Long messageId,
            @PathVariable Long attachmentId) {
        try {
            service.delete(attachmentId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Message attachment deleted successfully")
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