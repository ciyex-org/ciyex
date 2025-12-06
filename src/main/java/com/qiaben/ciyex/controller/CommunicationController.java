package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.CommunicationDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.CommunicationStatus;
import com.qiaben.ciyex.service.CommunicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/communications")
@Slf4j
public class CommunicationController {

    private final CommunicationService service;

    public CommunicationController(CommunicationService service) {
        this.service = service;
    }

    /* ------------------- CREATE ------------------- */
    @PostMapping
    public ResponseEntity<ApiResponse<CommunicationDto>> create(
            @RequestBody CommunicationDto dto) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);

            // Validate required fields
            if (dto.getPatientId() == null) {
                return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                        .success(false)
                        .message("Patient ID is required")
                        .build());
            }
            if (dto.getProviderId() == null) {
                return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                        .success(false)
                        .message("Provider ID is required")
                        .build());
            }
            if (dto.getPayload() == null || dto.getPayload().trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                        .success(false)
                        .message("Message content is required")
                        .build());
            }

            // Ensure fromType and messageType are set
            if (dto.getFromType() == null || dto.getFromType().trim().isEmpty()) {
                // Default to provider for EHR endpoint
                dto.setFromType("provider");
            }
            if (dto.getMessageType() == null || dto.getMessageType().trim().isEmpty()) {
                dto.setMessageType("provider_to_patient");
            }

            log.info("Creating communication: patientId={}, providerId={}, fromType={}, messageType={}",
                    dto.getPatientId(), dto.getProviderId(), dto.getFromType(), dto.getMessageType());

            CommunicationDto created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(true)
                    .message("Communication created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create Communication: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(false)
                    .message("Failed to create Communication: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- GET BY PATIENT ------------------- */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<CommunicationDto>>> getByPatient(
            @PathVariable Long patientId) {
        try {
            RequestContext ctx = new RequestContext();
            // orgId deprecated; tenantName populated by interceptor.
            RequestContext.set(ctx);

            List<CommunicationDto> list = service.getByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.<List<CommunicationDto>>builder()
                    .success(true)
                    .message("Communications retrieved successfully")
                    .data(list)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve Communications for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<List<CommunicationDto>>builder()
                    .success(false)
                    .message("Failed to retrieve Communications: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- GET CONVERSATION ------------------- */
    @GetMapping("/conversation")
    public ResponseEntity<ApiResponse<List<CommunicationDto>>> getConversation(
            @RequestParam Long patientId,
            @RequestParam Long providerId) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);

            List<CommunicationDto> conversation = service.getConversation(patientId, providerId);
            return ResponseEntity.ok(ApiResponse.<List<CommunicationDto>>builder()
                    .success(true)
                    .message("Conversation retrieved successfully")
                    .data(conversation)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve conversation for patient {} and provider {}: {}",
                    patientId, providerId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<List<CommunicationDto>>builder()
                    .success(false)
                    .message("Failed to retrieve conversation: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- GET ONE ------------------- */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommunicationDto>> getOne(
            @PathVariable Long id) {
        try {
            RequestContext ctx = new RequestContext();
            // orgId deprecated; tenantName populated by interceptor.
            RequestContext.set(ctx);

            CommunicationDto dto = service.searchAll().stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Communication not found: " + id));

            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(true)
                    .message("Communication retrieved successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve Communication id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(false)
                    .message("Failed to retrieve Communication: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- UPDATE ------------------- */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CommunicationDto>> update(
            @PathVariable Long id,
            @RequestBody CommunicationDto dto) {
        try {
            RequestContext ctx = new RequestContext();
            // orgId deprecated; tenantName populated by interceptor.
            RequestContext.set(ctx);

            CommunicationDto updated = service.updateItem(null, id, dto);
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(true)
                    .message("Communication updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update Communication id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(false)
                    .message("Failed to update Communication: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- ARCHIVE ------------------- */
    @PutMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<CommunicationDto>> archive(
            @PathVariable Long id) {
        try {
            RequestContext ctx = new RequestContext();
            // orgId deprecated; tenantName populated by interceptor.
            RequestContext.set(ctx);

            CommunicationDto dto = service.setStatus(id, CommunicationStatus.ARCHIVED);

            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(true)
                    .message("Communication archived successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to archive Communication id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(false)
                    .message("Failed to archive Communication: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- RESTORE ------------------- */
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<CommunicationDto>> restore(
            @PathVariable Long id) {
        try {
            RequestContext ctx = new RequestContext();
            // orgId deprecated; tenantName populated by interceptor.
            RequestContext.set(ctx);

            CommunicationDto dto = service.setStatus(id, CommunicationStatus.SENT);

            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(true)
                    .message("Communication restored successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to restore Communication id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(false)
                    .message("Failed to restore Communication: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- MARK AS READ ------------------- */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<CommunicationDto>> markAsRead(
            @PathVariable Long id,
            @RequestParam String readBy) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);

            CommunicationDto dto = service.markAsRead(id, readBy);
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(true)
                    .message("Communication marked as read")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to mark communication {} as read: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(false)
                    .message("Failed to mark as read: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- DELETE ------------------- */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id) {
        try {
            RequestContext ctx = new RequestContext();
            // orgId deprecated; tenantName populated by interceptor.
            RequestContext.set(ctx);

            service.deleteItemById(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Communication deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete Communication id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete Communication: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- GET ALL ------------------- */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommunicationDto>>> getAllByOrg() {
        try {
            List<CommunicationDto> all = service.searchAll();
            return ResponseEntity.ok(ApiResponse.<List<CommunicationDto>>builder()
                    .success(true)
                    .message("Communications retrieved successfully")
                    .data(all)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve all Communications: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<List<CommunicationDto>>builder()
                    .success(false)
                    .message("Failed to retrieve Communications: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }
}
