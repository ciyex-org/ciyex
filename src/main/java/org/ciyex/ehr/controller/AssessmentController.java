//package org.ciyex.ehr.controller;
//
//import org.ciyex.ehr.dto.ApiResponse;
//import org.ciyex.ehr.dto.AssessmentDto;
//import org.ciyex.ehr.service.AssessmentService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/assessment")
//@RequiredArgsConstructor
//@Slf4j
//public class AssessmentController {
//
//    private final AssessmentService service;
//
//    // READ ALL: /api/assessment/{patientId}
//    @GetMapping("/{patientId}")
//    public ResponseEntity<ApiResponse<List<AssessmentDto>>> getAllByPatient(
//            @PathVariable Long patientId,
//            ) {
//        var list = service.getAllByPatient(patientId);
//        return ResponseEntity.ok(ApiResponse.<List<AssessmentDto>>builder()
//                .success(true).message("Assessment list fetched").data(list).build());
//    }
//
//    // READ ALL: /api/assessment/{patientId}/{encounterId}
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<AssessmentDto>>> getAllByEncounter(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            ) {
//        var list = service.getAllByEncounter(patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<AssessmentDto>>builder()
//                .success(true).message("Assessment list fetched").data(list).build());
//    }
//
//    // READ ONE: /api/assessment/{patientId}/{encounterId}/{id}
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<AssessmentDto>> getOne(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            ) {
//        var dto = service.getOne(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<AssessmentDto>builder()
//                .success(true).message("Assessment fetched").data(dto).build());
//    }
//
//    // CREATE: /api/assessment/{patientId}/{encounterId}
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<AssessmentDto>> create(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            //            @RequestBody AssessmentDto dto) {
//        var created = service.create(patientId, encounterId, dto);
//        return ResponseEntity.ok(ApiResponse.<AssessmentDto>builder()
//                .success(true).message("Assessment created").data(created).build());
//    }
//
//    // UPDATE: /api/assessment/{patientId}/{encounterId}/{id}
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<AssessmentDto>> update(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            //            @RequestBody AssessmentDto dto) {
//        var updated = service.update(patientId, encounterId, id, dto);
//        return ResponseEntity.ok(ApiResponse.<AssessmentDto>builder()
//                .success(true).message("Assessment updated").data(updated).build());
//    }
//
//    // DELETE: /api/assessment/{patientId}/{encounterId}/{id}
//    @DeleteMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            ) {
//        service.delete(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<Void>builder()
//                .success(true).message("Assessment deleted").build());
//    }
//}





package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.AssessmentDto;
import org.ciyex.ehr.service.AssessmentService;
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
@RequestMapping("/api/assessment")
@RequiredArgsConstructor
@Slf4j
public class AssessmentController {
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<AssessmentDto>>> getAllByPatient(@PathVariable Long patientId) {
        try {
            var items = service.getAllByPatient(patientId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<AssessmentDto>>builder()
                        .success(true)
                        .message("No Assessments found for Patient ID: " + patientId)
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<AssessmentDto>>builder()
                    .success(true)
                    .message("Assessments fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Assessments for Patient ID: " + patientId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<AssessmentDto>>builder()
                            .success(false)
                            .message("Error fetching Assessments for Patient ID: " + patientId + ". " + ex.getMessage())
                            .build());
        }
    }

    private final AssessmentService service;

    // LIST
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<AssessmentDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        try {
            var list = service.getAllByEncounter(patientId, encounterId);
            if (list.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<AssessmentDto>>builder()
                        .success(true)
                        .message(String.format("No Assessments found for Patient ID: %d, Encounter ID: %d", patientId, encounterId))
                        .data(list)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<AssessmentDto>>builder()
                    .success(true)
                    .message("Assessments fetched successfully")
                    .data(list)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Assessments for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<AssessmentDto>>builder()
                            .success(false)
                            .message(String.format("Error fetching Assessments for Patient ID: %d, Encounter ID: %d. %s", patientId, encounterId, ex.getMessage()))
                            .build());
        }
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<AssessmentDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            var dto = service.getOne(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<AssessmentDto>builder()
                    .success(true).message("Assessment fetched successfully").data(dto).build());
        } catch (IllegalArgumentException ex) {
            log.error("Assessment not found: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<AssessmentDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error fetching Assessment for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<AssessmentDto>builder()
                            .success(false)
                            .message("Error fetching Assessment: " + ex.getMessage())
                            .build());
        }
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<AssessmentDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody AssessmentDto dto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(dto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<AssessmentDto>builder()
                    .success(false).message(validationError).build());
        }

        try {
            var saved = service.create(patientId, encounterId, dto);
            return ResponseEntity.ok(ApiResponse.<AssessmentDto>builder()
                    .success(true).message("Assessment created").data(saved).build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error during Assessment creation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<AssessmentDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            log.error("Business rule violation during Assessment creation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(ApiResponse.<AssessmentDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error creating Assessment for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<AssessmentDto>builder()
                            .success(false)
                            .message("Error creating Assessment: " + ex.getMessage())
                            .build());
        }
    }

    // UPDATE (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<AssessmentDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody AssessmentDto dto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(dto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<AssessmentDto>builder()
                    .success(false).message(validationError).build());
        }

        try {
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<AssessmentDto>builder()
                    .success(true).message("Assessment updated").data(saved).build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error during Assessment update: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<AssessmentDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            log.error("Business rule violation during Assessment update: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED) // 423 LOCKED
                    .body(ApiResponse.<AssessmentDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error updating Assessment for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<AssessmentDto>builder()
                            .success(false)
                            .message("Error updating Assessment: " + ex.getMessage())
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
                    .success(true).message("Assessment deleted").build());
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
    public ResponseEntity<ApiResponse<AssessmentDto>> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(patientId, encounterId, id, user);
            return ResponseEntity.ok(ApiResponse.<AssessmentDto>builder()
                    .success(true).message("Assessment e-signed").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<AssessmentDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("eSign failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<AssessmentDto>builder().success(false).message(ex.getMessage()).build());
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
            String filename = "assessment-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException ex) {
            log.error("Error printing Assessment for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error generating Assessment PDF", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message("Error generating PDF: " + ex.getMessage()).build());
        }
    }

    /**
     * Validates mandatory fields for Assessment creation and update
     * @param dto AssessmentDto to validate
     * @return error message if validation fails, null if validation passes
     */
    private String validateMandatoryFields(AssessmentDto dto) {
        StringBuilder missingFields = new StringBuilder();

        if (dto.getDiagnosisCode() == null || dto.getDiagnosisCode().trim().isEmpty()) {
            missingFields.append("diagnosisCode, ");
        }

        if (dto.getDiagnosisName() == null || dto.getDiagnosisName().trim().isEmpty()) {
            missingFields.append("diagnosisName, ");
        }

        if (dto.getAssessmentText() == null || dto.getAssessmentText().trim().isEmpty()) {
            missingFields.append("assessmentText, ");
        }

        // if (dto.getNotes() == null || dto.getNotes().trim().isEmpty()) {
        //     missingFields.append("notes, ");
        // }

        if (!missingFields.isEmpty()) {
            // Remove the trailing comma and space
            missingFields.setLength(missingFields.length() - 2);
            return "Missing mandatory fields: " + missingFields;
        }

        return null;
    }
}
