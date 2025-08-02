package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.core.LocationDto;
import com.qiaben.ciyex.service.core.LocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@Slf4j
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LocationDto>> create(@RequestBody LocationDto dto) {
        try {
            LocationDto createdLocation = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<LocationDto>builder()
                    .success(true)
                    .message("Location created successfully")
                    .data(createdLocation)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create location: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<LocationDto>builder()
                    .success(false)
                    .message("Failed to create location: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<ApiResponse<LocationDto>> get(@PathVariable String externalId) {
        try {
            LocationDto location = service.getById(externalId);
            return ResponseEntity.ok(ApiResponse.<LocationDto>builder()
                    .success(true)
                    .message("Location retrieved successfully")
                    .data(location)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve location with externalId {}: {}", externalId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<LocationDto>builder()
                    .success(false)
                    .message("Failed to retrieve location: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{externalId}")
    public ResponseEntity<ApiResponse<LocationDto>> update(@PathVariable String externalId, @RequestBody LocationDto dto) {
        try {
            LocationDto updatedLocation = service.update(externalId, dto);
            return ResponseEntity.ok(ApiResponse.<LocationDto>builder()
                    .success(true)
                    .message("Location updated successfully")
                    .data(updatedLocation)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update location with externalId {}: {}", externalId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<LocationDto>builder()
                    .success(false)
                    .message("Failed to update location: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{externalId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String externalId) {
        try {
            service.delete(externalId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Location deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete location with externalId {}: {}", externalId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete location: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationDto>>> getAllLocations() {
        try {
            ApiResponse<List<LocationDto>> response = service.getAllLocations();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve all locations: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<List<LocationDto>>builder()
                    .success(false)
                    .message("Failed to retrieve locations: " + e.getMessage())
                    .build());
        }
    }
}