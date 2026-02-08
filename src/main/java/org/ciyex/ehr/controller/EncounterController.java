



package org.ciyex.ehr.controller;

import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.EncounterDto;
import org.ciyex.ehr.service.EncounterService;

@RestController
@RequestMapping("/api/{patientId}/encounters")

public class EncounterController {

    private final EncounterService encounterService;

    @Autowired
    public EncounterController(EncounterService encounterService) {
        this.encounterService = encounterService;
    }




    // CREATE
    @PostMapping
    public ResponseEntity<ApiResponse<EncounterDto>> createEncounter(
            @PathVariable Long patientId,
            @RequestBody EncounterDto encounterDto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(encounterDto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message(validationError)
                    .build());
        }

        try {
            encounterDto.setPatientId(patientId);
            EncounterDto created = encounterService.createEncounter(patientId, encounterDto);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter created")
                    .data(created)
                    .build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Error creating Encounter: " + ex.getMessage())
                    .build());
        }
    }

    // LIST for patient
    @GetMapping
    public ResponseEntity<ApiResponse<List<EncounterDto>>> listEncounters(
            @PathVariable Long patientId) {
        try {
            List<EncounterDto> items = encounterService.listByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.<List<EncounterDto>>builder()
                    .success(true)
                    .message("Encounters fetched")
                    .data(items)
                    .build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ApiResponse.<List<EncounterDto>>builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(ApiResponse.<List<EncounterDto>>builder()
                    .success(false)
                    .message("Error fetching encounters: " + ex.getMessage())
                    .build());
        }
    }

    // GET one by id (scoped)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EncounterDto>> getEncounter(
            @PathVariable Long patientId,
            @PathVariable Long id) {
        try {
            EncounterDto dto = encounterService.getByIdForPatient(id, patientId);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter fetched")
                    .data(dto)
                    .build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Error fetching encounter: " + ex.getMessage())
                    .build());
        }
    }

    // UPDATE (scoped)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EncounterDto>> updateEncounter(
            @PathVariable Long patientId,
            @PathVariable Long id,
            @RequestBody EncounterDto encounterDto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(encounterDto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message(validationError)
                    .build());
        }

        try {
            encounterDto.setPatientId(patientId);
            EncounterDto updated = encounterService.updateEncounter(id, patientId, encounterDto);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter updated")
                    .data(updated)
                    .build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Error updating encounter: " + ex.getMessage())
                    .build());
        }
    }

    // DELETE (scoped)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEncounter(
            @PathVariable Long patientId,
            @PathVariable Long id) {
        try {
            encounterService.deleteEncounter(id, patientId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Encounter deleted successfully")
                    .build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423).body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error deleting encounter: " + ex.getMessage())
                    .build());
        }
    }
    @PostMapping("/{id}/sign")
    public ResponseEntity<ApiResponse<EncounterDto>> signEncounter(
            @PathVariable Long patientId,
            @PathVariable Long id) {
        try {
            EncounterDto dto = encounterService.signEncounter(id, patientId);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter signed")
                    .data(dto)
                    .build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
        } catch (Exception ex) {
            return ResponseEntity.status(400).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Error signing encounter: " + ex.getMessage())
                    .build());
        }
    }

    @PostMapping("/{id}/unsign")
    public ResponseEntity<ApiResponse<EncounterDto>> unsignEncounter(
            @PathVariable Long patientId,
            @PathVariable Long id) {
        try {
            EncounterDto dto = encounterService.unsignEncounter(id, patientId);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter unsigned")
                    .data(dto)
                    .build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
        } catch (Exception ex) {
            return ResponseEntity.status(400).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Error unsigning encounter: " + ex.getMessage())
                    .build());
        }
    }

    @PostMapping("/{id}/incomplete")
    public ResponseEntity<ApiResponse<EncounterDto>> markIncomplete(
            @PathVariable Long patientId,
            @PathVariable Long id) {
        try {
            EncounterDto dto = encounterService.markIncomplete(id, patientId);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter marked incomplete")
                    .data(dto)
                    .build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
        } catch (Exception ex) {
            return ResponseEntity.status(400).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Error marking encounter incomplete: " + ex.getMessage())
                    .build());
        }
    }

    /**
     * Validates mandatory fields for Encounter creation and update
     * @param dto EncounterDto to validate
     * @return error message if validation fails, null if validation passes
     */
    private String validateMandatoryFields(EncounterDto dto) {
        StringBuilder missingFields = new StringBuilder();

        if (dto.getVisitCategory() == null || dto.getVisitCategory().trim().isEmpty()) {
            missingFields.append("visitCategory, ");
        }

        if (dto.getEncounterDate() == null) {
            missingFields.append("encounterDate, ");
        }

        if (dto.getEncounterProvider() == null || dto.getEncounterProvider().trim().isEmpty()) {
            missingFields.append("encounterProvider, ");
        }

        if (!missingFields.isEmpty()) {
            // Remove the trailing comma and space
            missingFields.setLength(missingFields.length() - 2);
            return "Missing mandatory fields: " + missingFields;
        }

        return null;
    }



}



