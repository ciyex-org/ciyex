package org.ciyex.ehr.priorauth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.priorauth.dto.PriorAuthDto;
import org.ciyex.ehr.priorauth.service.PriorAuthService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Claim.read')")
@RestController
@RequestMapping("/api/prior-auth")
@RequiredArgsConstructor
@Slf4j
public class PriorAuthController {

    private final PriorAuthService service;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            if (q != null && !q.isBlank()) {
                var results = service.search(q, status);
                // Wrap list in a Page-like structure so the frontend can parse it uniformly
                int total = results.size();
                int fromIndex = Math.min(page * size, total);
                int toIndex = Math.min(fromIndex + size, total);
                var pageSlice = results.subList(fromIndex, toIndex);
                var pageData = Map.of(
                        "content", pageSlice,
                        "totalPages", (int) Math.ceil((double) total / size),
                        "totalElements", total
                );
                return ResponseEntity.ok(ApiResponse.ok("Prior authorizations retrieved", pageData));
            }
            if (status != null && !status.isBlank()) {
                var results = service.listByStatus(status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
                return ResponseEntity.ok(ApiResponse.ok("Prior authorizations retrieved", results));
            }
            var results = service.list(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
            return ResponseEntity.ok(ApiResponse.ok("Prior authorizations retrieved", results));
        } catch (Exception e) {
            log.error("Failed to list prior authorizations", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PriorAuthDto>> getById(@PathVariable Long id) {
        try {
            var auth = service.getById(id);
            return ResponseEntity.ok(ApiResponse.ok("Prior authorization retrieved", auth));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get prior authorization {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PriorAuthDto>>> getByPatient(@PathVariable Long patientId) {
        try {
            var auths = service.getByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Prior authorizations retrieved", auths));
        } catch (Exception e) {
            log.error("Failed to get prior authorizations for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats() {
        try {
            var stats = service.stats();
            return ResponseEntity.ok(ApiResponse.ok("Prior authorization stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get prior authorization stats", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PriorAuthDto>> create(@RequestBody PriorAuthDto dto) {
        try {
            var created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.ok("Prior authorization created", created));
        } catch (Exception e) {
            log.error("Failed to create prior authorization", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PriorAuthDto>> update(
            @PathVariable Long id, @RequestBody PriorAuthDto dto) {
        try {
            var updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Prior authorization updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update prior authorization {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PriorAuthDto>> approve(
            @PathVariable Long id, @RequestBody PriorAuthDto dto) {
        try {
            var approved = service.approve(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Prior authorization approved", approved));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to approve prior authorization {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/deny")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PriorAuthDto>> deny(
            @PathVariable Long id, @RequestBody PriorAuthDto dto) {
        try {
            var denied = service.deny(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Prior authorization denied", denied));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to deny prior authorization {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Prior authorization deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete prior authorization {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
