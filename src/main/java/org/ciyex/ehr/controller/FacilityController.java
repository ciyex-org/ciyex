package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.FacilityDto;
import org.ciyex.ehr.service.FacilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
@Slf4j
public class FacilityController {

    private final FacilityService facilityService;

    /**
     * Create a new facility
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FacilityDto>> createFacility(@RequestBody FacilityDto dto) {
        try {
            log.info("Creating facility: {}", dto.getName());
            FacilityDto created = facilityService.create(dto);
            return ResponseEntity.ok(
                    ApiResponse.<FacilityDto>builder()
                            .success(true)
                            .message("Facility created successfully")
                            .data(created)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to create facility: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<FacilityDto>builder()
                            .success(false)
                            .message("Failed to create facility: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get facility by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FacilityDto>> getFacilityById(@PathVariable Long id) {
        try {
            log.info("Fetching facility with id: {}", id);
            FacilityDto facility = facilityService.getById(id);
            return ResponseEntity.ok(
                    ApiResponse.<FacilityDto>builder()
                            .success(true)
                            .message("Facility retrieved successfully")
                            .data(facility)
                            .build()
            );
        } catch (RuntimeException e) {
            log.error("Facility not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<FacilityDto>builder()
                            .success(false)
                            .message("Facility not found")
                            .build());
        } catch (Exception e) {
            log.error("Failed to retrieve facility: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<FacilityDto>builder()
                            .success(false)
                            .message("Failed to retrieve facility: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Update facility
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FacilityDto>> updateFacility(
            @PathVariable Long id,
            @RequestBody FacilityDto dto) {
        try {
            log.info("Updating facility with id: {}", id);
            FacilityDto updated = facilityService.update(id, dto);
            return ResponseEntity.ok(
                    ApiResponse.<FacilityDto>builder()
                            .success(true)
                            .message("Facility updated successfully")
                            .data(updated)
                            .build()
            );
        } catch (RuntimeException e) {
            log.error("Facility not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<FacilityDto>builder()
                            .success(false)
                            .message("Facility not found")
                            .build());
        } catch (Exception e) {
            log.error("Failed to update facility: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<FacilityDto>builder()
                            .success(false)
                            .message("Failed to update facility: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Delete facility (hard delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFacility(@PathVariable Long id) {
        try {
            log.info("Deleting facility with id: {}", id);
            facilityService.delete(id);
            return ResponseEntity.ok(
                    ApiResponse.<Void>builder()
                            .success(true)
                            .message("Facility deleted successfully")
                            .build()
            );
        } catch (RuntimeException e) {
            log.error("Facility not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Facility not found")
                            .build());
        } catch (Exception e) {
            log.error("Failed to delete facility: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to delete facility: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Soft delete facility (mark as inactive)
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateFacility(@PathVariable Long id) {
        try {
            log.info("Deactivating facility with id: {}", id);
            facilityService.softDelete(id);
            return ResponseEntity.ok(
                    ApiResponse.<Void>builder()
                            .success(true)
                            .message("Facility deactivated successfully")
                            .build()
            );
        } catch (RuntimeException e) {
            log.error("Facility not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Facility not found")
                            .build());
        } catch (Exception e) {
            log.error("Failed to deactivate facility: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to deactivate facility: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get all facilities
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FacilityDto>>> getAllFacilities() {
        try {
            log.info("Fetching all facilities");
            ApiResponse<List<FacilityDto>> response = facilityService.getAllFacilities();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve facilities: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<FacilityDto>>builder()
                            .success(false)
                            .message("Failed to retrieve facilities: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get active facilities only
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<FacilityDto>>> getActiveFacilities() {
        try {
            log.info("Fetching active facilities");
            ApiResponse<List<FacilityDto>> response = facilityService.getActiveFacilities();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve active facilities: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<FacilityDto>>builder()
                            .success(false)
                            .message("Failed to retrieve active facilities: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get facilities by active status
     */
    @GetMapping("/status/{isActive}")
    public ResponseEntity<ApiResponse<List<FacilityDto>>> getFacilitiesByStatus(
            @PathVariable Boolean isActive) {
        try {
            log.info("Fetching facilities with status: {}", isActive);
            ApiResponse<List<FacilityDto>> response = facilityService.getFacilitiesByStatus(isActive);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve facilities by status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<FacilityDto>>builder()
                            .success(false)
                            .message("Failed to retrieve facilities: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Search facilities by name
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FacilityDto>>> searchFacilitiesByName(
            @RequestParam String name) {
        try {
            log.info("Searching facilities by name: {}", name);
            ApiResponse<List<FacilityDto>> response = facilityService.searchByName(name);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to search facilities: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<FacilityDto>>builder()
                            .success(false)
                            .message("Failed to search facilities: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get paginated facilities
     */
    @GetMapping("/paginated")
    public ResponseEntity<ApiResponse<Page<FacilityDto>>> getPaginatedFacilities(Pageable pageable) {
        try {
            log.info("Fetching paginated facilities");
            Page<FacilityDto> page = facilityService.getAllPaginated(pageable);
            return ResponseEntity.ok(
                    ApiResponse.<Page<FacilityDto>>builder()
                            .success(true)
                            .message("Facilities retrieved successfully")
                            .data(page)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to retrieve paginated facilities: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Page<FacilityDto>>builder()
                            .success(false)
                            .message("Failed to retrieve facilities: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get facility statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<FacilityStatistics>> getFacilityStatistics() {
        try {
            log.info("Fetching facility statistics");
            FacilityStatistics stats = FacilityStatistics.builder()
                    .totalCount(facilityService.getTotalCount())
                    .activeCount(facilityService.getActiveCount())
                    .inactiveCount(facilityService.getInactiveCount())
                    .build();

            return ResponseEntity.ok(
                    ApiResponse.<FacilityStatistics>builder()
                            .success(true)
                            .message("Statistics retrieved successfully")
                            .data(stats)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to retrieve statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<FacilityStatistics>builder()
                            .success(false)
                            .message("Failed to retrieve statistics: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Inner class for statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class FacilityStatistics {
        private long totalCount;
        private long activeCount;
        private long inactiveCount;
    }
}






