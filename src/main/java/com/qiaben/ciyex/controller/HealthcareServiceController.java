package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.HealthcareServiceDto;
import com.qiaben.ciyex.service.HealthcareServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/healthcare-services")
public class HealthcareServiceController {

    private final HealthcareServiceService service;

    @Autowired
    public HealthcareServiceController(HealthcareServiceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HealthcareServiceDto>> create(@RequestBody HealthcareServiceDto dto) {
        HealthcareServiceDto createdService = service.create(dto);
        ApiResponse<HealthcareServiceDto> response = new ApiResponse.Builder<HealthcareServiceDto>()
                .success(true)
                .message("Healthcare Service created successfully")
                .data(createdService)
                .build();
        return ResponseEntity.ok(response);
    }


/*
    @GetMapping
    public ResponseEntity<ApiResponse<List<HealthcareServiceDto>>> getByOrgId() {
        List<HealthcareServiceDto> services = service.getByOrgId();
        ApiResponse<List<HealthcareServiceDto>> response = new ApiResponse.Builder<List<HealthcareServiceDto>>()
                .success(true)
                .message("Healthcare Services fetched successfully")
                .data(services)
                .build();
        return ResponseEntity.ok(response);
    }
*/


    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HealthcareServiceDto>> update(
            @PathVariable Long id,
            @RequestBody HealthcareServiceDto dto
            ) {

        HealthcareServiceDto updatedService = service.update(id, dto);
        ApiResponse<HealthcareServiceDto> response = new ApiResponse.Builder<HealthcareServiceDto>()
                .success(true)
                .message("Healthcare Service updated successfully")
                .data(updatedService)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id ) {
        service.delete(id);
        ApiResponse<Void> response = new ApiResponse.Builder<Void>()
                .success(true)
                .message("Healthcare Service deleted successfully")
                .build();
        return ResponseEntity.ok(response);
    }


}
