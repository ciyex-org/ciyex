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

    // READ ALL: /api/patient-medical-history/{patientId}
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<PatientMedicalHistoryDto>>> getAllByPatient(
            @PathVariable Long patientId,
            @RequestHeader("orgId") Long orgId) {
        List<PatientMedicalHistoryDto> list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<PatientMedicalHistoryDto>>builder()
                .success(true).message("Patient Medical History fetched successfully")
                .data(list).build());
    }

    // READ ALL: /api/patient-medical-history/{patientId}/{encounterId}
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<PatientMedicalHistoryDto>>> getAllByPatientAndEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {
        List<PatientMedicalHistoryDto> list = service.getAllByPatientAndEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<PatientMedicalHistoryDto>>builder()
                .success(true).message("Patient Medical History for encounter fetched successfully")
                .data(list).build());
    }

    // READ ONE: /api/patient-medical-history/{patientId}/{encounterId}/{historyId}
    @GetMapping("/{patientId}/{encounterId}/{historyId}")
    public ResponseEntity<ApiResponse<PatientMedicalHistoryDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long historyId,
            @RequestHeader("orgId") Long orgId) {
        PatientMedicalHistoryDto dto = service.get(orgId, patientId, encounterId, historyId);
        return ResponseEntity.ok(ApiResponse.<PatientMedicalHistoryDto>builder()
                .success(true).message("Patient Medical History item fetched successfully")
                .data(dto).build());
    }

    // CREATE: /api/patient-medical-history/{patientId}/{encounterId}
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<PatientMedicalHistoryDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody PatientMedicalHistoryDto payload) {
        PatientMedicalHistoryDto created = service.create(orgId, patientId, encounterId, payload);
        return ResponseEntity.ok(ApiResponse.<PatientMedicalHistoryDto>builder()
                .success(true).message("Patient Medical History created successfully")
                .data(created).build());
    }

    // UPDATE: /api/patient-medical-history/{patientId}/{encounterId}/{historyId}
    @PutMapping("/{patientId}/{encounterId}/{historyId}")
    public ResponseEntity<ApiResponse<PatientMedicalHistoryDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long historyId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody PatientMedicalHistoryDto payload) {
        PatientMedicalHistoryDto updated = service.update(orgId, patientId, encounterId, historyId, payload);
        return ResponseEntity.ok(ApiResponse.<PatientMedicalHistoryDto>builder()
                .success(true).message("Patient Medical History updated successfully")
                .data(updated).build());
    }

    // DELETE: /api/patient-medical-history/{patientId}/{encounterId}/{historyId}
    @DeleteMapping("/{patientId}/{encounterId}/{historyId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long historyId,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, patientId, encounterId, historyId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Patient Medical History deleted successfully")
                .build());
    }
}
