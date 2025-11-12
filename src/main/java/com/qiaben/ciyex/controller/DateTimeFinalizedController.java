//package com.qiaben.ciyex.controller;
//
//import com.qiaben.ciyex.dto.ApiResponse;
//import com.qiaben.ciyex.dto.DateTimeFinalizedDto;
//import com.qiaben.ciyex.service.DateTimeFinalizedService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/date-time-finalized")
//@RequiredArgsConstructor
//@Slf4j
//public class DateTimeFinalizedController {
//
//    private final DateTimeFinalizedService service;
//
//    // READ ALL by patient
//    @GetMapping("/{patientId}")
//    public ResponseEntity<ApiResponse<List<DateTimeFinalizedDto>>> getAllByPatient(
//            @PathVariable Long patientId,
//            ) {
//        var list = service.getAllByPatient(patientId);
//        return ResponseEntity.ok(ApiResponse.<List<DateTimeFinalizedDto>>builder()
//                .success(true).message("Finalization timestamps fetched").data(list).build());
//    }
//
//    // READ ALL by patient + encounter
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<DateTimeFinalizedDto>>> getAllByEncounter(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            ) {
//        var list = service.getAllByEncounter(patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<DateTimeFinalizedDto>>builder()
//                .success(true).message("Finalization timestamps fetched").data(list).build());
//    }
//
//    // READ ONE
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<DateTimeFinalizedDto>> getOne(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            ) {
//        var dto = service.getOne(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<DateTimeFinalizedDto>builder()
//                .success(true).message("Finalization timestamp fetched").data(dto).build());
//    }
//
//    // CREATE
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<DateTimeFinalizedDto>> create(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            //            @RequestBody DateTimeFinalizedDto dto) {
//        var created = service.create(patientId, encounterId, dto);
//        return ResponseEntity.ok(ApiResponse.<DateTimeFinalizedDto>builder()
//                .success(true).message("Finalization timestamp created").data(created).build());
//    }
//
//    // UPDATE
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<DateTimeFinalizedDto>> update(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            //            @RequestBody DateTimeFinalizedDto dto) {
//        var updated = service.update(patientId, encounterId, id, dto);
//        return ResponseEntity.ok(ApiResponse.<DateTimeFinalizedDto>builder()
//                .success(true).message("Finalization timestamp updated").data(updated).build());
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
//                .success(true).message("Finalization timestamp deleted").build());
//    }
//}






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
        var items = service.getAllByPatient(patientId);
        return ResponseEntity.ok(ApiResponse.<List<DateTimeFinalizedDto>>builder().success(true).message("Fetched").data(items).build());
    }

    private final DateTimeFinalizedService service;

    // LIST
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<DateTimeFinalizedDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        var items = service.list(patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<DateTimeFinalizedDto>>builder()
                .success(true).message("Finalizations fetched").data(items).build());
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
                    .success(true).message("Finalization fetched").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<DateTimeFinalizedDto>builder().success(false).message(ex.getMessage()).build());
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

        var saved = service.create(patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<DateTimeFinalizedDto>builder()
                .success(true).message("Finalization created").data(saved).build());
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
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.<DateTimeFinalizedDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<DateTimeFinalizedDto>builder().success(false).message(ex.getMessage()).build());
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
    public ResponseEntity<byte[]> print(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        byte[] pdf = service.renderPdf(patientId, encounterId, id);
        String filename = "date-time-finalized-" + id + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
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
