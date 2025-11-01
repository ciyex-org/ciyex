//package com.qiaben.ciyex.controller;
//
//import com.qiaben.ciyex.dto.ApiResponse;
//import com.qiaben.ciyex.dto.ProviderSignatureDto;
//import com.qiaben.ciyex.service.ProviderSignatureService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/provider-signatures")
//@RequiredArgsConstructor
//@Slf4j
//public class ProviderSignatureController {
//
//    private final ProviderSignatureService service;
//
//    // READ ALL by patient
//    @GetMapping("/{patientId}")
//    public ResponseEntity<ApiResponse<List<ProviderSignatureDto>>> getAllByPatient(
//            @PathVariable Long patientId,
//            ) {
//        var list = service.getAllByPatient(patientId);
//        return ResponseEntity.ok(ApiResponse.<List<ProviderSignatureDto>>builder()
//                .success(true).message("Provider signatures fetched").data(list).build());
//    }
//
//    // READ ALL by patient + encounter
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<ProviderSignatureDto>>> getAllByEncounter(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            ) {
//        var list = service.getAllByEncounter(patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<ProviderSignatureDto>>builder()
//                .success(true).message("Provider signatures fetched").data(list).build());
//    }
//
//    // READ ONE
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<ProviderSignatureDto>> getOne(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            ) {
//        var dto = service.getOne(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<ProviderSignatureDto>builder()
//                .success(true).message("Provider signature fetched").data(dto).build());
//    }
//
//    // CREATE
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<ProviderSignatureDto>> create(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            //            @RequestBody ProviderSignatureDto dto) {
//        var created = service.create(patientId, encounterId, dto);
//        return ResponseEntity.ok(ApiResponse.<ProviderSignatureDto>builder()
//                .success(true).message("Provider signature created").data(created).build());
//    }
//
//    // UPDATE
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<ProviderSignatureDto>> update(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            //            @RequestBody ProviderSignatureDto dto) {
//        var updated = service.update(patientId, encounterId, id, dto);
//        return ResponseEntity.ok(ApiResponse.<ProviderSignatureDto>builder()
//                .success(true).message("Provider signature updated").data(updated).build());
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
//                .success(true).message("Provider signature deleted").build());
//    }
//}



package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ProviderSignatureDto;
import com.qiaben.ciyex.service.ProviderSignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider-signatures")
@RequiredArgsConstructor
@Slf4j
public class ProviderSignatureController {
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<ProviderSignatureDto>>> getAllByPatient(@PathVariable Long patientId) {
        var items = service.getAllByPatient(patientId);
        return ResponseEntity.ok(ApiResponse.<List<ProviderSignatureDto>>builder().success(true).message("Fetched").data(items).build());
    }

    private final ProviderSignatureService service;

    // LIST (card loads latest from this list)
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<ProviderSignatureDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        var items = service.list(patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<ProviderSignatureDto>>builder()
                .success(true).message("Provider signatures fetched").data(items).build());
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ProviderSignatureDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            var dto = service.getOne(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<ProviderSignatureDto>builder()
                    .success(true).message("Provider signature fetched").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ProviderSignatureDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    
    
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<ProviderSignatureDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody ProviderSignatureDto dto) {
        var saved = service.create(patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<ProviderSignatureDto>builder()
                .success(true).message("Provider signature saved").data(saved).build());
    }

    // eSign alias endpoint (same as create)
    @PostMapping("/{patientId}/{encounterId}/{id}/esign")
    public ResponseEntity<ApiResponse<ProviderSignatureDto>> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody ProviderSignatureDto dto) {
        var saved = service.eSign(patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<ProviderSignatureDto>builder()
                .success(true).message("Provider signature e-signed").data(saved).build());
    }

    // UPDATE
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ProviderSignatureDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody ProviderSignatureDto dto) {
        try {
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<ProviderSignatureDto>builder()
                    .success(true).message("Provider signature updated").data(saved).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ProviderSignatureDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // DELETE (Card expects plain 2xx; 204 is fine)
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            service.delete(patientId, encounterId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder().success(false).message(ex.getMessage()).build());
        }
    }

    // PRINT (PDF)
    @GetMapping("/{patientId}/{encounterId}/{id}/print")
    public ResponseEntity<byte[]> print(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        byte[] pdf = service.renderPdf(patientId, encounterId, id);
        String filename = "provider-signature-" + id + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
