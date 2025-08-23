package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PatientDto;
import com.qiaben.ciyex.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientService service;

    // Create a new Patient
    @PostMapping
    public ResponseEntity<ApiResponse<PatientDto>> create(@RequestBody PatientDto dto) {
        try {
            PatientDto createdPatient = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                    .success(true)
                    .message("Patient created successfully")
                    .data(createdPatient)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create patient", e);
            return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                    .success(false)
                    .message("Failed to create patient: " + e.getMessage())
                    .build());
        }
    }

    // Retrieve a patient by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientDto>> get(@PathVariable Long id) {
        try {
            PatientDto patient = service.getById(id);
            if (patient == null) {
                return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                        .success(false)
                        .message("Patient not found with id: " + id)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                    .success(true)
                    .message("Patient retrieved successfully")
                    .data(patient)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve patient with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                    .success(false)
                    .message("Failed to retrieve patient: " + e.getMessage())
                    .build());
        }
    }

    // Update an existing patient
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientDto>> update(@PathVariable Long id, @RequestBody PatientDto dto) {
        try {
            PatientDto updatedPatient = service.update(id, dto);
            if (updatedPatient == null) {
                return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                        .success(false)
                        .message("Patient not found with id: " + id)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                    .success(true)
                    .message("Patient updated successfully")
                    .data(updatedPatient)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update patient with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                    .success(false)
                    .message("Failed to update patient: " + e.getMessage())
                    .build());
        }
    }

    // Delete a patient by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Patient deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete patient with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete patient: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getPatientCount() {
        try {
            long count = service.countPatientsForCurrentOrg();
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(true)
                    .message("Patient count retrieved successfully")
                    .data(count)
                    .build());
        } catch (Exception e) {
            log.error("Failed to count patients", e);
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(false)
                    .message("Failed to count patients: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PatientDto>>> getAllPatients(
            @PageableDefault(sort = "id") Pageable pageable,
            @RequestParam(required = false) String search
    ) {
        try {
            Page<PatientDto> patients = service.getAllPatients(pageable, search);
            return ResponseEntity.ok(ApiResponse.<Page<PatientDto>>builder()
                    .success(true)
                    .message("Patients retrieved successfully")
                    .data(patients)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve patients", e);
            return ResponseEntity.ok(ApiResponse.<Page<PatientDto>>builder()
                    .success(false)
                    .message("Failed to retrieve patients: " + e.getMessage())
                    .build());
        }
    }
}