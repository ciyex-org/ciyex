package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.GlobalEncounterSaveDto;
import org.ciyex.ehr.service.GlobalEncounterSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for global save functionality.
 * Handles saving multiple encounter-related sections in a single request.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/{patientId}/encounters/{encounterId}/global-save")
public class GlobalEncounterSaveController {

    private final GlobalEncounterSaveService globalEncounterSaveService;

    /**
     * Global save endpoint - Save all populated sections at once
     * Only saves fields that have been filled in (non-null sections)
     * 
     * @param patientId Patient ID
     * @param encounterId Encounter ID
     * @param globalSaveDto DTO containing populated sections
     * @return ApiResponse with status of each saved section
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> globalSave(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody GlobalEncounterSaveDto globalSaveDto) {

        try {
            log.info("Global save request received for patientId: {}, encounterId: {}", patientId, encounterId);
            log.debug("Populated sections: {}", globalSaveDto.getPopulatedSections().keySet());

            ApiResponse<Map<String, Object>> response = globalEncounterSaveService.globalSave(
                    patientId, encounterId, globalSaveDto
            );

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (IllegalArgumentException ex) {
            log.error("Validation error in global save: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message(ex.getMessage())
                            .build());
        } catch (Exception ex) {
            log.error("Error in global save endpoint", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Error during global save: " + ex.getMessage())
                            .build());
        }
    }

    /**
     * Helper endpoint to check which sections are populated in a request
     * Useful for frontend debugging
     * 
     * @param patientId Patient ID
     * @param encounterId Encounter ID
     * @param globalSaveDto DTO containing sections to validate
     * @return Map of populated sections
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateSections(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody GlobalEncounterSaveDto globalSaveDto) {

        try {
            Map<String, Object> populatedSections = globalSaveDto.getPopulatedSections();

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message(String.format("Validation successful. %d section(s) populated.", populatedSections.size()))
                    .data(populatedSections)
                    .build());

        } catch (Exception ex) {
            log.error("Error validating sections", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Error validating sections: " + ex.getMessage())
                            .build());
        }
    }
}
