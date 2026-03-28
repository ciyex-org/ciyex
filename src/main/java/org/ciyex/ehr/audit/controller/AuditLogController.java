package org.ciyex.ehr.audit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.audit.dto.AuditLogDto;
import org.ciyex.ehr.audit.service.AuditLogService;
import org.ciyex.ehr.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
@Slf4j
public class AuditLogController {

    private final AuditLogService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String sort) {
        try {
            var pageable = PageRequest.of(page, size);
            Page<AuditLogDto> result;

            if (q != null && !q.isBlank()) {
                result = service.search(q, pageable);
            } else if (userId != null && !userId.isBlank()) {
                result = service.getByUser(userId, pageable);
            } else if (patientId != null) {
                result = service.getByPatient(patientId, pageable);
            } else if (resourceType != null && !resourceType.isBlank()) {
                result = service.getByResourceType(resourceType, pageable);
            } else if (action != null && !action.isBlank()) {
                result = service.getByAction(action, pageable);
            } else {
                result = service.list(pageable);
            }

            return ResponseEntity.ok(ApiResponse.ok("Audit logs retrieved", result));
        } catch (Exception e) {
            log.error("Failed to retrieve audit logs", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve audit logs: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        try {
            var stats = service.getStats();
            return ResponseEntity.ok(ApiResponse.ok("Audit stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to retrieve audit stats", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve audit stats: " + e.getMessage()));
        }
    }

    @GetMapping("/resource-types")
    public ResponseEntity<ApiResponse<java.util.List<String>>> resourceTypes() {
        try {
            var types = service.getDistinctResourceTypes();
            return ResponseEntity.ok(ApiResponse.ok("Resource types retrieved", types));
        } catch (Exception e) {
            log.error("Failed to retrieve resource types", e);
            return ResponseEntity.ok(ApiResponse.ok("Resource types retrieved", java.util.List.of()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AuditLogDto>> create(@RequestBody AuditLogDto dto) {
        try {
            var created = service.log(dto);
            return ResponseEntity.ok(ApiResponse.ok("Audit log created", created));
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create audit log: " + e.getMessage()));
        }
    }
}
