package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.AssessmentDto;
import com.qiaben.ciyex.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessment")
@RequiredArgsConstructor
@Slf4j
public class AssessmentController {

    private final AssessmentService service;

    // READ ALL: /api/assessment/{patientId}
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<AssessmentDto>>> getAllByPatient(
            @PathVariable Long patientId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<AssessmentDto>>builder()
                .success(true).message("Assessment list fetched").data(list).build());
    }

    // READ ALL: /api/assessment/{patientId}/{encounterId}
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<AssessmentDto>>> getAllByEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<AssessmentDto>>builder()
                .success(true).message("Assessment list fetched").data(list).build());
    }

    // READ ONE: /api/assessment/{patientId}/{encounterId}/{id}
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<AssessmentDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        var dto = service.getOne(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<AssessmentDto>builder()
                .success(true).message("Assessment fetched").data(dto).build());
    }

    // CREATE: /api/assessment/{patientId}/{encounterId}
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<AssessmentDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody AssessmentDto dto) {
        var created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<AssessmentDto>builder()
                .success(true).message("Assessment created").data(created).build());
    }

    // UPDATE: /api/assessment/{patientId}/{encounterId}/{id}
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<AssessmentDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody AssessmentDto dto) {
        var updated = service.update(orgId, patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<AssessmentDto>builder()
                .success(true).message("Assessment updated").data(updated).build());
    }

    // DELETE: /api/assessment/{patientId}/{encounterId}/{id}
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Assessment deleted").build());
    }
}
