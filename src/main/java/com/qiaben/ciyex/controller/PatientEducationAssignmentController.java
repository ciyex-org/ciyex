package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PatientEducationAssignmentDto;
import com.qiaben.ciyex.service.PatientEducationAssignmentService;
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
            @PathVariable String educationId,
            @RequestBody PatientEducationAssignmentDto dto) {
        return ResponseEntity.ok(ApiResponse.<PatientEducationAssignmentDto>builder()
                .success(true)
                .message("Assigned successfully")
                .data(service.assign(educationId, dto))
                .build());
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PatientEducationAssignmentDto>>> getByPatient(
            @PathVariable Long patientId) {
        return ResponseEntity.ok(ApiResponse.<List<PatientEducationAssignmentDto>>builder()
                .success(true)
                .message("Assignments retrieved successfully")
                .data(service.getByPatient(patientId))
                .build());
    }

    @PutMapping("/{id}/delivered")
    public ResponseEntity<ApiResponse<PatientEducationAssignmentDto>> markDelivered(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.<PatientEducationAssignmentDto>builder()
                .success(true)
                .message("Marked delivered")
                .data(service.markDelivered(id))
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Assignment deleted")
                .data(null)
                .build());
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCount() {
        long count = service.count();
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .success(true)
                .message("Assignment count retrieved successfully")
                .data(count)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientEducationAssignmentDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<PatientEducationAssignmentDto>>builder()
                .success(true)
                .message("Assignments retrieved successfully")
                .data(service.getAll())
                .build());
    }

}

