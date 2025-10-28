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
//            //            @RequestBody PlanDto dto
//    ) {
//        PlanDto out = service.create(patientId, encounterId, dto);
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
//            //            @RequestBody PlanDto dto
//    ) {
//        PlanDto out = service.update(patientId, encounterId, id, dto);
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
//            
//    ) {
//        PlanDto out = service.getOne(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<PlanDto>builder()
//                .success(true).message("Plan fetched").data(out).build());
//    }
//
//    @GetMapping("/{patientId}")
//    public ResponseEntity<ApiResponse<List<PlanDto>>> getAllByPatient(
//            @PathVariable Long patientId,
//            
//    ) {
//        List<PlanDto> out = service.getAllByPatient(patientId);
//        return ResponseEntity.ok(ApiResponse.<List<PlanDto>>builder()
//                .success(true).message("Plans fetched").data(out).build());
//    }
//
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<PlanDto>>> getAllByEncounter(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            
//    ) {
//        List<PlanDto> out = service.getAllByEncounter(patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<PlanDto>>builder()
//                .success(true).message("Plans fetched").data(out).build());
//    }
//
//    @DeleteMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            
//    ) {
//        service.delete(patientId, encounterId, id);
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
            @PathVariable Long patientId, @PathVariable Long encounterId) {
        var items = service.list(patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<PlanDto>>builder().success(true).message("Plans fetched").data(items).build());
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PlanDto>> getOne(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id) {
        try {
            var dto = service.getOne(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<PlanDto>builder().success(true).message("Plan fetched").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<PlanDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<PlanDto>> create(
            @PathVariable Long patientId, @PathVariable Long encounterId, @RequestBody PlanDto dto) {
        var saved = service.create(patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<PlanDto>builder().success(true).message("Plan created").data(saved).build());
    }

    // UPDATE (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PlanDto>> update(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id, @RequestBody PlanDto dto) {
        try {
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<PlanDto>builder().success(true).message("Plan updated").data(saved).build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423).body(ApiResponse.<PlanDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<PlanDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // DELETE (PlanList tolerates 204 or JSON)  :contentReference[oaicite:2]{index=2}
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<?> delete(@PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id) {
        try {
            service.delete(patientId, encounterId, id);
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
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id, Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(patientId, encounterId, id, user);
            return ResponseEntity.ok(ApiResponse.<PlanDto>builder().success(true).message("Plan e-signed").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<PlanDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // PRINT (PDF)
    @GetMapping("/{patientId}/{encounterId}/{id}/print")
    public ResponseEntity<byte[]> print(@PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id) {
        byte[] pdf = service.renderPdf(patientId, encounterId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"plan-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
