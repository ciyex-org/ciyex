//package com.qiaben.ciyex.controller;
//
//import com.qiaben.ciyex.dto.ApiResponse;
//import com.qiaben.ciyex.dto.SignoffDto;
//import com.qiaben.ciyex.service.SignoffService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/signoffs")
//@RequiredArgsConstructor
//@Slf4j
//public class SignoffController {
//
//    private final SignoffService service;
//
//    @GetMapping("/{patientId}")
//    public ResponseEntity<ApiResponse<List<SignoffDto>>> getAllByPatient(
//            @PathVariable Long patientId) {
//        var list = service.getAllByPatient(patientId);
//        return ResponseEntity.ok(ApiResponse.<List<SignoffDto>>builder()
//                .success(true).message("Signoffs fetched").data(list).build());
//    }
//
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<SignoffDto>>> getAllByEncounter(
//            @PathVariable Long patientId, @PathVariable Long encounterId) {
//        var list = service.getAllByEncounter(patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<SignoffDto>>builder()
//                .success(true).message("Signoffs fetched").data(list).build());
//    }
//
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<SignoffDto>> getOne(
//            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
//            ) {
//        var dto = service.getOne(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<SignoffDto>builder()
//                .success(true).message("Signoff fetched").data(dto).build());
//    }
//
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<SignoffDto>> create(
//            @PathVariable Long patientId, @PathVariable Long encounterId,
//            //            @RequestBody SignoffDto dto) {
//        var created = service.create(patientId, encounterId, dto);
//        return ResponseEntity.ok(ApiResponse.<SignoffDto>builder()
//                .success(true).message("Signoff created").data(created).build());
//    }
//
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<SignoffDto>> update(
//            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
//            //            @RequestBody SignoffDto dto) {
//        var updated = service.update(patientId, encounterId, id, dto);
//        return ResponseEntity.ok(ApiResponse.<SignoffDto>builder()
//                .success(true).message("Signoff updated").data(updated).build());
//    }
//
//    @DeleteMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(
//            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
//            ) {
//        service.delete(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<Void>builder()
//                .success(true).message("Signoff deleted").build());
//    }
//}





package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.SignoffDto;
import com.qiaben.ciyex.service.SignoffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/signoffs")
@RequiredArgsConstructor
@Slf4j
public class SignoffController {

    private final SignoffService service;

    // LIST (SignoffCard loads here first)
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<SignoffDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        var items = service.list(patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<SignoffDto>>builder()
                .success(true).message("Sign-offs fetched").data(items).build());
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<SignoffDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            var dto = service.getOne(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<SignoffDto>builder()
                    .success(true).message("Sign-off fetched").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<SignoffDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // CREATE draft (Card may call this when none exists)
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<SignoffDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody(required = false) SignoffDto dto) {
        var saved = service.create(patientId, encounterId, dto != null ? dto : new SignoffDto());
        return ResponseEntity.ok(ApiResponse.<SignoffDto>builder()
                .success(true).message("Sign-off created").data(saved).build());
    }

    // UPDATE (423 if signed/locked)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<SignoffDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody SignoffDto dto) {
        try {
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<SignoffDto>builder()
                    .success(true).message("Sign-off updated").data(saved).build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.<SignoffDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<SignoffDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // DELETE (SignoffCard expects 204 to short-circuit)  :contentReference[oaicite:1]{index=1}
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

    // ESIGN (POST with no body in the Card)  :contentReference[oaicite:2]{index=2}
    @PostMapping("/{patientId}/{encounterId}/{id}/esign")
    public ResponseEntity<ApiResponse<SignoffDto>> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(patientId, encounterId, id, user);
            return ResponseEntity.ok(ApiResponse.<SignoffDto>builder()
                    .success(true).message("Sign-off e-signed").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<SignoffDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("eSign failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<SignoffDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // PRINT (PDF)
    @GetMapping("/{patientId}/{encounterId}/{id}/print")
    public ResponseEntity<byte[]> print(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        byte[] pdf = service.renderPdf(patientId, encounterId, id);
        String filename = "signoff-" + id + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
