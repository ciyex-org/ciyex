//package com.qiaben.ciyex.controller;
//
//import com.qiaben.ciyex.dto.ApiResponse;
//import com.qiaben.ciyex.dto.ReviewOfSystemDto;
//import com.qiaben.ciyex.service.ReviewOfSystemService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//

//import java.util.List;
//
//@RestController
//@RequestMapping("/api/reviewofsystems") // short path to match your Bruno screenshot
//@RequiredArgsConstructor
//@Slf4j
//public class ReviewOfSystemController {
//
//    private final ReviewOfSystemService service;
//
//    @GetMapping("/{patientId}")
//    public ResponseEntity<ApiResponse<List<ReviewOfSystemDto>>> getAllByPatient(
//            @PathVariable Long patientId) {
//        var list = service.getAllByPatient(patientId);
//        return ResponseEntity.ok(ApiResponse.<List<ReviewOfSystemDto>>builder()
//                .success(true).message("ROS fetched").data(list).build());
//    }
//
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<ReviewOfSystemDto>>> getAllByEncounter(
//            @PathVariable Long patientId, @PathVariable Long encounterId,
//            ) {
//        var list = service.getAllByEncounter(patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<ReviewOfSystemDto>>builder()
//                .success(true).message("ROS fetched").data(list).build());
//    }
//
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> getOne(
//            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
//            ) {
//        var dto = service.getOne(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
//                .success(true).message("ROS fetched").data(dto).build());
//    }
//
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> create(
//            @PathVariable Long patientId, @PathVariable Long encounterId,
//            @RequestBody ReviewOfSystemDto dto) {
//        var created = service.create(patientId, encounterId, dto);
//        return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
//                .success(true).message("ROS created").data(created).build());
//    }
//
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> update(
//            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
//            @RequestBody ReviewOfSystemDto dto) {
//        var updated = service.update(patientId, encounterId, id, dto);
//        return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
//                .success(true).message("ROS updated").data(updated).build());
//    }
//
//    @DeleteMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(
//            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
//            ) {
//        service.delete(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<Void>builder()
//                .success(true).message("ROS deleted").build());
//    }
//}


package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ReviewOfSystemDto;
import com.qiaben.ciyex.service.ReviewOfSystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/reviewofsystems")
@RequiredArgsConstructor
@Slf4j
public class ReviewOfSystemController {
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<ReviewOfSystemDto>>> getAllByPatient(@PathVariable Long patientId) {
        try {
            var items = service.getAllByPatient(patientId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<ReviewOfSystemDto>>builder()
                        .success(true)
                        .message("No Review of Systems found for Patient ID: " + patientId)
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<ReviewOfSystemDto>>builder()
                    .success(true)
                    .message("Review of Systems fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Review of Systems for Patient ID: " + patientId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<ReviewOfSystemDto>>builder()
                            .success(false)
                            .message("Error fetching Review of Systems for Patient ID: " + patientId + ". " + ex.getMessage())
                            .build());
        }
    }

    private final ReviewOfSystemService service;

    /**
     * Validates that all mandatory fields are present: systemName, notes, and systemDetails.
     * Throws IllegalArgumentException if any field is missing.
     */
    private void validateMandatoryFields(ReviewOfSystemDto dto) {
        List<String> missingFields = new java.util.ArrayList<>();

        if (dto.getSystemName() == null || dto.getSystemName().trim().isEmpty()) {
            missingFields.add("systemName");
        }
        if (dto.getNotes() == null || dto.getNotes().trim().isEmpty()) {
            missingFields.add("notes");
        }
        if (dto.getSystemDetails() == null || dto.getSystemDetails().isEmpty()) {
            missingFields.add("systemDetails");
        }

        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Missing mandatory fields: " + String.join(", ", missingFields));
        }
    }

    // LIST
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<ReviewOfSystemDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        try {
            var items = service.list(patientId, encounterId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<ReviewOfSystemDto>>builder()
                        .success(true)
                        .message(String.format("No Review of Systems found for Patient ID: %d, Encounter ID: %d", patientId, encounterId))
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<ReviewOfSystemDto>>builder()
                    .success(true)
                    .message("ROS list fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching ROS for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<ReviewOfSystemDto>>builder()
                            .success(false)
                            .message(String.format("Error fetching ROS for Patient ID: %d, Encounter ID: %d. %s", patientId, encounterId, ex.getMessage()))
                            .build());
        }
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            var dto = service.getOne(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
                    .success(true).message("ROS fetched").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ReviewOfSystemDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody ReviewOfSystemDto dto) {
        try {
            validateMandatoryFields(dto);
            var saved = service.create(patientId, encounterId, dto);
            return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
                    .success(true).message("ROS created").data(saved).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<ReviewOfSystemDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // UPDATE (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody ReviewOfSystemDto dto) {
        try {
            validateMandatoryFields(dto);
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
                    .success(true).message("ROS updated").data(saved).build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.<ReviewOfSystemDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<ReviewOfSystemDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // DELETE (423 if signed)
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            service.delete(patientId, encounterId, id);
            return ResponseEntity.noContent().build(); // 204 suits current UI. :contentReference[oaicite:1]{index=1}
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder().success(false).message(ex.getMessage()).build());
        }
    }

    // ESIGN (idempotent)
    @PostMapping("/{patientId}/{encounterId}/{id}/esign")
    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(patientId, encounterId, id, user);
            return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
                    .success(true).message("ROS e-signed").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ReviewOfSystemDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<ReviewOfSystemDto>builder().success(false).message(ex.getMessage()).build());
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
            String filename = "ros-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException ex) {
            log.error("Error printing ROS for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error generating ROS PDF", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message("Error generating PDF: " + ex.getMessage()).build());
        }
    }
}
