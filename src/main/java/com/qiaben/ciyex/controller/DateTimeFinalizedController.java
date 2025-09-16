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
//            @RequestHeader("orgId") Long orgId) {
//        var list = service.getAllByPatient(orgId, patientId);
//        return ResponseEntity.ok(ApiResponse.<List<DateTimeFinalizedDto>>builder()
//                .success(true).message("Finalization timestamps fetched").data(list).build());
//    }
//
//    // READ ALL by patient + encounter
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<DateTimeFinalizedDto>>> getAllByEncounter(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @RequestHeader("orgId") Long orgId) {
//        var list = service.getAllByEncounter(orgId, patientId, encounterId);
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
//            @RequestHeader("orgId") Long orgId) {
//        var dto = service.getOne(orgId, patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<DateTimeFinalizedDto>builder()
//                .success(true).message("Finalization timestamp fetched").data(dto).build());
//    }
//
//    // CREATE
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<DateTimeFinalizedDto>> create(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @RequestHeader("orgId") Long orgId,
//            @RequestBody DateTimeFinalizedDto dto) {
//        var created = service.create(orgId, patientId, encounterId, dto);
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
//            @RequestHeader("orgId") Long orgId,
//            @RequestBody DateTimeFinalizedDto dto) {
//        var updated = service.update(orgId, patientId, encounterId, id, dto);
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
//            @RequestHeader("orgId") Long orgId) {
//        service.delete(orgId, patientId, encounterId, id);
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

    private final DateTimeFinalizedService service;

    // LIST
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<DateTimeFinalizedDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {
        var items = service.list(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<DateTimeFinalizedDto>>builder()
                .success(true).message("Finalizations fetched").data(items).build());
    }

    // GET
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<DateTimeFinalizedDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        try {
            var dto = service.getOne(orgId, patientId, encounterId, id);
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
            @RequestHeader("orgId") Long orgId,
            @RequestBody DateTimeFinalizedDto dto) {
        var saved = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<DateTimeFinalizedDto>builder()
                .success(true).message("Finalization created").data(saved).build());
    }

    // UPDATE (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<DateTimeFinalizedDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody DateTimeFinalizedDto dto) {
        try {
            var saved = service.update(orgId, patientId, encounterId, id, dto);
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
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        try {
            service.delete(orgId, patientId, encounterId, id);
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
            @RequestHeader("orgId") Long orgId,
            Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(orgId, patientId, encounterId, id, user);
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
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        byte[] pdf = service.renderPdf(orgId, patientId, encounterId, id);
        String filename = "date-time-finalized-" + id + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
