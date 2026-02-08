//package org.ciyex.ehr.controller;
//
//import org.ciyex.ehr.dto.ApiResponse;
//import org.ciyex.ehr.dto.HistoryOfPresentIllnessDto;
//import org.ciyex.ehr.service.HistoryOfPresentIllnessService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/history-of-present-illness")
//@RequiredArgsConstructor
//@Slf4j
//public class HistoryOfPresentIllnessController {
//
//    private final HistoryOfPresentIllnessService service;
//
//    // READ ALL: /api/history-of-present-illness/{patientId}
//    @GetMapping("/{patientId}")
//    public ResponseEntity<ApiResponse<List<HistoryOfPresentIllnessDto>>> getAllByPatient(
//            @PathVariable Long patientId,
//            ) {
//        var list = service.getAllByPatient(patientId);
//        return ResponseEntity.ok(ApiResponse.<List<HistoryOfPresentIllnessDto>>builder()
//                .success(true).message("HPI fetched successfully").data(list).build());
//    }
//
//    // READ ALL: /api/history-of-present-illness/{patientId}/{encounterId}
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<HistoryOfPresentIllnessDto>>> getAllByEncounter(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            ) {
//        var list = service.getAllByEncounter(patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<HistoryOfPresentIllnessDto>>builder()
//                .success(true).message("HPI fetched successfully").data(list).build());
//    }
//
//    // READ ONE: /api/history-of-present-illness/{patientId}/{encounterId}/{id}
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> getOne(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            ) {
//        var dto = service.getOne(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<HistoryOfPresentIllnessDto>builder()
//                .success(true).message("HPI fetched successfully").data(dto).build());
//    }
//
//    // CREATE: /api/history-of-present-illness/{patientId}/{encounterId}
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> create(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            //            @RequestBody HistoryOfPresentIllnessDto dto) {
//        var created = service.create(patientId, encounterId, dto);
//        return ResponseEntity.ok(ApiResponse.<HistoryOfPresentIllnessDto>builder()
//                .success(true).message("HPI created").data(created).build());
//    }
//
//    // UPDATE: /api/history-of-present-illness/{patientId}/{encounterId}/{id}
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> update(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            //            @RequestBody HistoryOfPresentIllnessDto dto) {
//        var updated = service.update(patientId, encounterId, id, dto);
//        return ResponseEntity.ok(ApiResponse.<HistoryOfPresentIllnessDto>builder()
//                .success(true).message("HPI updated").data(updated).build());
//    }
//
//    // DELETE: /api/history-of-present-illness/{patientId}/{encounterId}/{id}
//    @DeleteMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            ) {
//        service.delete(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<Void>builder()
//                .success(true).message("HPI deleted").build());
//    }
//}







package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.HistoryOfPresentIllnessDto;
import org.ciyex.ehr.service.HistoryOfPresentIllnessService;
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
@RequestMapping("/api/history-of-present-illness")
@RequiredArgsConstructor
@Slf4j
public class HistoryOfPresentIllnessController {
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<HistoryOfPresentIllnessDto>>> getAllByPatient(@PathVariable Long patientId) {
        try {
            var items = service.getAllByPatient(patientId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<HistoryOfPresentIllnessDto>>builder()
                        .success(true)
                        .message("No History of Present Illness found for Patient ID: " + patientId)
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<HistoryOfPresentIllnessDto>>builder()
                    .success(true)
                    .message("History of Present Illness fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching HPI for Patient ID: " + patientId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<HistoryOfPresentIllnessDto>>builder()
                            .success(false)
                            .message("Error fetching HPI for Patient ID: " + patientId + ". " + ex.getMessage())
                            .build());
        }
    }

    private final HistoryOfPresentIllnessService service;

    // LIST
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<HistoryOfPresentIllnessDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        try {
            var items = service.list(patientId, encounterId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<HistoryOfPresentIllnessDto>>builder()
                        .success(true)
                        .message(String.format("No History of Present Illness found for Patient ID: %d, Encounter ID: %d", patientId, encounterId))
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<HistoryOfPresentIllnessDto>>builder()
                    .success(true)
                    .message("HPI list fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching HPI for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<HistoryOfPresentIllnessDto>>builder()
                            .success(false)
                            .message(String.format("Error fetching HPI for Patient ID: %d, Encounter ID: %d. %s", patientId, encounterId, ex.getMessage()))
                            .build());
        }
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            var dto = service.getOne(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<HistoryOfPresentIllnessDto>builder()
                    .success(true).message("HPI fetched").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<HistoryOfPresentIllnessDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody HistoryOfPresentIllnessDto dto) {
        try {
            var saved = service.create(patientId, encounterId, dto);
            return ResponseEntity.ok(ApiResponse.<HistoryOfPresentIllnessDto>builder()
                    .success(true).message("HPI created").data(saved).build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error during HPI creation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<HistoryOfPresentIllnessDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            log.error("Business rule violation during HPI creation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(ApiResponse.<HistoryOfPresentIllnessDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error creating HPI for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<HistoryOfPresentIllnessDto>builder()
                            .success(false)
                            .message("Error creating HPI: " + ex.getMessage())
                            .build());
        }
    }

    // UPDATE (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody HistoryOfPresentIllnessDto dto) {
        try {
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<HistoryOfPresentIllnessDto>builder()
                    .success(true).message("HPI updated").data(saved).build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error during HPI update: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<HistoryOfPresentIllnessDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            log.error("Business rule violation during HPI update: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED) // 423 LOCKED
                    .body(ApiResponse.<HistoryOfPresentIllnessDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error updating HPI for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<HistoryOfPresentIllnessDto>builder()
                            .success(false)
                            .message("Error updating HPI: " + ex.getMessage())
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
                    .success(true).message("HPI deleted").build());
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
    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(patientId, encounterId, id, user);
            return ResponseEntity.ok(ApiResponse.<HistoryOfPresentIllnessDto>builder()
                    .success(true).message("HPI e-signed").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<HistoryOfPresentIllnessDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("HPI eSign failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<HistoryOfPresentIllnessDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // PRINT
    @GetMapping("/{patientId}/{encounterId}/{id}/print")
    public ResponseEntity<?> print(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            byte[] pdf = service.renderPdf(patientId, encounterId, id);
            String filename = "hpi-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException ex) {
            log.error("Error printing HPI for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error generating HPI PDF", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message("Error generating PDF: " + ex.getMessage()).build());
        }
    }
}
