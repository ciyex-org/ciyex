package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ImmunizationDto;
import com.qiaben.ciyex.service.ImmunizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/immunizations")
public class ImmunizationController {

    private final ImmunizationService service;

    @Autowired
    public ImmunizationController(ImmunizationService service) {
        this.service = service;
    }

    // Create Immunization
    @PostMapping
    public ResponseEntity<ApiResponse<ImmunizationDto>> create(@RequestBody ImmunizationDto dto, @RequestHeader(value = "orgId") Long orgId) {
        ImmunizationDto createdImmunization = service.create(dto, orgId);
        ApiResponse<ImmunizationDto> response = new ApiResponse.Builder<ImmunizationDto>()
                .success(true)
                .message("Immunization created successfully")
                .data(createdImmunization)
                .build();
        return ResponseEntity.ok(response);
    }

    // Read All Immunizations by orgId
    @GetMapping
    public ResponseEntity<ApiResponse<List<ImmunizationDto>>> getByOrgId(@RequestHeader(value = "orgId") Long orgId) {
        List<ImmunizationDto> immunizations = service.getByOrgId(orgId);
        ApiResponse<List<ImmunizationDto>> response = new ApiResponse.Builder<List<ImmunizationDto>>()
                .success(true)
                .message("Immunizations fetched successfully")
                .data(immunizations)
                .build();
        return ResponseEntity.ok(response);
    }

    // Read Immunization by id
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ImmunizationDto>> getById(@PathVariable Long id) {
        ImmunizationDto immunization = service.getById(id);
        if (immunization != null) {
            ApiResponse<ImmunizationDto> response = new ApiResponse.Builder<ImmunizationDto>()
                    .success(true)
                    .message("Immunization fetched successfully")
                    .data(immunization)
                    .build();
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<ImmunizationDto> response = new ApiResponse.Builder<ImmunizationDto>()
                    .success(false)
                    .message("Immunization not found")
                    .build();
            return ResponseEntity.status(404).body(response);
        }
    }

    // Update Immunization by id
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ImmunizationDto>> update(@PathVariable Long id, @RequestBody ImmunizationDto dto, @RequestHeader(value = "orgId") Long orgId) {
        ImmunizationDto updatedImmunization = service.update(id, dto, orgId);
        if (updatedImmunization != null) {
            ApiResponse<ImmunizationDto> response = new ApiResponse.Builder<ImmunizationDto>()
                    .success(true)
                    .message("Immunization updated successfully")
                    .data(updatedImmunization)
                    .build();
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<ImmunizationDto> response = new ApiResponse.Builder<ImmunizationDto>()
                    .success(false)
                    .message("Immunization not found")
                    .build();
            return ResponseEntity.status(404).body(response);
        }
    }

    // Delete Immunization by id
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        boolean deleted = service.delete(id);
        if (deleted) {
            ApiResponse<String> response = new ApiResponse.Builder<String>()
                    .success(true)
                    .message("Immunization deleted successfully")
                    .build();
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<String> response = new ApiResponse.Builder<String>()
                    .success(false)
                    .message("Immunization not found")
                    .build();
            return ResponseEntity.status(404).body(response);
        }
    }
}
