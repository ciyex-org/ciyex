//package org.ciyex.ehr.controller;
//
//import org.ciyex.ehr.dto.ApiResponse;
//import org.ciyex.ehr.dto.FamilyHistoryDto;
//import org.ciyex.ehr.service.FamilyHistoryService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/family-history")
//@RequiredArgsConstructor
//@Slf4j
//public class FamilyHistoryController {
//
//    private final FamilyHistoryService service;
//
//    // READ ALL: /api/family-history/{patientId}
//    @GetMapping("/{patientId}")
//    public ResponseEntity<ApiResponse<List<FamilyHistoryDto>>> getAllByPatient(
//            @PathVariable Long patientId,
//            ) {
//        var list = service.getAllByPatient(patientId);
//        return ResponseEntity.ok(ApiResponse.<List<FamilyHistoryDto>>builder()
//                .success(true).message("Family History fetched").data(list).build());
//    }
//
//    // READ ALL: /api/family-history/{patientId}/{encounterId}
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<FamilyHistoryDto>>> getAllByEncounter(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            ) {
//        var list = service.getAllByEncounter(patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<FamilyHistoryDto>>builder()
//                .success(true).message("Family History fetched").data(list).build());
//    }
//
//    // READ ONE: /api/family-history/{patientId}/{encounterId}/{id}
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<FamilyHistoryDto>> getOne(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            ) {
//        var dto = service.getOne(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<FamilyHistoryDto>builder()
//                .success(true).message("Family History fetched").data(dto).build());
//    }
//
//    // CREATE
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<FamilyHistoryDto>> create(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            //            @RequestBody FamilyHistoryDto dto) {
//        var created = service.create(patientId, encounterId, dto);
//        return ResponseEntity.ok(ApiResponse.<FamilyHistoryDto>builder()
//                .success(true).message("Family History created").data(created).build());
//    }
//
//    // UPDATE (replace entries)
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<FamilyHistoryDto>> update(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            //            @RequestBody FamilyHistoryDto dto) {
//        var updated = service.update(patientId, encounterId, id, dto);
//        return ResponseEntity.ok(ApiResponse.<FamilyHistoryDto>builder()
//                .success(true).message("Family History updated").data(updated).build());
//    }
//
//    // DELETE
//    @DeleteMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            ) {
//        service.delete(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<Void>builder()
//                .success(true).message("Family History deleted").build());
//    }
//}






package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.FamilyHistoryDto;
import org.ciyex.ehr.service.FamilyHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/family-history")
@RequiredArgsConstructor
@Slf4j
public class FamilyHistoryController {
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<FamilyHistoryDto>>> getAllByPatient(@PathVariable Long patientId) {
        try {
            var items = service.getAllByPatient(patientId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<FamilyHistoryDto>>builder()
                        .success(true)
                        .message("No family history found for Patient ID: " + patientId)
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<FamilyHistoryDto>>builder()
                    .success(true)
                    .message("Family history fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching family history for Patient ID: " + patientId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<FamilyHistoryDto>>builder()
                            .success(false)
                            .message("Error fetching family history for Patient ID: " + patientId + ". " + ex.getMessage())
                            .build());
        }
    }

    private final FamilyHistoryService service;

    /**
     * Validates that all entries have mandatory fields: relation, diagnosisCode, and diagnosisText.
     * Throws IllegalArgumentException if any field is missing.
     */
    private void validateMandatoryFields(FamilyHistoryDto dto) {
        if (dto.getEntries() == null || dto.getEntries().isEmpty()) {
            return; // Allow empty entries for container creation
        }

        List<String> missingFields = new java.util.ArrayList<>();
        for (int i = 0; i < dto.getEntries().size(); i++) {
            var entry = dto.getEntries().get(i);
            if (entry.getRelation() == null || entry.getRelation().trim().isEmpty()) {
                missingFields.add("Entry " + (i + 1) + ": relation");
            }
            if (entry.getDiagnosisCode() == null || entry.getDiagnosisCode().trim().isEmpty()) {
                missingFields.add("Entry " + (i + 1) + ": diagnosisCode");
            }
            if (entry.getDiagnosisText() == null || entry.getDiagnosisText().trim().isEmpty()) {
                missingFields.add("Entry " + (i + 1) + ": diagnosisText");
            }
        }

        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Missing mandatory fields: " + String.join(", ", missingFields));
        }
    }

    // LIST (usually one container)
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<FamilyHistoryDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        try {
            var items = service.list(patientId, encounterId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<FamilyHistoryDto>>builder()
                        .success(true)
                        .message(String.format("No family history found for Patient ID: %d, Encounter ID: %d", patientId, encounterId))
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<FamilyHistoryDto>>builder()
                    .success(true)
                    .message("Family history fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching family history for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<FamilyHistoryDto>>builder()
                            .success(false)
                            .message(String.format("Error fetching family history for Patient ID: %d, Encounter ID: %d. %s", patientId, encounterId, ex.getMessage()))
                            .build());
        }
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<FamilyHistoryDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            var dto = service.getOne(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<FamilyHistoryDto>builder()
                    .success(true).message("Family history fetched").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<FamilyHistoryDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // CREATE container (with optional entries)
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<FamilyHistoryDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody FamilyHistoryDto dto) {
        try {
            validateMandatoryFields(dto);
            var saved = service.create(patientId, encounterId, dto);
            return ResponseEntity.ok(ApiResponse.<FamilyHistoryDto>builder()
                    .success(true).message("Family history created").data(saved).build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error during Family History creation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<FamilyHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            log.error("Business rule violation during Family History creation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(ApiResponse.<FamilyHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error creating Family History for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<FamilyHistoryDto>builder()
                            .success(false)
                            .message("Error creating Family History: " + ex.getMessage())
                            .build());
        }
    }

    // REPLACE entries (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<FamilyHistoryDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody FamilyHistoryDto dto) {
        try {
            validateMandatoryFields(dto);
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<FamilyHistoryDto>builder()
                    .success(true).message("Family history updated").data(saved).build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error during Family History update: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<FamilyHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            log.error("Business rule violation during Family History update: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED) // 423 LOCKED
                    .body(ApiResponse.<FamilyHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error updating Family History for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<FamilyHistoryDto>builder()
                            .success(false)
                            .message("Error updating Family History: " + ex.getMessage())
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
                    .success(true).message("Family history deleted").build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // ESIGN (idempotent). Body may include {"entryId": 123}
    @PostMapping("/{patientId}/{encounterId}/{id}/esign")
    public ResponseEntity<ApiResponse<FamilyHistoryDto>> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            Long entryId = null;
            if (body != null && body.get("entryId") != null) {
                try { entryId = Long.valueOf(String.valueOf(body.get("entryId"))); } catch (Exception ignore) {}
            }
            var dto = service.eSign(patientId, encounterId, id, user, entryId);
            return ResponseEntity.ok(ApiResponse.<FamilyHistoryDto>builder()
                    .success(true).message("Family history e-signed").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<FamilyHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("eSign failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<FamilyHistoryDto>builder().success(false).message(ex.getMessage()).build());
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
            String filename = "family-history-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException ex) {
            log.error("Error printing family history for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error generating PDF", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message("Error generating PDF: " + ex.getMessage()).build());
        }
    }
}
