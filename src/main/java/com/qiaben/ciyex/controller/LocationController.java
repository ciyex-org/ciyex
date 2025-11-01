package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.LocationDto;
import com.qiaben.ciyex.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<ApiResponse<LocationDto>> create(@RequestBody LocationDto dto) {
        try {
            log.info("Creating new location: {}", dto.getName());
            LocationDto created = locationService.create(dto);
            return ResponseEntity.ok(ApiResponse.<LocationDto>builder()
                    .success(true)
                    .message("Location created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Error creating location: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<LocationDto>builder()
                    .success(false)
                    .message("Failed to create location: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationDto>> getById(@PathVariable Long id) {
        try {
            log.info("Fetching location with id: {}", id);
            LocationDto dto = locationService.getById(id);
            return ResponseEntity.ok(ApiResponse.<LocationDto>builder()
                    .success(true)
                    .message("Location retrieved successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Error fetching location by id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<LocationDto>builder()
                    .success(false)
                    .message("Failed to retrieve location: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationDto>> update(@PathVariable Long id, @RequestBody LocationDto dto) {
        try {
            log.info("Updating location with id: {}", id);
            LocationDto updated = locationService.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<LocationDto>builder()
                    .success(true)
                    .message("Location updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Error updating location {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<LocationDto>builder()
                    .success(false)
                    .message("Failed to update location: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            log.info("Deleting location with id: {}", id);
            locationService.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Location deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error deleting location {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete location: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<LocationDto>>> getAll(Pageable pageable) {
        try {
            log.info("Fetching all locations with pagination: {}", pageable);
            Page<LocationDto> page = locationService.getAll(pageable);
            return ResponseEntity.ok(ApiResponse.<Page<LocationDto>>builder()
                    .success(true)
                    .message("Locations retrieved successfully")
                    .data(page)
                    .build());
        } catch (Exception e) {
            log.error("Error fetching all locations: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<Page<LocationDto>>builder()
                    .success(false)
                    .message("Failed to retrieve locations: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<LocationDto>>> search(@RequestParam String keyword, Pageable pageable) {
        try {
            log.info("Searching locations with keyword: {}", keyword);
            Page<LocationDto> results = locationService.search(keyword, pageable);
            return ResponseEntity.ok(ApiResponse.<Page<LocationDto>>builder()
                    .success(true)
                    .message("Search results retrieved successfully")
                    .data(results)
                    .build());
        } catch (Exception e) {
            log.error("Error searching locations with keyword '{}': {}", keyword, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<Page<LocationDto>>builder()
                    .success(false)
                    .message("Failed to search locations: " + e.getMessage())
                    .build());
        }
    }
}
 