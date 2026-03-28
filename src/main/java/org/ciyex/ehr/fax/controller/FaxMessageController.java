package org.ciyex.ehr.fax.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.fax.dto.FaxMessageDto;
import org.ciyex.ehr.fax.service.FaxMessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
@RestController
@RequestMapping("/api/fax")
@RequiredArgsConstructor
@Slf4j
public class FaxMessageController {

    private final FaxMessageService service;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            if (q != null && !q.isBlank()) {
                List<FaxMessageDto> list = service.search(q);
                Page<FaxMessageDto> pageResult = new PageImpl<>(list, pageable, list.size());
                return ResponseEntity.ok(ApiResponse.ok("Fax messages retrieved", pageResult));
            }
            if (direction != null && !direction.isBlank()) {
                var results = service.listByDirection(direction, pageable);
                return ResponseEntity.ok(ApiResponse.ok("Fax messages retrieved", results));
            }
            var results = service.getAll(pageable);
            return ResponseEntity.ok(ApiResponse.ok("Fax messages retrieved", results));
        } catch (Exception e) {
            log.error("Failed to list fax messages", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to list fax messages: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FaxMessageDto>> getById(@PathVariable Long id) {
        try {
            var fax = service.getById(id);
            return ResponseEntity.ok(ApiResponse.ok("Fax message retrieved", fax));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get fax message {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        try {
            var stats = service.getStats();
            return ResponseEntity.ok(ApiResponse.ok("Fax stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get fax stats", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FaxMessageDto>> create(@RequestBody FaxMessageDto dto) {
        try {
            var created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.ok("Fax message created", created));
        } catch (Exception e) {
            log.error("Failed to create fax message", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FaxMessageDto>> update(
            @PathVariable Long id, @RequestBody FaxMessageDto dto) {
        try {
            var updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Fax message updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update fax message {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<FaxMessageDto>> assign(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Long patientId = body.get("patientId") != null ? Long.valueOf(body.get("patientId").toString()) : null;
            String patientName = body.get("patientName") != null ? body.get("patientName").toString() : null;
            String category = body.get("category") != null ? body.get("category").toString() : null;
            if (patientId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("patientId is required"));
            }
            var assigned = service.assignToPatient(id, patientId, patientName, category);
            return ResponseEntity.ok(ApiResponse.ok("Fax assigned to patient", assigned));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to assign fax message {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<ApiResponse<FaxMessageDto>> process(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String processedBy = body.get("processedBy");
            if (processedBy == null || processedBy.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("processedBy is required"));
            }
            var processed = service.markProcessed(id, processedBy);
            return ResponseEntity.ok(ApiResponse.ok("Fax marked as processed", processed));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to process fax message {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Fax message deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete fax message {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
