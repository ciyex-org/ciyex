package org.ciyex.ehr.portal.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.portal.dto.PortalAccessRequestDto;
import org.ciyex.ehr.portal.service.PortalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_patient/Patient.read')")
@RestController
@RequestMapping("/api/portal/requests")
@RequiredArgsConstructor
@Slf4j
public class PortalAccessController {

    private final PortalService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PortalAccessRequestDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            var results = service.listRequests(pageable);
            return ResponseEntity.ok(ApiResponse.ok("Access requests retrieved", results));
        } catch (Exception e) {
            log.error("Failed to list access requests", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PortalAccessRequestDto>> getOne(@PathVariable Long id) {
        try {
            var request = service.getRequest(id);
            return ResponseEntity.ok(ApiResponse.ok("Access request retrieved", request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get access request {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('SCOPE_patient/Patient.read')")
    public ResponseEntity<ApiResponse<PortalAccessRequestDto>> approve(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            var approved = service.approveRequest(id, body.getOrDefault("approvedBy", "system"));
            return ResponseEntity.ok(ApiResponse.ok("Access request approved", approved));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to approve access request {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/deny")
    @PreAuthorize("hasAuthority('SCOPE_patient/Patient.read')")
    public ResponseEntity<ApiResponse<PortalAccessRequestDto>> deny(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            var denied = service.denyRequest(id, body.getOrDefault("reason", ""));
            return ResponseEntity.ok(ApiResponse.ok("Access request denied", denied));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to deny access request {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats() {
        try {
            var stats = service.requestStats();
            return ResponseEntity.ok(ApiResponse.ok("Request stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get request stats", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
