package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PracticeDto;
import com.qiaben.ciyex.service.KeycloakAdminService;
import com.qiaben.ciyex.service.PracticeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@Slf4j
public class SettingsController {

    private final PracticeService practiceService;
    private final KeycloakAdminService keycloakAdminService;

    @Value("${ciyex.env:local}")
    private String environment;

    @Value("${keycloak.enabled:true}")
    private boolean keycloakEnabled;

    public SettingsController(PracticeService practiceService, 
                            @Autowired(required = false) KeycloakAdminService keycloakAdminService) {
        this.practiceService = practiceService;
        this.keycloakAdminService = keycloakAdminService;
    }

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
            }

            // Save updated practice
            PracticeDto updated = practiceService.update(practice.getId(), practice);

            // Update Keycloak if enabled and timeout changed
            if (keycloakEnabled && keycloakAdminService != null && request.getSessionTimeoutMinutes() != null) {
                Integer timeoutValue = request.getSessionTimeoutMinutes();
                try {
                    keycloakAdminService.updateClientTokenLifespan(timeoutValue);
                    log.info("[{}] ✅ Keycloak updated with {}m timeout", environment, timeoutValue);
                } catch (Exception e) {
                    log.warn("[{}] ⚠️ Keycloak update failed: {}", environment, e.getMessage());
                }
            }

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

    /**
     * GET /settings/token-expiry
     * Returns current token expiry settings with environment info
     */
    @GetMapping("/token-expiry")
    public ResponseEntity<ApiResponse<TokenExpiryResponse>> getTokenExpiry() {
        try {
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
            Integer minutes = practice.getTokenExpiryMinutes() != null ? practice.getTokenExpiryMinutes() : 5;

            TokenExpiryResponse response = new TokenExpiryResponse();
            response.setMinutes(minutes);
            response.setEnvironment(environment);
            response.setKeycloakEnabled(keycloakEnabled);
            response.setKeycloakUpdated(true);
            response.setKeycloakMessage(keycloakEnabled ? "Connected" : "Disabled");

            return ResponseEntity.ok(
                ApiResponse.<TokenExpiryResponse>builder()
                    .success(true)
                    .message("Token expiry settings retrieved")
                    .data(response)
                    .build()
            );
        } catch (Exception e) {
            log.error("[{}] ❌ Error getting token expiry", environment, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<TokenExpiryResponse>builder()
                    .success(false)
                    .message("Failed to get token expiry settings")
                    .data(null)
                    .build());
        }
    }

    /**
     * POST /settings/token-expiry
     * Updates token expiry settings with Keycloak integration
     */
    @PostMapping("/token-expiry")
    public ResponseEntity<ApiResponse<TokenExpiryResponse>> updateTokenExpiry(
            @RequestBody TokenExpiryRequest request) {
        try {
            // Validate range
            if (request.getMinutes() < 5 || request.getMinutes() > 30) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<TokenExpiryResponse>builder()
                        .success(false)
                        .message("Token expiry must be between 5 and 30 minutes")
                        .data(null)
                        .build());
            }

            // Get practice
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

            // Save to database
            practiceService.update(practice.getId(), practice);

            // Update Keycloak if enabled
            boolean keycloakUpdated = false;
            String keycloakMessage = "Disabled";
            
            if (keycloakEnabled && keycloakAdminService != null) {
                try {
                    keycloakUpdated = keycloakAdminService.updateClientTokenLifespan(request.getMinutes());
                    keycloakMessage = keycloakUpdated ? "Updated successfully" : "Update failed";
                } catch (Exception e) {
                    keycloakMessage = "Connection failed";
                }
            }

            TokenExpiryResponse response = new TokenExpiryResponse();
            response.setMinutes(request.getMinutes());
            response.setEnvironment(environment);
            response.setKeycloakEnabled(keycloakEnabled);
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
            log.error("[{}] ❌ Error updating token expiry", environment, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<TokenExpiryResponse>builder()
                    .success(false)
                    .message("Failed to update token expiry")
                    .data(null)
                    .build());
        }
    }

    @Data
    public static class TokenExpiryRequest {
        private Integer minutes;
    }

    @Data
    public static class TokenExpiryResponse {
        private Integer minutes;
        private String environment;
        private boolean keycloakEnabled;
        private boolean keycloakUpdated;
        private String keycloakMessage;
    }
}
