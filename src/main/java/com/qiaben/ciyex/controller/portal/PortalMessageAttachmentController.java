package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.MessageAttachmentDto;
import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalMessageAttachmentDto;
import com.qiaben.ciyex.repository.ProviderRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.service.MessageAttachmentService;
import com.qiaben.ciyex.service.portal.PortalMessageAttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/portal/messages/{messageId}/attachments", "/api/fhir/portal/messages/{messageId}/attachments"})
@Slf4j
@RequiredArgsConstructor
public class PortalMessageAttachmentController {

    private final PortalMessageAttachmentService portalMessageAttachmentService;
    private final PortalUserRepository portalUserRepository;
    private final ProviderRepository providerRepository;

    /**
     * Get all attachments for a specific message
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT') or hasAuthority('PROVIDER') or hasRole('PROVIDER')")
    public ResponseEntity<?> getMessageAttachments(
            @PathVariable("messageId") Long messageId,
            Authentication authentication) {

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Not authenticated"
                ));
            }

            String userEmail = authentication.getName();
            boolean isPatient = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("PATIENT") || auth.getAuthority().equals("ROLE_PATIENT"));
            boolean isProvider = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("PROVIDER") || auth.getAuthority().equals("ROLE_PROVIDER"));

            Long userId;
            if (isPatient) {
                // For patients, get portal user ID
                userId = portalUserRepository.findByEmail(userEmail)
                        .map(portalUser -> portalUser.getId())
                        .orElse(1L); // Fallback
            } else if (isProvider) {
                // For providers, get provider ID
                userId = providerRepository.findAll().stream()
                        .filter(provider -> userEmail.equals(provider.getEmail()))
                        .map(provider -> provider.getId())
                        .findFirst()
                        .orElse(1L); // Fallback
            } else {
                userId = 1L; // Fallback
            }

            ApiResponse<List<PortalMessageAttachmentDto>> response =
                portalMessageAttachmentService.getMessageAttachments(userId, messageId);

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
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT') or hasAuthority('PROVIDER') or hasRole('PROVIDER')")
    public ResponseEntity<?> uploadAttachment(
            @PathVariable("messageId") Long messageId,
            @RequestPart("dto") String dtoJson,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Not authenticated"
                ));
            }

            String userEmail = authentication.getName();
            boolean isPatient = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("PATIENT") || auth.getAuthority().equals("ROLE_PATIENT"));
            boolean isProvider = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("PROVIDER") || auth.getAuthority().equals("ROLE_PROVIDER"));

            Long userId;
            if (isPatient) {
                // For patients, get portal user ID
                userId = portalUserRepository.findByEmail(userEmail)
                        .map(portalUser -> portalUser.getId())
                        .orElse(1L); // Fallback
            } else if (isProvider) {
                // For providers, get provider ID
                userId = providerRepository.findAll().stream()
                        .filter(provider -> userEmail.equals(provider.getEmail()))
                        .map(provider -> provider.getId())
                        .findFirst()
                        .orElse(1L); // Fallback
            } else {
                userId = 1L; // Fallback
            }

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
                portalMessageAttachmentService.uploadMessageAttachment(userId, messageId, dto, file);

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
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT') or hasAuthority('PROVIDER') or hasRole('PROVIDER')")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable("messageId") Long messageId,
            @PathVariable("attachmentId") Long attachmentId,
            Authentication authentication) {

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            String userEmail = authentication.getName();
            boolean isPatient = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("PATIENT") || auth.getAuthority().equals("ROLE_PATIENT"));
            boolean isProvider = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("PROVIDER") || auth.getAuthority().equals("ROLE_PROVIDER"));

            Long userId;
            if (isPatient) {
                // For patients, get portal user ID
                userId = portalUserRepository.findByEmail(userEmail)
                        .map(portalUser -> portalUser.getId())
                        .orElse(1L); // Fallback
            } else if (isProvider) {
                // For providers, get provider ID
                userId = providerRepository.findAll().stream()
                        .filter(provider -> userEmail.equals(provider.getEmail()))
                        .map(provider -> provider.getId())
                        .findFirst()
                        .orElse(1L); // Fallback
            } else {
                userId = 1L; // Fallback
            }

            MessageAttachmentService.DownloadResult result =
                portalMessageAttachmentService.getAttachmentDownload(userId, messageId, attachmentId);

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
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT') or hasAuthority('PROVIDER') or hasRole('PROVIDER')")
    public ResponseEntity<?> deleteAttachment(
            @PathVariable("messageId") Long messageId,
            @PathVariable("attachmentId") Long attachmentId,
            Authentication authentication) {

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Not authenticated"
                ));
            }

            String userEmail = authentication.getName();
            boolean isPatient = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("PATIENT") || auth.getAuthority().equals("ROLE_PATIENT"));
            boolean isProvider = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("PROVIDER") || auth.getAuthority().equals("ROLE_PROVIDER"));

            Long userId;
            if (isPatient) {
                // For patients, get portal user ID
                userId = portalUserRepository.findByEmail(userEmail)
                        .map(portalUser -> portalUser.getId())
                        .orElse(1L); // Fallback
            } else if (isProvider) {
                // For providers, get provider ID
                userId = providerRepository.findAll().stream()
                        .filter(provider -> userEmail.equals(provider.getEmail()))
                        .map(provider -> provider.getId())
                        .findFirst()
                        .orElse(1L); // Fallback
            } else {
                userId = 1L; // Fallback
            }

            ApiResponse<Void> response =
                portalMessageAttachmentService.deleteMessageAttachment(userId, messageId, attachmentId);

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

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.valueOf(value.toString());
    }
}