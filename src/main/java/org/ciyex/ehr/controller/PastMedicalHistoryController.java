//package org.ciyex.ehr.controller;
//
//import org.ciyex.ehr.dto.ApiResponse;
//import org.ciyex.ehr.dto.PastMedicalHistoryDto;
//import org.ciyex.ehr.service.PastMedicalHistoryService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/past-medical-history")
//@RequiredArgsConstructor
//@Slf4j
//public class PastMedicalHistoryController {
//
//    private final PastMedicalHistoryService service;
//
//    // READ ALL: /api/past-medical-history/{patientId}
//    @GetMapping("/{patientId}")
//    public ResponseEntity<ApiResponse<List<PastMedicalHistoryDto>>> getAllByPatient(
//            @PathVariable Long patientId,
//            ) {
//        var list = service.getAllByPatient(patientId);
//        return ResponseEntity.ok(ApiResponse.<List<PastMedicalHistoryDto>>builder()
//                .success(true).message("PMH fetched successfully").data(list).build());
//    }
//
//    // READ ALL: /api/past-medical-history/{patientId}/{encounterId}
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<PastMedicalHistoryDto>>> getAllByEncounter(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            ) {
//        var list = service.getAllByEncounter(patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<PastMedicalHistoryDto>>builder()
//                .success(true).message("PMH fetched successfully").data(list).build());
//    }
//
//    // READ ONE: /api/past-medical-history/{patientId}/{encounterId}/{id}
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<PastMedicalHistoryDto>> getOne(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            ) {
//        var dto = service.getOne(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<PastMedicalHistoryDto>builder()
//                .success(true).message("PMH fetched successfully").data(dto).build());
//    }
//
//    // CREATE: /api/past-medical-history/{patientId}/{encounterId}
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<PastMedicalHistoryDto>> create(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            //            @RequestBody PastMedicalHistoryDto dto) {
//        var created = service.create(patientId, encounterId, dto);
//        return ResponseEntity.ok(ApiResponse.<PastMedicalHistoryDto>builder()
//                .success(true).message("PMH created").data(created).build());
//    }
//
//    // UPDATE: /api/past-medical-history/{patientId}/{encounterId}/{id}
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<PastMedicalHistoryDto>> update(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            //            @RequestBody PastMedicalHistoryDto dto) {
//        var updated = service.update(patientId, encounterId, id, dto);
//        return ResponseEntity.ok(ApiResponse.<PastMedicalHistoryDto>builder()
//                .success(true).message("PMH updated").data(updated).build());
//    }
//
//    // DELETE: /api/past-medical-history/{patientId}/{encounterId}/{id}
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
import org.ciyex.ehr.dto.PastMedicalHistoryDto;
import org.ciyex.ehr.service.PastMedicalHistoryService;
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
@RequestMapping("/api/past-medical-history")
@RequiredArgsConstructor
@Slf4j
public class PastMedicalHistoryController {
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<PastMedicalHistoryDto>>> getAllByPatient(@PathVariable Long patientId) {
        try {
            var items = service.getAllByPatient(patientId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<PastMedicalHistoryDto>>builder()
                        .success(true)
                        .message("No Past Medical History found for Patient ID: " + patientId)
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<PastMedicalHistoryDto>>builder()
                    .success(true)
                    .message("Past Medical History fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching PMH for Patient ID: " + patientId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<PastMedicalHistoryDto>>builder()
                            .success(false)
                            .message("Error fetching PMH for Patient ID: " + patientId + ". " + ex.getMessage())
                            .build());
        }
    }

    private final PastMedicalHistoryService service;

    // LIST
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<PastMedicalHistoryDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        try {
            var items = service.list(patientId, encounterId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<PastMedicalHistoryDto>>builder()
                        .success(true)
                        .message(String.format("No Past Medical History found for Patient ID: %d, Encounter ID: %d", patientId, encounterId))
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<PastMedicalHistoryDto>>builder()
                    .success(true)
                    .message("PMH list fetched successfully")
                    .data(items)
                    .build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<PastMedicalHistoryDto>>builder()
                            .success(false)
                            .message(ex.getMessage())
                            .build());
        } catch (Exception ex) {
            log.error("Error fetching PMH for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<PastMedicalHistoryDto>>builder()
                            .success(false)
                            .message(String.format("Error fetching PMH for Patient ID: %d, Encounter ID: %d. %s", patientId, encounterId, ex.getMessage()))
                            .build());
        }
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PastMedicalHistoryDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            var dto = service.getOne(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<PastMedicalHistoryDto>builder()
                    .success(true).message("PMH fetched").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PastMedicalHistoryDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<PastMedicalHistoryDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody PastMedicalHistoryDto dto) {
        try {
            var saved = service.create(patientId, encounterId, dto);
            return ResponseEntity.ok(ApiResponse.<PastMedicalHistoryDto>builder()
                    .success(true).message("PMH created").data(saved).build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error during PMH creation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<PastMedicalHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            log.error("Business rule violation during PMH creation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(ApiResponse.<PastMedicalHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error creating PMH for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PastMedicalHistoryDto>builder()
                            .success(false)
                            .message("Error creating PMH: " + ex.getMessage())
                            .build());
        }
    }

    // UPDATE (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PastMedicalHistoryDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody PastMedicalHistoryDto dto) {
        try {
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<PastMedicalHistoryDto>builder()
                    .success(true).message("PMH updated").data(saved).build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error during PMH update: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<PastMedicalHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            log.error("Business rule violation during PMH update: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED) // 423 LOCKED
                    .body(ApiResponse.<PastMedicalHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error updating PMH for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PastMedicalHistoryDto>builder()
                            .success(false)
                            .message("Error updating PMH: " + ex.getMessage())
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
                    .success(true).message("PMH deleted").build());
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
    public ResponseEntity<ApiResponse<PastMedicalHistoryDto>> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(patientId, encounterId, id, user);
            return ResponseEntity.ok(ApiResponse.<PastMedicalHistoryDto>builder()
                    .success(true).message("PMH e-signed").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PastMedicalHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("PMH eSign failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<PastMedicalHistoryDto>builder().success(false).message(ex.getMessage()).build());
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
            String filename = "pmh-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException ex) {
            log.error("Error printing PMH for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error generating PMH PDF", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message("Error generating PDF: " + ex.getMessage()).build());
        }
    }
}
