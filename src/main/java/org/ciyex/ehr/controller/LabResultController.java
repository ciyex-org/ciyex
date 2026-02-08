package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.LabResultDto;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.service.LabResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lab-result")
@RequiredArgsConstructor
@Slf4j
public class LabResultController {

    private final LabResultService service;

    // ---------- SEARCH ----------
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<LabResultDto>>> search(@RequestParam("q") String q) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);
            var results = service.search(q);
            return ResponseEntity.ok(ApiResponse.<List<LabResultDto>>builder()
                    .success(true)
                    .message("Lab results search results")
                    .data(results)
                    .build());
        } catch (Exception e) {
            log.error("Failed to search lab results: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<List<LabResultDto>>builder()
                    .success(false)
                    .message("Failed to search lab results: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    // ---------- LIST for patient ----------
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<LabResultDto>>> listForPatient(@PathVariable Long patientId) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);
            var filtered = service.getForPatient(patientId);
            return ResponseEntity.ok(ApiResponse.<List<LabResultDto>>builder()
                    .success(true)
                    .message("Lab results retrieved successfully")
                    .data(filtered)
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Patient not found with id {}", patientId);
            return ResponseEntity.ok(ApiResponse.<List<LabResultDto>>builder()
                    .success(false)
                    .message("Patient ID not found")
                    .build());
        } catch (Exception e) {
            log.error("Failed to list lab results for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<List<LabResultDto>>builder()
                    .success(false)
                    .message("Failed to list lab results: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    // ---------- READ single for patient ----------
    @GetMapping("/{patientId}/{id}")
    public ResponseEntity<ApiResponse<LabResultDto>> getForPatient(@PathVariable Long patientId, @PathVariable Long id) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);
            var dto = service.getOne(id);
            if (dto == null || dto.getPatientId() == null || !Objects.equals(dto.getPatientId(), patientId)) {
                return ResponseEntity.ok(ApiResponse.<LabResultDto>builder()
                        .success(false)
                        .message("Lab result not found for the specified patient")
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<LabResultDto>builder()
                    .success(true)
                    .message("Lab result retrieved successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to get lab result id {} for patientId {}: {}", id, patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<LabResultDto>builder()
                    .success(false)
                    .message("Failed to retrieve lab result: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    // ---------- CREATE for patient ----------
    @PostMapping("/{patientId}")
    public ResponseEntity<ApiResponse<LabResultDto>> createForPatient(@PathVariable Long patientId, @RequestBody LabResultDto dto) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);
            if (dto.getPatientId() == null) {
                dto.setPatientId(patientId);
            } else if (!patientId.equals(dto.getPatientId())) {
                throw new IllegalArgumentException("patientId in path does not match patientId in payload");
            }
            var created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<LabResultDto>builder()
                    .success(true)
                    .message("Lab result created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create lab result for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<LabResultDto>builder()
                    .success(false)
                    .message("Failed to create lab result: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    // ---------- UPDATE for patient ----------
    @PutMapping("/{patientId}/{id}")
    public ResponseEntity<ApiResponse<LabResultDto>> updateForPatient(@PathVariable Long patientId, @PathVariable Long id, @RequestBody LabResultDto dto) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);
            var existing = service.getOne(id);
            if (existing == null || existing.getPatientId() == null || !Objects.equals(existing.getPatientId(), patientId)) {
                return ResponseEntity.ok(ApiResponse.<LabResultDto>builder()
                        .success(false)
                        .message("Lab result not found for the specified patient")
                        .build());
            }
            dto.setPatientId(patientId);
            var updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<LabResultDto>builder()
                    .success(true)
                    .message("Lab result updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update lab result id {} for patientId {}: {}", id, patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<LabResultDto>builder()
                    .success(false)
                    .message("Failed to update lab result: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    // ---------- DELETE for patient ----------
    @DeleteMapping("/{patientId}/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForPatient(@PathVariable Long patientId, @PathVariable Long id) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);
            var existing = service.getOne(id);
            if (existing == null || existing.getPatientId() == null || !Objects.equals(existing.getPatientId(), patientId)) {
                return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .success(false)
                        .message("Lab result not found for the specified patient")
                        .build());
            }
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Lab result deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete lab result id {} for patientId {}: {}", id, patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete lab result: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }
}
