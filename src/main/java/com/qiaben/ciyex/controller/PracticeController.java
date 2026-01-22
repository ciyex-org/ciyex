package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PracticeDto;
import com.qiaben.ciyex.service.PracticeService;
import com.qiaben.ciyex.service.KeycloakAdminService;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/practices")
public class PracticeController {

    private final PracticeService service;
    private final KeycloakAdminService keycloakAdminService;

    public PracticeController(PracticeService service, KeycloakAdminService keycloakAdminService) {
        this.service = service;
        this.keycloakAdminService = keycloakAdminService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PracticeDto>> create(@RequestBody PracticeDto dto) {
        try {
            PracticeDto createdPractice = service.create(dto);
            return ResponseEntity.ok(
                    ApiResponse.<PracticeDto>builder()
                            .success(true)
                            .message("Practice created successfully")
                            .data(createdPractice)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<PracticeDto>builder()
                            .success(false)
                            .message("Validation failed: " + e.getMessage())
                            .data(null)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PracticeDto>builder()
                            .success(false)
                            .message("Failed to create practice")
                            .data(null)
                            .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PracticeDto>> get(@PathVariable Long id) {
        try {
            PracticeDto practice = service.getById(id);
            if (practice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<PracticeDto>builder()
                                .success(false)
                                .message("Practice not found")
                                .data(null)
                                .build());
            }
            return ResponseEntity.ok(
                    ApiResponse.<PracticeDto>builder()
                            .success(true)
                            .message("Practice retrieved successfully")
                            .data(practice)
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PracticeDto>builder()
                            .success(false)
                            .message(e.getMessage())
                            .data(null)
                            .build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PracticeDto>builder()
                            .success(false)
                            .message("Failed to retrieve practice: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PracticeDto>> update(@PathVariable Long id, @RequestBody PracticeDto dto) {
        try {
            PracticeDto updatedPractice = service.update(id, dto);
            if (updatedPractice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<PracticeDto>builder()
                                .success(false)
                                .message("Practice not found")
                                .data(null)
                                .build());
            }
            return ResponseEntity.ok(
                    ApiResponse.<PracticeDto>builder()
                            .success(true)
                            .message("Practice updated successfully")
                            .data(updatedPractice)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PracticeDto>builder()
                            .success(false)
                            .message("Failed to update practice")
                            .data(null)
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(
                    ApiResponse.<Void>builder()
                            .success(true)
                            .message("Practice deleted successfully")
                            .data(null)
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Practice not found")
                            .data(null)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to delete practice")
                            .data(null)
                            .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PracticeDto>>> getAllPractices() {
        try {
            ApiResponse<List<PracticeDto>> response = service.getAllPractices();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<PracticeDto>>builder()
                            .success(false)
                            .message("Failed to retrieve practices")
                            .data(null)
                            .build());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getPracticeCount() {
        try {
            long count = service.getPracticeCount();
            return ResponseEntity.ok(
                    ApiResponse.<Long>builder()
                            .success(true)
                            .message("Practice count retrieved successfully")
                            .data(count)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Long>builder()
                            .success(false)
                            .message("Failed to retrieve practice count")
                            .data(null)
                            .build());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PracticeDto>>> searchPracticesByName(@RequestParam String name) {
        try {
            ApiResponse<List<PracticeDto>> response = service.getPracticesByName(name);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<PracticeDto>>builder()
                            .success(false)
                            .message("Failed to search practices")
                            .data(null)
                            .build());
        }
    }

    @GetMapping("/{identifier}/practice-settings")
    public ResponseEntity<ApiResponse<PracticeDto.PracticeSettings>> getPracticeSettings(@PathVariable String identifier) {
        try {
            PracticeDto practice = null;

            // Try to parse as Long ID first, then fallback to name search
            try {
                Long id = Long.parseLong(identifier);
                practice = service.getById(id);
            } catch (NumberFormatException e) {
                // If not a number, search by name
                List<PracticeDto> practices = service.getPracticesByName(identifier).getData();
                if (practices != null && !practices.isEmpty()) {
                    practice = practices.get(0);
                }
            }

            if (practice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<PracticeDto.PracticeSettings>builder()
                                .success(false)
                                .message("Practice not found")
                                .data(null)
                                .build());
            }

            PracticeDto.PracticeSettings settings = practice.getPracticeSettings();
            if (settings == null) {
                settings = new PracticeDto.PracticeSettings();
            }

            return ResponseEntity.ok(
                    ApiResponse.<PracticeDto.PracticeSettings>builder()
                            .success(true)
                            .message("Practice settings retrieved successfully")
                            .data(settings)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PracticeDto.PracticeSettings>builder()
                            .success(false)
                            .message("Failed to retrieve practice settings")
                            .data(null)
                            .build());
        }
    }

    @PutMapping("/{id}/practice-settings")
    public ResponseEntity<ApiResponse<PracticeDto>> updatePracticeSettings(
            @PathVariable Long id,
            @RequestBody PracticeSettingsUpdateRequest request
    ) {
        try {
            PracticeDto practice = service.getById(id);
            if (practice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<PracticeDto>builder()
                                .success(false)
                                .message("Practice not found")
                                .data(null)
                                .build());
            }

            // Update practice settings
            if (practice.getPracticeSettings() == null) {
                practice.setPracticeSettings(new PracticeDto.PracticeSettings());
            }
            practice.getPracticeSettings().setEnablePatientPractice(request.getEnablePatientPractice());
            if (request.getSessionTimeoutMinutes() != null) {
                practice.getPracticeSettings().setSessionTimeoutMinutes(request.getSessionTimeoutMinutes());
            }
            if (request.getTokenExpiryMinutes() != null) {
                practice.getPracticeSettings().setTokenExpiryMinutes(request.getTokenExpiryMinutes());
            }

            PracticeDto updatedPractice = service.update(id, practice);
            
            // Update Keycloak token lifespan dynamically
            if (request.getTokenExpiryMinutes() != null) {
                keycloakAdminService.updateClientTokenLifespan(request.getTokenExpiryMinutes());
            }
            
            return ResponseEntity.ok(
                    ApiResponse.<PracticeDto>builder()
                            .success(true)
                            .message("Practice settings updated successfully")
                            .data(updatedPractice)
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PracticeDto>builder()
                            .success(false)
                            .message("Practice not found")
                            .data(null)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PracticeDto>builder()
                            .success(false)
                            .message("Failed to update practice settings")
                            .data(null)
                            .build());
        }
    }

    @GetMapping("/{identifier}/regional-settings")
    public ResponseEntity<ApiResponse<PracticeDto.RegionalSettings>> getRegionalSettings(@PathVariable String identifier) {
        try {
            PracticeDto practice = null;

            // Try to parse as Long ID first, then fallback to name search
            try {
                Long id = Long.parseLong(identifier);
                practice = service.getById(id);
            } catch (NumberFormatException e) {
                // If not a number, search by name
                List<PracticeDto> practices = service.getPracticesByName(identifier).getData();
                if (practices != null && !practices.isEmpty()) {
                    practice = practices.get(0);
                }
            }

            if (practice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<PracticeDto.RegionalSettings>builder()
                                .success(false)
                                .message("Practice not found")
                                .data(null)
                                .build());
            }

            PracticeDto.RegionalSettings settings = practice.getRegionalSettings();
            if (settings == null) {
                settings = new PracticeDto.RegionalSettings();
            }

            return ResponseEntity.ok(
                    ApiResponse.<PracticeDto.RegionalSettings>builder()
                            .success(true)
                            .message("Regional settings retrieved successfully")
                            .data(settings)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PracticeDto.RegionalSettings>builder()
                            .success(false)
                            .message("Failed to retrieve regional settings")
                            .data(null)
                            .build());
        }
    }

    @PutMapping("/{id}/regional-settings")
    public ResponseEntity<ApiResponse<PracticeDto>> updateRegionalSettings(
            @PathVariable Long id,
            @RequestBody RegionalSettingsUpdateRequest request
    ) {
        try {
            PracticeDto practice = service.getById(id);
            if (practice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<PracticeDto>builder()
                                .success(false)
                                .message("Practice not found")
                                .data(null)
                                .build());
            }

            // Update regional settings
            if (practice.getRegionalSettings() == null) {
                practice.setRegionalSettings(new PracticeDto.RegionalSettings());
            }

            PracticeDto.RegionalSettings regional = practice.getRegionalSettings();
            if (request.getUnitsForVisitForms() != null) {
                regional.setUnitsForVisitForms(request.getUnitsForVisitForms());
            }
            if (request.getDisplayFormatUSWeights() != null) {
                regional.setDisplayFormatUSWeights(request.getDisplayFormatUSWeights());
            }
            if (request.getTelephoneCountryCode() != null) {
                regional.setTelephoneCountryCode(request.getTelephoneCountryCode());
            }
            if (request.getDateDisplayFormat() != null) {
                regional.setDateDisplayFormat(request.getDateDisplayFormat());
            }
            if (request.getTimeDisplayFormat() != null) {
                regional.setTimeDisplayFormat(request.getTimeDisplayFormat());
            }
            if (request.getTimeZone() != null) {
                regional.setTimeZone(request.getTimeZone());
            }
            if (request.getCurrencyDesignator() != null) {
                regional.setCurrencyDesignator(request.getCurrencyDesignator());
            }

            PracticeDto updatedPractice = service.update(id, practice);
            return ResponseEntity.ok(
                    ApiResponse.<PracticeDto>builder()
                            .success(true)
                            .message("Regional settings updated successfully")
                            .data(updatedPractice)
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PracticeDto>builder()
                            .success(false)
                            .message("Practice not found")
                            .data(null)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PracticeDto>builder()
                            .success(false)
                            .message("Failed to update regional settings")
                            .data(null)
                            .build());
        }
    }

    // Global exception handler for the controller
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<PracticeDto>> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.<PracticeDto>builder()
                        .success(false)
                        .message(ex.getReason())
                        .data(null)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<PracticeDto>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<PracticeDto>builder()
                        .success(false)
                        .message("An unexpected error occurred")
                        .data(null)
                        .build());
    }

    // Request DTOs for specific updates
    @Data
    public static class PracticeSettingsUpdateRequest {
        private Boolean enablePatientPractice;
        private Integer sessionTimeoutMinutes;
        private Integer tokenExpiryMinutes;
    }

    @Data
    public static class RegionalSettingsUpdateRequest {
        private String unitsForVisitForms;
        private String displayFormatUSWeights;
        private String telephoneCountryCode;
        private String dateDisplayFormat;
        private String timeDisplayFormat;
        private String timeZone;
        private String currencyDesignator;
    }
}