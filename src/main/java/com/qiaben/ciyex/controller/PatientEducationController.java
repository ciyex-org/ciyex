package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PatientEducationDto;
import com.qiaben.ciyex.service.PatientEducationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient-education")
@RequiredArgsConstructor
@Slf4j
public class PatientEducationController {

    private final PatientEducationService service;

    @PostMapping
    public ResponseEntity<ApiResponse<PatientEducationDto>> create(@RequestBody PatientEducationDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Patient education created successfully", service.create(dto)));
        } catch (Exception e) {
            log.error("Create failed", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to create: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientEducationDto>> get(@PathVariable String id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Retrieved successfully", service.getById(id)));
        } catch (Exception e) {
            log.error("Get failed: {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientEducationDto>> update(@PathVariable String id, @RequestBody PatientEducationDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Updated successfully", service.update(id, dto)));
        } catch (Exception e) {
            log.error("Update failed: {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to update: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Deleted successfully", null));
        } catch (Exception e) {
            log.error("Delete failed: {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to delete: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PatientEducationDto>>> getAll(@PageableDefault Pageable pageable) {
        try {
            Page<PatientEducationDto> page = service.getAll(pageable);
            return ResponseEntity.ok(ApiResponse.ok("Retrieved " + page.getTotalElements() + " items", page));
        } catch (Exception e) {
            log.error("GetAll failed", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve: " + e.getMessage()));
        }
    }
}
