package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.fhir.GenericFhirResourceService;
import org.ciyex.ehr.service.KeycloakAdminService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;

/**
 * Settings Controller — uses GenericFhirResourceService (tabKey=practice) for practice data.
 */
@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Slf4j
public class SettingsController {

    private final GenericFhirResourceService fhirService;
    private final KeycloakAdminService keycloakAdminService;

    @GetMapping("/practice/settings")
    public ResponseEntity<ApiResponse<PracticeSettingsResponse>> getPracticeSettings() {
        try {
            Map<String, Object> practice = findFirstPractice();
            if (practice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<PracticeSettingsResponse>builder()
                                .success(false).message("No practice found").data(null).build());
            }

            PracticeSettingsResponse response = new PracticeSettingsResponse();
            response.setPracticeName(str(practice, "name"));
            response.setEnablePatientPractice(bool(practice, "practiceSettings.enablePatientPractice", true));
            response.setSessionTimeoutMinutes(integer(practice, "tokenExpiryMinutes",
                    integer(practice, "practiceSettings.tokenExpiryMinutes", 5)));

            return ResponseEntity.ok(ApiResponse.<PracticeSettingsResponse>builder()
                    .success(true).message("Practice settings retrieved successfully").data(response).build());
        } catch (Exception e) {
            log.error("Error retrieving practice settings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PracticeSettingsResponse>builder()
                            .success(false).message("Failed to retrieve practice settings").data(null).build());
        }
    }

    @PostMapping("/practice/settings")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<String>> updatePracticeSettings(
            @RequestBody PracticeSettingsRequest request) {
        try {
            Map<String, Object> practice = findFirstPractice();
            if (practice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<String>builder().success(false).message("No practice found").data(null).build());
            }

            String practiceId = (String) practice.get("id");

            // Build update map with current data + changes
            Map<String, Object> update = new LinkedHashMap<>(practice);
            if (request.getPracticeName() != null) {
                update.put("name", request.getPracticeName());
            }
            if (request.getEnablePatientPractice() != null) {
                update.put("practiceSettings.enablePatientPractice", request.getEnablePatientPractice());
            }
            if (request.getSessionTimeoutMinutes() != null) {
                Integer timeout = request.getSessionTimeoutMinutes();
                if (timeout < 5 || timeout > 30) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.<String>builder()
                                    .success(false).message("Session timeout must be between 5 and 30 minutes").data(null).build());
                }
                update.put("tokenExpiryMinutes", timeout);
                update.put("practiceSettings.tokenExpiryMinutes", timeout);

                // Update Keycloak token lifespan
                keycloakAdminService.updateClientTokenLifespan(timeout);
            }

            fhirService.update("practice", null, practiceId, update);

            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true).message("Practice settings updated successfully").data("Settings saved").build());
        } catch (Exception e) {
            log.error("Error updating practice settings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<String>builder()
                            .success(false).message("Failed to update practice settings").data(null).build());
        }
    }

    @GetMapping("/regional/settings")
    public ResponseEntity<ApiResponse<RegionalSettingsResponse>> getRegionalSettings() {
        try {
            Map<String, Object> practice = findFirstPractice();
            if (practice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<RegionalSettingsResponse>builder()
                                .success(false).message("No practice found").data(null).build());
            }

            RegionalSettingsResponse response = new RegionalSettingsResponse();
            response.setUnitsForVisitForms(str(practice, "regionalSettings.unitsForVisitForms"));
            response.setDisplayFormatUSWeights(str(practice, "regionalSettings.displayFormatUSWeights"));
            response.setTelephoneCountryCode(str(practice, "regionalSettings.telephoneCountryCode"));
            response.setDateDisplayFormat(str(practice, "regionalSettings.dateDisplayFormat"));
            response.setTimeDisplayFormat(str(practice, "regionalSettings.timeDisplayFormat"));
            response.setTimeZone(str(practice, "regionalSettings.timeZone"));
            response.setCurrencyDesignator(str(practice, "regionalSettings.currencyDesignator"));

            return ResponseEntity.ok(ApiResponse.<RegionalSettingsResponse>builder()
                    .success(true).message("Regional settings retrieved successfully").data(response).build());
        } catch (Exception e) {
            log.error("Error retrieving regional settings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<RegionalSettingsResponse>builder()
                            .success(false).message("Failed to retrieve regional settings").data(null).build());
        }
    }

    @PutMapping("/regional/settings")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<String>> updateRegionalSettings(
            @RequestBody RegionalSettingsRequest request) {
        try {
            Map<String, Object> practice = findFirstPractice();
            if (practice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<String>builder().success(false).message("No practice found").data(null).build());
            }

            String practiceId = (String) practice.get("id");
            Map<String, Object> update = new LinkedHashMap<>(practice);

            if (request.getUnitsForVisitForms() != null) update.put("regionalSettings.unitsForVisitForms", request.getUnitsForVisitForms());
            if (request.getDisplayFormatUSWeights() != null) update.put("regionalSettings.displayFormatUSWeights", request.getDisplayFormatUSWeights());
            if (request.getTelephoneCountryCode() != null) update.put("regionalSettings.telephoneCountryCode", request.getTelephoneCountryCode());
            if (request.getDateDisplayFormat() != null) update.put("regionalSettings.dateDisplayFormat", request.getDateDisplayFormat());
            if (request.getTimeDisplayFormat() != null) update.put("regionalSettings.timeDisplayFormat", request.getTimeDisplayFormat());
            if (request.getTimeZone() != null) update.put("regionalSettings.timeZone", request.getTimeZone());
            if (request.getCurrencyDesignator() != null) update.put("regionalSettings.currencyDesignator", request.getCurrencyDesignator());

            fhirService.update("practice", null, practiceId, update);

            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true).message("Regional settings updated successfully").data("Settings saved").build());
        } catch (Exception e) {
            log.error("Error updating regional settings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<String>builder()
                            .success(false).message("Failed to update regional settings").data(null).build());
        }
    }

    // ---------- Helpers ----------

    @SuppressWarnings("unchecked")
    private Map<String, Object> findFirstPractice() {
        Map<String, Object> result = fhirService.listAll("practice", 0, 1);
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        if (content == null || content.isEmpty()) return null;
        return content.get(0);
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof String s ? s : (val != null ? val.toString() : null);
    }

    private boolean bool(Map<String, Object> map, String key, boolean defaultVal) {
        Object val = map.get(key);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return defaultVal;
    }

    private int integer(Map<String, Object> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return defaultVal; }
        }
        return defaultVal;
    }

    // ---------- Request/Response DTOs ----------

    @Data
    public static class PracticeSettingsRequest {
        private String practiceName;
        private Boolean enablePatientPractice;
        private Integer sessionTimeoutMinutes;
    }

    @Data
    public static class PracticeSettingsResponse {
        private String practiceName;
        private Boolean enablePatientPractice;
        private Integer sessionTimeoutMinutes;
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
}
