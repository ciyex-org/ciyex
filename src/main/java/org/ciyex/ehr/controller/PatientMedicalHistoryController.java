//package org.ciyex.ehr.controller;
//
//import org.ciyex.ehr.dto.ApiResponse;
//import org.ciyex.ehr.dto.PatientMedicalHistoryDto;
//import org.ciyex.ehr.service.PatientMedicalHistoryService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/patient-medical-history")
//@RequiredArgsConstructor
//@Slf4j
//public class PatientMedicalHistoryController {
//
//    private final PatientMedicalHistoryService service;
//
//    @GetMapping("/{patientId}")
//    public ResponseEntity<ApiResponse<List<PatientMedicalHistoryDto>>> getAllByPatient(
//            @PathVariable Long patientId,
//            ) {
//        var list = service.getAllByPatient(patientId);
//        return ResponseEntity.ok(ApiResponse.<List<PatientMedicalHistoryDto>>builder()
//                .success(true).message("PMH fetched successfully").data(list).build());
//    }
//
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<PatientMedicalHistoryDto>>> getAllByEncounter(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            ) {
//        var list = service.getAllByEncounter(patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<PatientMedicalHistoryDto>>builder()
//                .success(true).message("PMH fetched successfully").data(list).build());
//    }
//
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<PatientMedicalHistoryDto>> getOne(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            ) {
//        var dto = service.getOne(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<PatientMedicalHistoryDto>builder()
//                .success(true).message("PMH fetched successfully").data(dto).build());
//    }
//
//    // <-- THIS is the mapping you’re calling
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<PatientMedicalHistoryDto>> create(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            //            @RequestBody PatientMedicalHistoryDto dto) {
//        var created = service.create(patientId, encounterId, dto);
//        return ResponseEntity.ok(ApiResponse.<PatientMedicalHistoryDto>builder()
//                .success(true).message("PMH created").data(created).build());
//    }
//
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<PatientMedicalHistoryDto>> update(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            //            @RequestBody PatientMedicalHistoryDto dto) {
//        var updated = service.update(patientId, encounterId, id, dto);
//        return ResponseEntity.ok(ApiResponse.<PatientMedicalHistoryDto>builder()
//                .success(true).message("PMH updated").data(updated).build());
//    }
//
//    @DeleteMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            ) {
//        service.delete(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<Void>builder()
//                .success(true).message("PMH deleted").build());
//    }
//}




package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.PatientMedicalHistoryDto;
import org.ciyex.ehr.service.PatientMedicalHistoryService;
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
@RequestMapping("/api/patient-medical-history")
@RequiredArgsConstructor
@Slf4j
public class PatientMedicalHistoryController {
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<PatientMedicalHistoryDto>>> getAllByPatient(@PathVariable Long patientId) {
        try {
            var items = service.getAllByPatient(patientId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<PatientMedicalHistoryDto>>builder()
                        .success(true)
                        .message("No Patient Medical History found for Patient ID: " + patientId)
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<PatientMedicalHistoryDto>>builder()
                    .success(true)
                    .message("Patient Medical History fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Patient Medical History for Patient ID: " + patientId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<PatientMedicalHistoryDto>>builder()
                            .success(false)
                            .message("Error fetching Patient Medical History for Patient ID: " + patientId + ". " + ex.getMessage())
                            .build());
        }
    }

    private final PatientMedicalHistoryService service;

    // LIST
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<PatientMedicalHistoryDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        try {
            var items = service.list(patientId, encounterId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<PatientMedicalHistoryDto>>builder()
                        .success(true)
                        .message(String.format("No Patient Medical History found for Patient ID: %d, Encounter ID: %d", patientId, encounterId))
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<PatientMedicalHistoryDto>>builder()
                    .success(true)
                    .message("History list fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Patient Medical History for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<PatientMedicalHistoryDto>>builder()
                            .success(false)
                            .message(String.format("Error fetching Patient Medical History for Patient ID: %d, Encounter ID: %d. %s", patientId, encounterId, ex.getMessage()))
                            .build());
        }
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PatientMedicalHistoryDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id) {
        try {
            var dto = service.getOne(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<PatientMedicalHistoryDto>builder()
                    .success(true).message("History fetched").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PatientMedicalHistoryDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<PatientMedicalHistoryDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody PatientMedicalHistoryDto dto) {
        try {
            var saved = service.create(patientId, encounterId, dto);
            return ResponseEntity.ok(ApiResponse.<PatientMedicalHistoryDto>builder()
                    .success(true).message("History created").data(saved).build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error during Patient Medical History creation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<PatientMedicalHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            log.error("Business rule violation during Patient Medical History creation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(ApiResponse.<PatientMedicalHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error creating Patient Medical History for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PatientMedicalHistoryDto>builder()
                            .success(false)
                            .message("Error creating Patient Medical History: " + ex.getMessage())
                            .build());
        }
    }

    // UPDATE (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PatientMedicalHistoryDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id,
            @RequestBody PatientMedicalHistoryDto dto) {
        try {
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<PatientMedicalHistoryDto>builder()
                    .success(true).message("History updated").data(saved).build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error during Patient Medical History update: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<PatientMedicalHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            log.error("Business rule violation during Patient Medical History update: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED) // 423 LOCKED
                    .body(ApiResponse.<PatientMedicalHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error updating Patient Medical History for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PatientMedicalHistoryDto>builder()
                            .success(false)
                            .message("Error updating Patient Medical History: " + ex.getMessage())
                            .build());
        }
    }

    // DELETE (423 if signed)
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id) {
        try {
            service.delete(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("History deleted").build());
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
    public ResponseEntity<ApiResponse<PatientMedicalHistoryDto>> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id,
            Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(patientId, encounterId, id, user);
            return ResponseEntity.ok(ApiResponse.<PatientMedicalHistoryDto>builder()
                    .success(true).message("History e-signed").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PatientMedicalHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("eSign failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<PatientMedicalHistoryDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // PRINT (PDF)
    @GetMapping("/{patientId}/{encounterId}/{id}/print")
    public ResponseEntity<?> print(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id) {
        try {
            byte[] pdf = service.renderPdf(patientId, encounterId, id);
            String filename = "patient-medical-history-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException ex) {
            log.error("Error printing Patient Medical History for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error generating Patient Medical History PDF", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message("Error generating PDF: " + ex.getMessage()).build());
        }
    }
}
