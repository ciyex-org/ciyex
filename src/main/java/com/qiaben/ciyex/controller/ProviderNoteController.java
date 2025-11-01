//package com.qiaben.ciyex.controller;
//
//import com.qiaben.ciyex.dto.ApiResponse;
//import com.qiaben.ciyex.dto.ProviderNoteDto;
//import com.qiaben.ciyex.service.ProviderNoteService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/provider-notes")
//@RequiredArgsConstructor
//@Slf4j
//public class ProviderNoteController {
//
//    private final ProviderNoteService service;
//
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<ProviderNoteDto>>> listByEncounter(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            ) {
//
//        var list = service.getAllByEncounter(patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<ProviderNoteDto>>builder()
//                .success(true).message("Provider notes fetched").data(list).build());
//    }
//
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<ProviderNoteDto>> getOne(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            ) {
//
//        var dto = service.getOne(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<ProviderNoteDto>builder()
//                .success(true).message("Provider note fetched").data(dto).build());
//    }
//
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<ProviderNoteDto>> create(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            //            @RequestBody ProviderNoteDto dto) {
//
//        var saved = service.create(patientId, encounterId, dto);
//        return ResponseEntity.ok(ApiResponse.<ProviderNoteDto>builder()
//                .success(true).message("Provider note created").data(saved).build());
//    }
//
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<ProviderNoteDto>> update(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            //            @RequestBody ProviderNoteDto dto) {
//
//        var updated = service.update(patientId, encounterId, id, dto);
//        return ResponseEntity.ok(ApiResponse.<ProviderNoteDto>builder()
//                .success(true).message("Provider note updated").data(updated).build());
//    }
//
//    @DeleteMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            ) {
//
//        service.delete(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<Void>builder()
//                .success(true).message("Provider note deleted").build());
//    }
//}




package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ProviderNoteDto;
import com.qiaben.ciyex.service.ProviderNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/provider-notes")
@RequiredArgsConstructor
@Slf4j
public class ProviderNoteController {
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<ProviderNoteDto>>> getAllByPatient(@PathVariable Long patientId) {
        var items = service.getAllByPatient(patientId);
        return ResponseEntity.ok(ApiResponse.<List<ProviderNoteDto>>builder().success(true).message("Fetched").data(items).build());
    }

    private final ProviderNoteService service;

    // LIST
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<ProviderNoteDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        var items = service.list(patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<ProviderNoteDto>>builder()
                .success(true).message("Provider notes fetched").data(items).build());
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ProviderNoteDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            var dto = service.getOne(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<ProviderNoteDto>builder()
                    .success(true).message("Provider note fetched").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ProviderNoteDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<ProviderNoteDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody ProviderNoteDto dto) {
        var saved = service.create(patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<ProviderNoteDto>builder()
                .success(true).message("Provider note created").data(saved).build());
    }

    // UPDATE (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ProviderNoteDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody ProviderNoteDto dto) {
        try {
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<ProviderNoteDto>builder()
                    .success(true).message("Provider note updated").data(saved).build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.<ProviderNoteDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ProviderNoteDto>builder().success(false).message(ex.getMessage()).build());
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
            // 204 keeps your UI happy (it handles empty body). :contentReference[oaicite:1]{index=1}
            return ResponseEntity.noContent().build();
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
    public ResponseEntity<ApiResponse<ProviderNoteDto>> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(patientId, encounterId, id, user);
            return ResponseEntity.ok(ApiResponse.<ProviderNoteDto>builder()
                    .success(true).message("Provider note e-signed").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ProviderNoteDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("eSign failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<ProviderNoteDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // PRINT (PDF)
    @GetMapping("/{patientId}/{encounterId}/{id}/print")
    public ResponseEntity<byte[]> print(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        byte[] pdf = service.renderPdf(patientId, encounterId, id);
        String filename = "provider-note-" + id + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
