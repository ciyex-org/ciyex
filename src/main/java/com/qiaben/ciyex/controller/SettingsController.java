package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PracticeDto;
import com.qiaben.ciyex.service.PracticeService;
import com.qiaben.ciyex.service.KeycloakAdminService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Settings Controller
 * 
 * Handles practice and regional settings endpoints
 * Persists tokenExpiryMinutes (session timeout) for each practice
 */
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Slf4j
public class SettingsController {

    private final PracticeService practiceService;
    private final KeycloakAdminService keycloakAdminService;

    /**
     * GET /settings/practice/settings
     * Returns current practice settings including sessionTimeoutMinutes
     */
    @GetMapping("/practice/settings")
    public ResponseEntity<ApiResponse<PracticeSettingsResponse>> getPracticeSettings() {
        try {
            log.info("Fetching practice settings");
            
            // Get all practices and use first one
            ApiResponse<List<PracticeDto>> practicesResponse = practiceService.getAllPractices();
            
            if (practicesResponse.getData() == null || practicesResponse.getData().isEmpty()) {
                log.warn("No practice found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PracticeSettingsResponse>builder()
                        .success(false)
                        .message("No practice found")
                        .data(null)
                        .build());
            }

            PracticeDto practice = practicesResponse.getData().get(0);
            
            PracticeSettingsResponse response = new PracticeSettingsResponse();
            response.setPracticeName(practice.getName());
            response.setEnablePatientPractice(practice.getPracticeSettings() != null ? 
                practice.getPracticeSettings().getEnablePatientPractice() : true);
            
            // Return tokenExpiryMinutes as sessionTimeoutMinutes for backward compatibility
            response.setSessionTimeoutMinutes(practice.getTokenExpiryMinutes() != null ? 
                practice.getTokenExpiryMinutes() : 5);
            
            log.info("✅ Practice settings retrieved: timeout={}m", response.getSessionTimeoutMinutes());

            return ResponseEntity.ok(
                ApiResponse.<PracticeSettingsResponse>builder()
                    .success(true)
                    .message("Practice settings retrieved successfully")
                    .data(response)
                    .build()
            );
        } catch (Exception e) {
            log.error("❌ Error retrieving practice settings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<PracticeSettingsResponse>builder()
                    .success(false)
                    .message("Failed to retrieve practice settings")
                    .data(null)
                    .build());
        }
    }

    /**
     * POST /settings/practice/settings
     * Updates practice settings including sessionTimeoutMinutes
     */
    @PostMapping("/practice/settings")
    public ResponseEntity<ApiResponse<String>> updatePracticeSettings(
            @RequestBody PracticeSettingsRequest request) {
        try {
            log.info("Updating practice settings: timeout={}m", request.getSessionTimeoutMinutes());
            
            // Get all practices and use first one
            ApiResponse<List<PracticeDto>> practicesResponse = practiceService.getAllPractices();
            
            if (practicesResponse.getData() == null || practicesResponse.getData().isEmpty()) {
                log.warn("No practice found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<String>builder()
                        .success(false)
                        .message("No practice found")
                        .data(null)
                        .build());
            }

            PracticeDto practice = practicesResponse.getData().get(0);
            
            // Update practice name
            if (request.getPracticeName() != null) {
                practice.setName(request.getPracticeName());
            }
            
            // Update practice settings
            if (practice.getPracticeSettings() == null) {
                practice.setPracticeSettings(new PracticeDto.PracticeSettings());
            }
            
            if (request.getEnablePatientPractice() != null) {
                practice.getPracticeSettings().setEnablePatientPractice(request.getEnablePatientPractice());
            }
            
            // Update session timeout (tokenExpiryMinutes)
            if (request.getSessionTimeoutMinutes() != null) {
                Integer timeout = request.getSessionTimeoutMinutes();
                
                // Validate range (5-30 minutes)
                if (timeout < 5 || timeout > 30) {
                    log.warn("Invalid timeout {}, must be 5-30", timeout);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.<String>builder()
                            .success(false)
                            .message("Session timeout must be between 5 and 30 minutes")
                            .data(null)
                            .build());
                }
                
                practice.setTokenExpiryMinutes(timeout);
                log.info("✅ Session timeout set to {} minutes", timeout);
                
                // Update Keycloak realm settings in real-time
                try {
                    boolean keycloakUpdated = keycloakAdminService.updateClientTokenLifespan(timeout);
                    if (keycloakUpdated) {
                        log.info("✅ Keycloak token lifespan updated to {} minutes", timeout);
                    } else {
                        log.warn("⚠️ Failed to update Keycloak token lifespan, but practice setting saved");
                    }
                } catch (Exception e) {
                    log.error("❌ Error updating Keycloak token lifespan: {}", e.getMessage());
                    // Continue with practice update even if Keycloak update fails
                }
            }

            // Save updated practice
            PracticeDto updated = practiceService.update(practice.getId(), practice);

            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .message("Practice settings updated successfully")
                    .data("Settings saved")
                    .build()
            );
        } catch (Exception e) {
            log.error("❌ Error updating practice settings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to update practice settings")
                    .data(null)
                    .build());
        }
    }

    /**
     * GET /settings/regional/settings
     * Returns regional formatting settings
     */
    @GetMapping("/regional/settings")
    public ResponseEntity<ApiResponse<RegionalSettingsResponse>> getRegionalSettings() {
        try {
            log.info("Fetching regional settings");
            
            // Get all practices and use first one
            ApiResponse<List<PracticeDto>> practicesResponse = practiceService.getAllPractices();
            
            if (practicesResponse.getData() == null || practicesResponse.getData().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<RegionalSettingsResponse>builder()
                        .success(false)
                        .message("No practice found")
                        .data(null)
                        .build());
            }

            PracticeDto practice = practicesResponse.getData().get(0);
            PracticeDto.RegionalSettings regional = practice.getRegionalSettings();
            
            RegionalSettingsResponse response = new RegionalSettingsResponse();
            if (regional != null) {
                response.setUnitsForVisitForms(regional.getUnitsForVisitForms());
                response.setDisplayFormatUSWeights(regional.getDisplayFormatUSWeights());
                response.setTelephoneCountryCode(regional.getTelephoneCountryCode());
                response.setDateDisplayFormat(regional.getDateDisplayFormat());
                response.setTimeDisplayFormat(regional.getTimeDisplayFormat());
                response.setTimeZone(regional.getTimeZone());
                response.setCurrencyDesignator(regional.getCurrencyDesignator());
            }

            return ResponseEntity.ok(
                ApiResponse.<RegionalSettingsResponse>builder()
                    .success(true)
                    .message("Regional settings retrieved successfully")
                    .data(response)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error retrieving regional settings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<RegionalSettingsResponse>builder()
                    .success(false)
                    .message("Failed to retrieve regional settings")
                    .data(null)
                    .build());
        }
    }

    /**
     * PUT /settings/regional/settings
     * Updates regional formatting settings
     */
    @PutMapping("/regional/settings")
    public ResponseEntity<ApiResponse<String>> updateRegionalSettings(
            @RequestBody RegionalSettingsRequest request) {
        try {
            log.info("Updating regional settings");
            
            // Get all practices and use first one
            ApiResponse<List<PracticeDto>> practicesResponse = practiceService.getAllPractices();
            
            if (practicesResponse.getData() == null || practicesResponse.getData().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<String>builder()
                        .success(false)
                        .message("No practice found")
                        .data(null)
                        .build());
            }

            PracticeDto practice = practicesResponse.getData().get(0);
            
            // Create or update regional settings
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

            // Save updated practice
            practiceService.update(practice.getId(), practice);

            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .message("Regional settings updated successfully")
                    .data("Settings saved")
                    .build()
            );
        } catch (Exception e) {
            log.error("Error updating regional settings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to update regional settings")
                    .data(null)
                    .build());
        }
    }

    /**
     * POST /settings/token-expiry
     * Updates token expiry for the organization and applies to Keycloak in real-time
     */
    @PostMapping("/token-expiry")
    public ResponseEntity<ApiResponse<TokenExpiryResponse>> updateTokenExpiry(
            @RequestBody TokenExpiryRequest request) {
        try {
            log.info("Updating token expiry to {} minutes", request.getMinutes());
            
            // Validate range (5-30 minutes)
            if (request.getMinutes() < 5 || request.getMinutes() > 30) {
                log.warn("Invalid token expiry {}, must be 5-30", request.getMinutes());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<TokenExpiryResponse>builder()
                        .success(false)
                        .message("Token expiry must be between 5 and 30 minutes")
                        .data(null)
                        .build());
            }
            
            // Get first practice
            ApiResponse<List<PracticeDto>> practicesResponse = practiceService.getAllPractices();
            if (practicesResponse.getData() == null || practicesResponse.getData().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<TokenExpiryResponse>builder()
                        .success(false)
                        .message("No practice found")
                        .data(null)
                        .build());
            }
            
            PracticeDto practice = practicesResponse.getData().get(0);
            practice.setTokenExpiryMinutes(request.getMinutes());
            
            // Update Keycloak first
            boolean keycloakUpdated = false;
            String keycloakMessage = "";
            try {
                keycloakUpdated = keycloakAdminService.updateClientTokenLifespan(request.getMinutes());
                if (keycloakUpdated) {
                    keycloakMessage = "Keycloak updated successfully";
                    log.info("✅ Keycloak token lifespan updated to {} minutes", request.getMinutes());
                } else {
                    keycloakMessage = "Keycloak update failed";
                    log.warn("⚠️ Failed to update Keycloak token lifespan");
                }
            } catch (Exception e) {
                keycloakMessage = "Keycloak update error: " + e.getMessage();
                log.error("❌ Error updating Keycloak: {}", e.getMessage());
            }
            
            // Update practice settings
            practiceService.update(practice.getId(), practice);
            log.info("✅ Practice token expiry updated to {} minutes", request.getMinutes());
            
            TokenExpiryResponse response = new TokenExpiryResponse();
            response.setMinutes(request.getMinutes());
            response.setKeycloakUpdated(keycloakUpdated);
            response.setKeycloakMessage(keycloakMessage);
            
            return ResponseEntity.ok(
                ApiResponse.<TokenExpiryResponse>builder()
                    .success(true)
                    .message("Token expiry updated successfully")
                    .data(response)
                    .build()
            );
        } catch (Exception e) {
            log.error("❌ Error updating token expiry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<TokenExpiryResponse>builder()
                    .success(false)
                    .message("Failed to update token expiry: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    /**
     * GET /settings/token-expiry
     * Gets current token expiry setting
     */
    @GetMapping("/token-expiry")
    public ResponseEntity<ApiResponse<TokenExpiryResponse>> getTokenExpiry() {
        try {
            log.info("Fetching current token expiry setting");
            
            ApiResponse<List<PracticeDto>> practicesResponse = practiceService.getAllPractices();
            if (practicesResponse.getData() == null || practicesResponse.getData().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<TokenExpiryResponse>builder()
                        .success(false)
                        .message("No practice found")
                        .data(null)
                        .build());
            }
            
            PracticeDto practice = practicesResponse.getData().get(0);
            Integer currentExpiry = practice.getTokenExpiryMinutes() != null ? 
                practice.getTokenExpiryMinutes() : 5;
            
            TokenExpiryResponse response = new TokenExpiryResponse();
            response.setMinutes(currentExpiry);
            response.setKeycloakUpdated(true); // Assume it's in sync
            response.setKeycloakMessage("Current setting");
            
            return ResponseEntity.ok(
                ApiResponse.<TokenExpiryResponse>builder()
                    .success(true)
                    .message("Token expiry retrieved successfully")
                    .data(response)
                    .build()
            );
        } catch (Exception e) {
            log.error("❌ Error fetching token expiry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<TokenExpiryResponse>builder()
                    .success(false)
                    .message("Failed to fetch token expiry")
                    .data(null)
                    .build());
        }
    }

    /**
     * Request/Response DTOs
     */
    @Data
    public static class PracticeSettingsRequest {
        private String practiceName;
        private Boolean enablePatientPractice;
        private Integer sessionTimeoutMinutes; // 5-30 minutes
    }

    @Data
    public static class PracticeSettingsResponse {
        private String practiceName;
        private Boolean enablePatientPractice;
        private Integer sessionTimeoutMinutes; // Maps to tokenExpiryMinutes
    }

    @Data
    public static class RegionalSettingsRequest {
        private String unitsForVisitForms;
        private String displayFormatUSWeights;
        private String telephoneCountryCode;
        private String dateDisplayFormat;
        private String timeDisplayFormat;
        private String timeZone;
        private String currencyDesignator;
    }

    @Data
    public static class RegionalSettingsResponse {
        private String unitsForVisitForms;
        private String displayFormatUSWeights;
        private String telephoneCountryCode;
        private String dateDisplayFormat;
        private String timeDisplayFormat;
        private String timeZone;
        private String currencyDesignator;
    }
    
    @Data
    public static class TokenExpiryRequest {
        private Integer minutes; // 5-30 minutes
    }
    
    @Data
    public static class TokenExpiryResponse {
        private Integer minutes;
        private Boolean keycloakUpdated;
        private String keycloakMessage;
    }
}
