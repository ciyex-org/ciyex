package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PatientEducationAssignmentDto;
import com.qiaben.ciyex.service.PatientEducationAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient-education-assignments")
@RequiredArgsConstructor
public class PatientEducationAssignmentController {

    private final PatientEducationAssignmentService service;

    @PostMapping("/{educationId}")
    public ResponseEntity<ApiResponse<PatientEducationAssignmentDto>> assign(
            @PathVariable("educationId") String educationId,
            @Valid @RequestBody PatientEducationAssignmentDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.<PatientEducationAssignmentDto>builder()
                    .success(true)
                    .message("Assigned successfully")
                    .data(service.assign(educationId, dto))
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<PatientEducationAssignmentDto>builder()
                    .success(false)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<PatientEducationAssignmentDto>builder()
                    .success(false)
                    .message("Failed to create assignment: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PatientEducationAssignmentDto>>> getByPatient(
            @PathVariable("patientId") Long patientId) {
        try {
            return ResponseEntity.ok(ApiResponse.<List<PatientEducationAssignmentDto>>builder()
                    .success(true)
                    .message("Assignments retrieved successfully")
                    .data(service.getByPatient(patientId))
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<List<PatientEducationAssignmentDto>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<List<PatientEducationAssignmentDto>>builder()
                    .success(false)
                    .message("Failed to retrieve assignments: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @PutMapping("/{id}/delivered")
    public ResponseEntity<ApiResponse<PatientEducationAssignmentDto>> markDelivered(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(ApiResponse.<PatientEducationAssignmentDto>builder()
                    .success(true)
                    .message("Marked delivered")
                    .data(service.markDelivered(id))
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<PatientEducationAssignmentDto>builder()
                    .success(false)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<PatientEducationAssignmentDto>builder()
                    .success(false)
                    .message("Failed to mark as delivered: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") String id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Assignment deleted")
                    .data(null)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete assignment: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCount() {
        try {
            long count = service.count();
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(true)
                    .message("Assignment count retrieved successfully")
                    .data(count)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<Long>builder()
                    .success(false)
                    .message("Failed to retrieve count: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientEducationAssignmentDto>>> getAll() {
        try {
            return ResponseEntity.ok(ApiResponse.<List<PatientEducationAssignmentDto>>builder()
                    .success(true)
                    .message("Assignments retrieved successfully")
                    .data(service.getAll())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<List<PatientEducationAssignmentDto>>builder()
                    .success(false)
                    .message("Failed to retrieve assignments: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientEducationAssignmentDto>> getById(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(ApiResponse.<PatientEducationAssignmentDto>builder()
                    .success(true)
                    .message("Assignment retrieved successfully")
                    .data(service.getById(id))
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<PatientEducationAssignmentDto>builder()
                    .success(false)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<PatientEducationAssignmentDto>builder()
                    .success(false)
                    .message("Failed to retrieve assignment: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

}