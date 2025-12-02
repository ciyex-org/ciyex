





package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.DateTimeFinalizedDto;
import com.qiaben.ciyex.service.DateTimeFinalizedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/date-time-finalized")
@RequiredArgsConstructor
@Slf4j
public class DateTimeFinalizedController {
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<DateTimeFinalizedDto>>> getAllByPatient(@PathVariable Long patientId) {
        try {
            var items = service.getAllByPatient(patientId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<DateTimeFinalizedDto>>builder()
                        .success(true)
                        .message("No Date/Time Finalized records found for Patient ID: " + patientId)
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<DateTimeFinalizedDto>>builder()
                    .success(true)
                    .message("Date/Time Finalized records fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Date/Time Finalized for Patient ID: " + patientId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<DateTimeFinalizedDto>>builder()
                            .success(false)
                            .message("Error fetching Date/Time Finalized for Patient ID: " + patientId + ". " + ex.getMessage())
                            .build());
        }
    }

    private final DateTimeFinalizedService service;

    // LIST
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<DateTimeFinalizedDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        try {
            var items = service.list(patientId, encounterId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<DateTimeFinalizedDto>>builder()
                        .success(true)
                        .message(String.format("No Date/Time Finalized records found for Patient ID: %d, Encounter ID: %d", patientId, encounterId))
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<DateTimeFinalizedDto>>builder()
                    .success(true)
                    .message("Finalizations fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Date/Time Finalized for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<DateTimeFinalizedDto>>builder()
                            .success(false)
                            .message(String.format("Error fetching Date/Time Finalized for Patient ID: %d, Encounter ID: %d. %s", patientId, encounterId, ex.getMessage()))
                            .build());
        }
    }

    // GET
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<DateTimeFinalizedDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            var dto = service.getOne(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<DateTimeFinalizedDto>builder()
                    .success(true).message("Finalization fetched successfully").data(dto).build());
        } catch (IllegalArgumentException ex) {
            log.error("Date/Time Finalized not found: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<DateTimeFinalizedDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error fetching Date/Time Finalized for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<DateTimeFinalizedDto>builder()
                            .success(false)
                            .message("Error fetching Date/Time Finalized: " + ex.getMessage())
                            .build());
        }
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<DateTimeFinalizedDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody DateTimeFinalizedDto dto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(dto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<DateTimeFinalizedDto>builder()
                    .success(false).message(validationError).build());
        }

        try {
            var saved = service.create(patientId, encounterId, dto);
            return ResponseEntity.ok(ApiResponse.<DateTimeFinalizedDto>builder()
                    .success(true).message("Finalization created").data(saved).build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error during Date/Time Finalized creation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<DateTimeFinalizedDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            log.error("Business rule violation during Date/Time Finalized creation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(ApiResponse.<DateTimeFinalizedDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error creating Date/Time Finalized for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<DateTimeFinalizedDto>builder()
                            .success(false)
                            .message("Error creating Date/Time Finalized: " + ex.getMessage())
                            .build());
        }
    }

    // UPDATE (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<DateTimeFinalizedDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody DateTimeFinalizedDto dto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(dto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<DateTimeFinalizedDto>builder()
                    .success(false).message(validationError).build());
        }

        try {
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<DateTimeFinalizedDto>builder()
                    .success(true).message("Finalization updated").data(saved).build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error during Date/Time Finalized update: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<DateTimeFinalizedDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            log.error("Business rule violation during Date/Time Finalized update: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED) // 423 LOCKED
                    .body(ApiResponse.<DateTimeFinalizedDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error updating Date/Time Finalized for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<DateTimeFinalizedDto>builder()
                            .success(false)
                            .message("Error updating Date/Time Finalized: " + ex.getMessage())
                            .build());
        }
    }

    // DELETE (423 if signed)
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            service.delete(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("Finalization deleted").build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // ESIGN
    @PostMapping("/{patientId}/{encounterId}/{id}/esign")
    public ResponseEntity<ApiResponse<DateTimeFinalizedDto>> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(patientId, encounterId, id, user);
            return ResponseEntity.ok(ApiResponse.<DateTimeFinalizedDto>builder()
                    .success(true).message("Finalization e-signed").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<DateTimeFinalizedDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("eSign failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<DateTimeFinalizedDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // PRINT (PDF)
    @GetMapping("/{patientId}/{encounterId}/{id}/print")
    public ResponseEntity<?> print(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            byte[] pdf = service.renderPdf(patientId, encounterId, id);
            String filename = "date-time-finalized-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException ex) {
            log.error("Error printing Date/Time Finalized for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error generating Date/Time Finalized PDF", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message("Error generating PDF: " + ex.getMessage()).build());
        }
    }

    /**
     * Validates mandatory fields for DateTimeFinalized creation and update
     * @param dto DateTimeFinalizedDto to validate
     * @return error message if validation fails, null if validation passes
     */
    private String validateMandatoryFields(DateTimeFinalizedDto dto) {
        StringBuilder missingFields = new StringBuilder();

        if (dto.getTargetType() == null || dto.getTargetType().trim().isEmpty()) {
            missingFields.append("targetType, ");
        }

        if (dto.getTargetId() == null) {
            missingFields.append("targetId, ");
        }

        if (dto.getFinalizedAt() == null || dto.getFinalizedAt().trim().isEmpty()) {
            missingFields.append("finalizedAt, ");
        }

        if (dto.getFinalizedBy() == null || dto.getFinalizedBy().trim().isEmpty()) {
            missingFields.append("finalizedBy, ");
        }

        if (dto.getFinalizerRole() == null || dto.getFinalizerRole().trim().isEmpty()) {
            missingFields.append("finalizerRole, ");
        }

        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            missingFields.append("reason, ");
        }

        if (!missingFields.isEmpty()) {
            // Remove the trailing comma and space
            missingFields.setLength(missingFields.length() - 2);
            return "Missing mandatory fields: " + missingFields;
        }

        return null;
    }
}
