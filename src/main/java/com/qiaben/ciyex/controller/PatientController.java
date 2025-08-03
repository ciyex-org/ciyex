package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PatientDto;
import com.qiaben.ciyex.service.PatientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@Slf4j
public class PatientController {

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

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
            log.error("Failed to create patient: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                    .success(false)
                    .message("Failed to create patient: " + e.getMessage())
                    .build());
        }
    }

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
            log.error("Failed to retrieve patient with id {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                    .success(false)
                    .message("Failed to retrieve patient: " + e.getMessage())
                    .build());
        }
    }

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
            log.error("Failed to update patient with id {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                    .success(false)
                    .message("Failed to update patient: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Patient deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete patient with id {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete patient: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientDto>>> getAllPatients() {
        try {
            ApiResponse<List<PatientDto>> response = service.getAllPatients();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve all patients: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<List<PatientDto>>builder()
                    .success(false)
                    .message("Failed to retrieve patients: " + e.getMessage())
                    .build());
        }
    }
}