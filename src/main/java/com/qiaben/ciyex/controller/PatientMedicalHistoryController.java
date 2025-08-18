package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.PatientMedicalHistoryDto;
import com.qiaben.ciyex.service.PatientMedicalHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/1/patient-medical-history")
public class PatientMedicalHistoryController {

    private final PatientMedicalHistoryService service;

    public PatientMedicalHistoryController(PatientMedicalHistoryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PatientMedicalHistoryDto> createMedicalHistory(
            @RequestHeader("orgId") Long orgId,
            @RequestBody PatientMedicalHistoryDto dto) {

        PatientMedicalHistoryDto createdHistory = service.createMedicalHistory(orgId, dto);
        return ResponseEntity.ok(createdHistory);
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<List<PatientMedicalHistoryDto>> getPatientMedicalHistory(
            @RequestHeader("orgId") Long orgId,
            @PathVariable Long patientId) {

        List<PatientMedicalHistoryDto> histories = service.getPatientMedicalHistory(orgId, patientId);
        return ResponseEntity.ok(histories);
    }
}
