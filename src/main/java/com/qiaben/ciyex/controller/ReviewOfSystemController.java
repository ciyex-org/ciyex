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
//            @PathVariable Long patientId, @RequestHeader("orgId") Long orgId) {
//        var list = service.getAllByPatient(orgId, patientId);
//        return ResponseEntity.ok(ApiResponse.<List<ReviewOfSystemDto>>builder()
//                .success(true).message("ROS fetched").data(list).build());
//    }
//
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<ReviewOfSystemDto>>> getAllByEncounter(
//            @PathVariable Long patientId, @PathVariable Long encounterId,
//            @RequestHeader("orgId") Long orgId) {
//        var list = service.getAllByEncounter(orgId, patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<ReviewOfSystemDto>>builder()
//                .success(true).message("ROS fetched").data(list).build());
//    }
//
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> getOne(
//            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
//            @RequestHeader("orgId") Long orgId) {
//        var dto = service.getOne(orgId, patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
//                .success(true).message("ROS fetched").data(dto).build());
//    }
//
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> create(
//            @PathVariable Long patientId, @PathVariable Long encounterId,
//            @RequestHeader("orgId") Long orgId, @RequestBody ReviewOfSystemDto dto) {
//        var created = service.create(orgId, patientId, encounterId, dto);
//        return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
//                .success(true).message("ROS created").data(created).build());
//    }
//
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> update(
//            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
//            @RequestHeader("orgId") Long orgId, @RequestBody ReviewOfSystemDto dto) {
//        var updated = service.update(orgId, patientId, encounterId, id, dto);
//        return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
//                .success(true).message("ROS updated").data(updated).build());
//    }
//
//    @DeleteMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(
//            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
//            @RequestHeader("orgId") Long orgId) {
//        service.delete(orgId, patientId, encounterId, id);
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

    private final ReviewOfSystemService service;

    // LIST
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<ReviewOfSystemDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {
        var items = service.list(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<ReviewOfSystemDto>>builder()
                .success(true).message("ROS list fetched").data(items).build());
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        try {
            var dto = service.getOne(orgId, patientId, encounterId, id);
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
            @RequestHeader("orgId") Long orgId,
            @RequestBody ReviewOfSystemDto dto) {
        var saved = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
                .success(true).message("ROS created").data(saved).build());
    }

    // UPDATE (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody ReviewOfSystemDto dto) {
        try {
            var saved = service.update(orgId, patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
                    .success(true).message("ROS updated").data(saved).build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.<ReviewOfSystemDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ReviewOfSystemDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // DELETE (423 if signed)
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        try {
            service.delete(orgId, patientId, encounterId, id);
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
            @RequestHeader("orgId") Long orgId,
            Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(orgId, patientId, encounterId, id, user);
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
    public ResponseEntity<byte[]> print(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        byte[] pdf = service.renderPdf(orgId, patientId, encounterId, id);
        String filename = "ros-" + id + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
