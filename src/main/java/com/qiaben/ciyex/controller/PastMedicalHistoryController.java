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
        List<PastMedicalHistoryDto> list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<PastMedicalHistoryDto>>builder()
                .success(true).message("PMH fetched successfully").data(list).build());
    }

    // READ ALL by encounter: /api/past-medical-history/{patientId}/{encounterId}
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<PastMedicalHistoryDto>>> getAllByPatientAndEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {
        List<PastMedicalHistoryDto> list = service.getAllByPatientAndEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<PastMedicalHistoryDto>>builder()
                .success(true).message("PMH by encounter fetched successfully").data(list).build());
    }

    // READ EXACT: /api/past-medical-history/{patientId}/{encounterId}/{pastMedicalHistoryId}
    @GetMapping("/{patientId}/{encounterId}/{pastMedicalHistoryId}")
    public ResponseEntity<ApiResponse<PastMedicalHistoryDto>> getExact(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long pastMedicalHistoryId,
            @RequestHeader("orgId") Long orgId) {
        PastMedicalHistoryDto dto = service.getExact(orgId, patientId, encounterId, pastMedicalHistoryId);
        return ResponseEntity.ok(ApiResponse.<PastMedicalHistoryDto>builder()
                .success(true).message("PMH item fetched successfully").data(dto).build());
    }

    // CREATE: /api/past-medical-history/{patientId}/{encounterId}
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<PastMedicalHistoryDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody PastMedicalHistoryDto dto) {
        PastMedicalHistoryDto created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<PastMedicalHistoryDto>builder()
                .success(true).message("PMH created successfully").data(created).build());
    }

    // UPDATE: /api/past-medical-history/{patientId}/{encounterId}/{pastMedicalHistoryId}
    @PutMapping("/{patientId}/{encounterId}/{pastMedicalHistoryId}")
    public ResponseEntity<ApiResponse<PastMedicalHistoryDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long pastMedicalHistoryId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody PastMedicalHistoryDto dto) {
        PastMedicalHistoryDto updated = service.update(orgId, patientId, encounterId, pastMedicalHistoryId, dto);
        return ResponseEntity.ok(ApiResponse.<PastMedicalHistoryDto>builder()
                .success(true).message("PMH updated successfully").data(updated).build());
    }

    // DELETE: /api/past-medical-history/{patientId}/{encounterId}/{pastMedicalHistoryId}
    @DeleteMapping("/{patientId}/{encounterId}/{pastMedicalHistoryId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long pastMedicalHistoryId,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, patientId, encounterId, pastMedicalHistoryId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("PMH deleted successfully").build());
    }
}
