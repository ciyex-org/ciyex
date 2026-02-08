package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.PatientDto;
import org.ciyex.ehr.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.ciyex.ehr.service.PatientHistoryService;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientService service;
    private final PatientHistoryService historyService;

    // ✅ Create a new Patient with validation
    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody PatientDto dto, BindingResult result) {
        // 🔹 Validation check before proceeding
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("Validation failed")
                            .data(errors)
                            .build());
        }

        try {
            PatientDto createdPatient = service.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<PatientDto>builder()
                    .success(true)
                    .message("Patient created successfully")
                    .data(createdPatient)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create patient", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<PatientDto>builder()
                    .success(false)
                    .message("Failed to create patient: " + e.getMessage())
                    .build());
        }
    }

    // ✅ Retrieve a patient by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientDto>> get(@PathVariable("id") Long id) {
        try {
            PatientDto patient = service.getById(id);
            return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                    .success(true)
                    .message("Patient retrieved successfully")
                    .data(patient)
                    .build());
        } catch (RuntimeException e) {
            log.error("Patient not found with id {}", id);
            return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                    .success(false)
                    .message("No patient id matches")
                    .data(null)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve patient with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                    .success(false)
                    .message("No patient id matches")
                    .data(null)
                    .build());
        }
    }

    // ✅ Update an existing patient (with validation)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable("id") Long id, @Valid @RequestBody PatientDto dto, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("Validation failed")
                            .data(errors)
                            .build());
        }

        try {
            PatientDto updatedPatient = service.update(id, dto);
            if (updatedPatient == null) {
                return ResponseEntity.ok(ApiResponse.<PatientDto>builder()
                        .success(false)
                        .message("No patient id matches")
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
                    .message("No patient id matches")
                    .build());
        }
    }

    // ✅ Delete a patient by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
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
                    .message("No patient id matches")
                    .build());
        }
    }


    // ✅ Get all patients with optional search
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PatientDto>>> getAllPatients(
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(name = "sort", required = false, defaultValue = "id") String sort,
            @RequestParam(name = "search", required = false) String search
    ) {
        try {
            Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by(sort));
            Page<PatientDto> patients = service.getAllPatients(pageable, search);
            return ResponseEntity.ok(ApiResponse.<Page<PatientDto>>builder()
                    .success(true)
                    .message("Patients retrieved successfully")
                    .data(patients)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve patients", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<Page<PatientDto>>builder()
                    .success(false)
                    .message("Failed to retrieve patients: " + e.getMessage())
                    .build());
        }
    }

    // ✅ Count all patients
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

    // ✅ Patient history endpoint
    @PostMapping("/{id}/history")
    public ResponseEntity<ApiResponse<Object>> createPatientHistory(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Object historyData) {
        try {
            service.getById(id); // Validate patient exists
            Object savedData = historyService.saveHistory(id, historyData);

            return ResponseEntity.ok(ApiResponse.<Object>builder()
                    .success(true)
                    .message("Patient history saved successfully")
                    .data(savedData)
                    .build());
        } catch (RuntimeException e) {
            log.error("Patient not found with id {}", id);
            return ResponseEntity.ok(ApiResponse.<Object>builder()
                    .success(false)
                    .message("No patient id matches")
                    .build());
        } catch (Exception e) {
            log.error("Failed to save patient history for id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Object>builder()
                    .success(false)
                    .message("No patient id matches")
                    .build());
        }
    }

    // ✅ Get patient history endpoint
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<Object>> getPatientHistory(@PathVariable("id") Long id) {
        try {
            service.getById(id); // Validate patient exists
            Object historyData = historyService.getHistory(id);
            return ResponseEntity.ok(ApiResponse.<Object>builder()
                    .success(true)
                    .message("Patient history retrieved successfully")
                    .data(historyData)
                    .build());
        } catch (RuntimeException e) {
            log.error("Patient not found with id {}", id);
            return ResponseEntity.ok(ApiResponse.<Object>builder()
                    .success(false)
                    .message("No patient id matches")
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve patient history for id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Object>builder()
                    .success(false)
                    .message("No patient id matches")
                    .build());
        }
    }
}