package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PatientEducationDto;
import com.qiaben.ciyex.service.PatientEducationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient-education")
public class PatientEducationController {

    private final PatientEducationService service;

    public PatientEducationController(PatientEducationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PatientEducationDto>> create(@RequestBody PatientEducationDto dto) {
        PatientEducationDto created = service.create(dto);
        return ResponseEntity.ok(ApiResponse.<PatientEducationDto>builder()
                .success(true)
                .message("Patient education created successfully")
                .data(created)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientEducationDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<PatientEducationDto>builder()
                .success(true)
                .message("Patient education retrieved successfully")
                .data(service.getById(id))
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientEducationDto>> update(@PathVariable Long id, @RequestBody PatientEducationDto dto) {
        return ResponseEntity.ok(ApiResponse.<PatientEducationDto>builder()
                .success(true)
                .message("Patient education updated successfully")
                .data(service.update(id, dto))
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Patient education deleted successfully")
                .data(null)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PatientEducationDto>>> getAll(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<PatientEducationDto>>builder()
                .success(true)
                .message("Patient educations retrieved successfully")
                .data(service.getAll(pageable))
                .build());
    }
}
