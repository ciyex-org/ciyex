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
            @RequestBody CommunicationDto dto,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

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
            @PathVariable("patientId") Long patientId,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
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

    /* ------------------- GET ONE ------------------- */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommunicationDto>> getOne(
            @PathVariable("id") Long id,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
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
            @PathVariable("id") Long id,
            @RequestBody CommunicationDto dto,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
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
    @PutMapping("/archive/{id}")
    public ResponseEntity<ApiResponse<CommunicationDto>> archive(
            @PathVariable("id") Long id,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
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

    /* ------------------- MARK AS READ ------------------- */
    @PutMapping("/read/{id}")
    public ResponseEntity<ApiResponse<CommunicationDto>> markAsRead(
            @PathVariable("id") Long id,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            CommunicationDto dto = service.markAsRead(id, "provider");

            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(true)
                    .message("Message marked as read successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to mark message as read id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(false)
                    .message("Failed to mark message as read: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- DELETE ------------------- */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("id") Long id,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
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
    public ResponseEntity<ApiResponse<List<CommunicationDto>>> getAllByOrg(
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

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
