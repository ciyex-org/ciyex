package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PastMedicalHistoryDto;
import com.qiaben.ciyex.service.PastMedicalHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/past-medical-history")
@RequiredArgsConstructor
@Slf4j
public class PastMedicalHistoryController {

    private final PastMedicalHistoryService service;

    // READ ALL: /api/past-medical-history/{patientId}
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<PastMedicalHistoryDto>>> getAllByPatient(
            @PathVariable Long patientId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<PastMedicalHistoryDto>>builder()
                .success(true).message("PMH fetched successfully").data(list).build());
    }

    // READ ALL: /api/past-medical-history/{patientId}/{encounterId}
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<PastMedicalHistoryDto>>> getAllByEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<PastMedicalHistoryDto>>builder()
                .success(true).message("PMH fetched successfully").data(list).build());
    }

    // READ ONE: /api/past-medical-history/{patientId}/{encounterId}/{id}
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PastMedicalHistoryDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        var dto = service.getOne(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<PastMedicalHistoryDto>builder()
                .success(true).message("PMH fetched successfully").data(dto).build());
    }

    // CREATE: /api/past-medical-history/{patientId}/{encounterId}
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<PastMedicalHistoryDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody PastMedicalHistoryDto dto) {
        var created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<PastMedicalHistoryDto>builder()
                .success(true).message("PMH created").data(created).build());
    }

    // UPDATE: /api/past-medical-history/{patientId}/{encounterId}/{id}
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PastMedicalHistoryDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody PastMedicalHistoryDto dto) {
        var updated = service.update(orgId, patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<PastMedicalHistoryDto>builder()
                .success(true).message("PMH updated").data(updated).build());
    }

    // DELETE: /api/past-medical-history/{patientId}/{encounterId}/{id}
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("PMH deleted").build());
    }
}
