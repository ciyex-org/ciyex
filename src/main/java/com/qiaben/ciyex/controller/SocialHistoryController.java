//package com.qiaben.ciyex.controller;
//
//import com.qiaben.ciyex.dto.ApiResponse;
//import com.qiaben.ciyex.dto.SocialHistoryDto;
//import com.qiaben.ciyex.service.SocialHistoryService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/social-history")
//@RequiredArgsConstructor
//@Slf4j
//public class SocialHistoryController {
//
//    private final SocialHistoryService service;
//
//    @GetMapping("/{patientId}")
//    public ResponseEntity<ApiResponse<List<SocialHistoryDto>>> getAllByPatient(
//            @PathVariable Long patientId,
//            ) {
//        var list = service.getAllByPatient(patientId);
//        return ResponseEntity.ok(ApiResponse.<List<SocialHistoryDto>>builder()
//                .success(true).message("Social History fetched").data(list).build());
//    }
//
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<SocialHistoryDto>>> getAllByEncounter(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            ) {
//        var list = service.getAllByEncounter(patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<SocialHistoryDto>>builder()
//                .success(true).message("Social History fetched").data(list).build());
//    }
//
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<SocialHistoryDto>> getOne(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            ) {
//        var dto = service.getOne(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<SocialHistoryDto>builder()
//                .success(true).message("Social History fetched").data(dto).build());
//    }
//
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<SocialHistoryDto>> create(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            //            @RequestBody SocialHistoryDto dto) {
//        var created = service.create(patientId, encounterId, dto);
//        return ResponseEntity.ok(ApiResponse.<SocialHistoryDto>builder()
//                .success(true).message("Social History created").data(created).build());
//    }
//
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<SocialHistoryDto>> update(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            //            @RequestBody SocialHistoryDto dto) {
//        var updated = service.update(patientId, encounterId, id, dto);
//        return ResponseEntity.ok(ApiResponse.<SocialHistoryDto>builder()
//                .success(true).message("Social History updated").data(updated).build());
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
//                .success(true).message("Social History deleted").build());
//    }
//}







package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.SocialHistoryDto;
import com.qiaben.ciyex.service.SocialHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/social-history")
@RequiredArgsConstructor
@Slf4j
public class SocialHistoryController {
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<SocialHistoryDto>>> getAllByPatient(@PathVariable Long patientId) {
        try {
            var items = service.getAllByPatient(patientId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<SocialHistoryDto>>builder()
                        .success(true)
                        .message("No Social History found for Patient ID: " + patientId)
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<SocialHistoryDto>>builder()
                    .success(true)
                    .message("Social History fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Social History for Patient ID: " + patientId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<SocialHistoryDto>>builder()
                            .success(false)
                            .message("Error fetching Social History for Patient ID: " + patientId + ". " + ex.getMessage())
                            .build());
        }
    }

    private final SocialHistoryService service;

    // READ (container) — your UI calls this URL to load SH for an encounter. :contentReference[oaicite:1]{index=1}
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<SocialHistoryDto>> get(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        try {
            var dto = service.getOne(patientId, encounterId);
            return ResponseEntity.ok(ApiResponse.<SocialHistoryDto>builder()
                    .success(true).message("Social History fetched successfully").data(dto).build());
        } catch (IllegalArgumentException ex) {
            log.error("Social History not found: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<SocialHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error fetching Social History for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<SocialHistoryDto>builder()
                            .success(false)
                            .message("Error fetching Social History: " + ex.getMessage())
                            .build());
        }
    }

    // READ by ID — get specific social history by ID
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<SocialHistoryDto>> getById(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            var dto = service.getById(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<SocialHistoryDto>builder()
                    .success(true).message("Social History fetched successfully").data(dto).build());
        } catch (IllegalArgumentException ex) {
            log.error("Social History not found: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<SocialHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error fetching Social History for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<SocialHistoryDto>builder()
                            .success(false)
                            .message("Error fetching Social History: " + ex.getMessage())
                            .build());
        }
    }

    // CREATE container (+entries)
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<SocialHistoryDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody SocialHistoryDto dto) {
        var saved = service.create(patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<SocialHistoryDto>builder()
                .success(true).message("Social History created").data(saved).build());
    }

    // UPDATE container (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<SocialHistoryDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody SocialHistoryDto dto) {
        try {
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<SocialHistoryDto>builder()
                    .success(true).message("Social History updated").data(saved).build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.<SocialHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<SocialHistoryDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // DELETE container (423 if signed)
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            service.delete(patientId, encounterId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder().success(false).message(ex.getMessage()).build());
        }
    }

    // ESIGN — optional index/id can be passed via query-string if you prefer "string" inputs
    //   Example: POST .../{id}/esign?index=0
    @PostMapping("/{patientId}/{encounterId}/{id}/esign")
    public ResponseEntity<ApiResponse<SocialHistoryDto>> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestParam(value = "index", required = false) Integer index, // string-friendly
            Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(patientId, encounterId, id, user);
            return ResponseEntity.ok(ApiResponse.<SocialHistoryDto>builder()
                    .success(true).message("Social History e-signed").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<SocialHistoryDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("eSign failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<SocialHistoryDto>builder().success(false).message(ex.getMessage()).build());
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
            String filename = "social-history-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException ex) {
            log.error("Error printing Social History for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error generating Social History PDF", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message("Error generating PDF: " + ex.getMessage()).build());
        }
    }
}
