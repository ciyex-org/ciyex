//package com.qiaben.ciyex.controller;
//
//import com.qiaben.ciyex.dto.ApiResponse;
//import com.qiaben.ciyex.dto.PlanDto;
//import com.qiaben.ciyex.service.PlanService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/plan")
//@RequiredArgsConstructor
//@Slf4j
//public class PlanController {
//
//    private final PlanService service;
//
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<PlanDto>> create(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @RequestHeader("orgId") Long orgId,
//            @RequestBody PlanDto dto
//    ) {
//        PlanDto out = service.create(orgId, patientId, encounterId, dto);
//        return ResponseEntity.ok(ApiResponse.<PlanDto>builder()
//                .success(true)
//                .message("Plan created")
//                .data(out)
//                .build());
//    }
//
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<PlanDto>> update(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            @RequestHeader("orgId") Long orgId,
//            @RequestBody PlanDto dto
//    ) {
//        PlanDto out = service.update(orgId, patientId, encounterId, id, dto);
//        return ResponseEntity.ok(ApiResponse.<PlanDto>builder()
//                .success(true)
//                .message("Plan updated")
//                .data(out).build());
//    }
//
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<PlanDto>> getOne(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            @RequestHeader("orgId") Long orgId
//    ) {
//        PlanDto out = service.getOne(orgId, patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<PlanDto>builder()
//                .success(true).message("Plan fetched").data(out).build());
//    }
//
//    @GetMapping("/{patientId}")
//    public ResponseEntity<ApiResponse<List<PlanDto>>> getAllByPatient(
//            @PathVariable Long patientId,
//            @RequestHeader("orgId") Long orgId
//    ) {
//        List<PlanDto> out = service.getAllByPatient(orgId, patientId);
//        return ResponseEntity.ok(ApiResponse.<List<PlanDto>>builder()
//                .success(true).message("Plans fetched").data(out).build());
//    }
//
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<PlanDto>>> getAllByEncounter(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @RequestHeader("orgId") Long orgId
//    ) {
//        List<PlanDto> out = service.getAllByEncounter(orgId, patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<PlanDto>>builder()
//                .success(true).message("Plans fetched").data(out).build());
//    }
//
//    @DeleteMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            @RequestHeader("orgId") Long orgId
//    ) {
//        service.delete(orgId, patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<Void>builder()
//                .success(true).message("Plan deleted").build());
//    }
//}




package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PlanDto;
import com.qiaben.ciyex.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
@Slf4j
public class PlanController {

    private final PlanService service;

    // LIST (used by PlanList)  :contentReference[oaicite:1]{index=1}
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<PlanDto>>> list(
            @PathVariable Long patientId, @PathVariable Long encounterId, @RequestHeader("orgId") Long orgId) {
        var items = service.list(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<PlanDto>>builder().success(true).message("Plans fetched").data(items).build());
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PlanDto>> getOne(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id, @RequestHeader("orgId") Long orgId) {
        try {
            var dto = service.getOne(orgId, patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<PlanDto>builder().success(true).message("Plan fetched").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<PlanDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<PlanDto>> create(
            @PathVariable Long patientId, @PathVariable Long encounterId, @RequestHeader("orgId") Long orgId, @RequestBody PlanDto dto) {
        var saved = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<PlanDto>builder().success(true).message("Plan created").data(saved).build());
    }

    // UPDATE (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PlanDto>> update(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id, @RequestHeader("orgId") Long orgId, @RequestBody PlanDto dto) {
        try {
            var saved = service.update(orgId, patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<PlanDto>builder().success(true).message("Plan updated").data(saved).build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423).body(ApiResponse.<PlanDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<PlanDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // DELETE (PlanList tolerates 204 or JSON)  :contentReference[oaicite:2]{index=2}
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<?> delete(@PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id, @RequestHeader("orgId") Long orgId) {
        try {
            service.delete(orgId, patientId, encounterId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423).body(ApiResponse.builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.builder().success(false).message(ex.getMessage()).build());
        }
    }

    // ESIGN (no body)
    @PostMapping("/{patientId}/{encounterId}/{id}/esign")
    public ResponseEntity<ApiResponse<PlanDto>> eSign(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id, @RequestHeader("orgId") Long orgId, Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(orgId, patientId, encounterId, id, user);
            return ResponseEntity.ok(ApiResponse.<PlanDto>builder().success(true).message("Plan e-signed").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<PlanDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // PRINT (PDF)
    @GetMapping("/{patientId}/{encounterId}/{id}/print")
    public ResponseEntity<byte[]> print(@PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id, @RequestHeader("orgId") Long orgId) {
        byte[] pdf = service.renderPdf(orgId, patientId, encounterId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"plan-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
