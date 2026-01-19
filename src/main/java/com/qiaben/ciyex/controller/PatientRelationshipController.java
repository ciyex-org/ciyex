package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PatientRelationshipDto;
import com.qiaben.ciyex.service.PatientRelationshipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{patientId}/relationships")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class PatientRelationshipController {

    private final PatientRelationshipService service;

    @Autowired
    public PatientRelationshipController(PatientRelationshipService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PatientRelationshipDto>> create(
            @PathVariable Long patientId,
            @RequestBody PatientRelationshipDto dto) {
        try {
            dto.setPatientId(patientId);
            PatientRelationshipDto created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<PatientRelationshipDto>builder()
                    .success(true)
                    .message("Patient relationship created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Error creating patient relationship", e);
            return ResponseEntity.ok(ApiResponse.<PatientRelationshipDto>builder()
                    .success(false)
                    .message("Failed to create patient relationship: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientRelationshipDto>>> getAll(@PathVariable Long patientId) {
        try {
            List<PatientRelationshipDto> relationships = service.getAllByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.<List<PatientRelationshipDto>>builder()
                    .success(true)
                    .message("Patient relationships retrieved successfully")
                    .data(relationships)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving patient relationships", e);
            return ResponseEntity.ok(ApiResponse.<List<PatientRelationshipDto>>builder()
                    .success(false)
                    .message("Failed to retrieve patient relationships: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientRelationshipDto>> getById(
            @PathVariable Long patientId,
            @PathVariable String id) {
        try {
            PatientRelationshipDto relationship = service.getById(id);
            if (!relationship.getPatientId().equals(patientId)) {
                return ResponseEntity.ok(ApiResponse.<PatientRelationshipDto>builder()
                        .success(false)
                        .message("Relationship with ID " + id + " does not belong to patient " + patientId)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<PatientRelationshipDto>builder()
                    .success(true)
                    .message("Patient relationship retrieved successfully")
                    .data(relationship)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving patient relationship", e);
            return ResponseEntity.ok(ApiResponse.<PatientRelationshipDto>builder()
                    .success(false)
                    .message("Failed to retrieve patient relationship: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientRelationshipDto>> update(
            @PathVariable Long patientId,
            @PathVariable String id,
            @RequestBody PatientRelationshipDto dto) {
        try {
            PatientRelationshipDto existing = service.getById(id);
            if (!existing.getPatientId().equals(patientId)) {
                return ResponseEntity.ok(ApiResponse.<PatientRelationshipDto>builder()
                        .success(false)
                        .message("Relationship with ID " + id + " does not belong to patient " + patientId)
                        .build());
            }
            dto.setPatientId(patientId);
            PatientRelationshipDto updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<PatientRelationshipDto>builder()
                    .success(true)
                    .message("Patient relationship updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Error updating patient relationship", e);
            return ResponseEntity.ok(ApiResponse.<PatientRelationshipDto>builder()
                    .success(false)
                    .message("Failed to update patient relationship: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable String id) {
        try {
            PatientRelationshipDto existing = service.getById(id);
            if (!existing.getPatientId().equals(patientId)) {
                return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .success(false)
                        .message("Relationship with ID " + id + " does not belong to patient " + patientId)
                        .build());
            }
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Patient relationship deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error deleting patient relationship", e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete patient relationship: " + e.getMessage())
                    .build());
        }
    }
}