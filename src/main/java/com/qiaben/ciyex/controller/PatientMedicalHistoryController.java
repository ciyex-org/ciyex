package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PatientMedicalHistoryDto;
import com.qiaben.ciyex.service.PatientMedicalHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient-medical-history")
@RequiredArgsConstructor
@Slf4j
public class PatientMedicalHistoryController {

    private final PatientMedicalHistoryService service;

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<PatientMedicalHistoryDto>>> getAllByPatient(
            @PathVariable Long patientId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<PatientMedicalHistoryDto>>builder()
                .success(true).message("PMH fetched successfully").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<PatientMedicalHistoryDto>>> getAllByEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<PatientMedicalHistoryDto>>builder()
                .success(true).message("PMH fetched successfully").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PatientMedicalHistoryDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        var dto = service.getOne(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<PatientMedicalHistoryDto>builder()
                .success(true).message("PMH fetched successfully").data(dto).build());
    }

    // <-- THIS is the mapping you’re calling
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<PatientMedicalHistoryDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody PatientMedicalHistoryDto dto) {
        var created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<PatientMedicalHistoryDto>builder()
                .success(true).message("PMH created").data(created).build());
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PatientMedicalHistoryDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody PatientMedicalHistoryDto dto) {
        var updated = service.update(orgId, patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<PatientMedicalHistoryDto>builder()
                .success(true).message("PMH updated").data(updated).build());
    }

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
