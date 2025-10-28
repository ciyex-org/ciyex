package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.MessageAttachmentDto;
import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalMessageAttachmentDto;
import com.qiaben.ciyex.service.MessageAttachmentService;
import com.qiaben.ciyex.service.portal.PortalMessageAttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portal/messages/{messageId}/attachments")
@Slf4j
@RequiredArgsConstructor
public class PortalMessageAttachmentController {

    private final PortalMessageAttachmentService portalMessageAttachmentService;

    /**
     * Get all attachments for a specific message
     */
    @GetMapping
    public ResponseEntity<?> getMessageAttachments(
            @PathVariable("messageId") Long messageId,
            HttpServletRequest request) {

        try {
            // Extract portal user ID from token (you'll need to implement this utility)
            Long portalUserId = extractPortalUserIdFromToken(request);

            ApiResponse<List<PortalMessageAttachmentDto>> response =
                portalMessageAttachmentService.getMessageAttachments(portalUserId, messageId);

            if (!response.isSuccess()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", response.getMessage()
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", response.getMessage(),
                "data", response.getData()
            ));

        } catch (Exception e) {
            log.error("Error getting message attachments: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Unable to load message attachments",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Upload attachment to a message
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAttachment(
            @PathVariable("messageId") Long messageId,
            @RequestPart("dto") String dtoJson,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {

        try {
            // Extract portal user ID from token
            Long portalUserId = extractPortalUserIdFromToken(request);


            // Set org ID in request context
            com.qiaben.ciyex.dto.integration.RequestContext ctx = new com.qiaben.ciyex.dto.integration.RequestContext();
            com.qiaben.ciyex.dto.integration.RequestContext.set(ctx);

            // Parse the DTO
            PortalMessageAttachmentDto portalDto = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(dtoJson, PortalMessageAttachmentDto.class);

            // Map to MessageAttachmentDto
            MessageAttachmentDto dto = new MessageAttachmentDto();
            dto.setMessageId(messageId);
            dto.setCategory(portalDto.getCategory());
            dto.setType(portalDto.getType());
            dto.setFileName(portalDto.getFileName());
            dto.setContentType(portalDto.getContentType());
            dto.setDescription(portalDto.getDescription());
            dto.setEncrypted(portalDto.isEncrypted());

            ApiResponse<PortalMessageAttachmentDto> response =
                portalMessageAttachmentService.uploadMessageAttachment(portalUserId, messageId, dto, file);

            if (!response.isSuccess()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", response.getMessage()
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", response.getMessage(),
                "data", response.getData()
            ));

        } catch (Exception e) {
            log.error("Error uploading message attachment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Unable to upload attachment",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Download attachment
     */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable("messageId") Long messageId,
            @PathVariable("attachmentId") Long attachmentId,
            HttpServletRequest request) {

        try {
            // Extract portal user ID from token
            Long portalUserId = extractPortalUserIdFromToken(request);

            MessageAttachmentService.DownloadResult result =
                portalMessageAttachmentService.getAttachmentDownload(portalUserId, messageId, attachmentId);

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

    /**
     * Delete attachment
     */
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<?> deleteAttachment(
            @PathVariable("messageId") Long messageId,
            @PathVariable("attachmentId") Long attachmentId,
            HttpServletRequest request) {

        try {
            // Extract portal user ID from token
            Long portalUserId = extractPortalUserIdFromToken(request);

            ApiResponse<Void> response =
                portalMessageAttachmentService.deleteMessageAttachment(portalUserId, messageId, attachmentId);

            if (!response.isSuccess()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", response.getMessage()
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", response.getMessage()
            ));

        } catch (Exception e) {
            log.error("Error deleting message attachment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Unable to delete attachment",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Extract portal user ID from JWT token
     */
    private Long extractPortalUserIdFromToken(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) {
            throw new RuntimeException("Authorization token missing");
        }
        // TODO: Implement JWT token parsing to extract user ID
        throw new UnsupportedOperationException("JWT token parsing not implemented");
    }

    /**
     * Extract Bearer token from Authorization header
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.valueOf(value.toString());
    }
}