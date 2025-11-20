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
        try {
            var items = service.getAllByPatient(patientId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<ProviderSignatureDto>>builder()
                        .success(true)
                        .message("No Provider Signatures found for Patient ID: " + patientId)
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<ProviderSignatureDto>>builder()
                    .success(true)
                    .message("Provider Signatures fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Provider Signatures for Patient ID: " + patientId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<ProviderSignatureDto>>builder()
                            .success(false)
                            .message("Error fetching Provider Signatures for Patient ID: " + patientId + ". " + ex.getMessage())
                            .build());
        }
    }

    private final ProviderSignatureService service;

    /**
     * Validates that all mandatory fields are present: signedBy, signerRole, signatureFormat, and comments.
     * Throws IllegalArgumentException if any field is missing.
     */
    private void validateMandatoryFields(ProviderSignatureDto dto) {
        List<String> missingFields = new java.util.ArrayList<>();

        if (dto.getSignedBy() == null || dto.getSignedBy().trim().isEmpty()) {
            missingFields.add("signedBy");
        }
        if (dto.getSignerRole() == null || dto.getSignerRole().trim().isEmpty()) {
            missingFields.add("signerRole");
        }
        if (dto.getSignatureFormat() == null || dto.getSignatureFormat().trim().isEmpty()) {
            missingFields.add("signatureFormat");
        }
        if (dto.getComments() == null || dto.getComments().trim().isEmpty()) {
            missingFields.add("comments");
        }

        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Missing mandatory fields: " + String.join(", ", missingFields));
        }
    }

    // LIST (card loads latest from this list)
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<ProviderSignatureDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        try {
            var items = service.list(patientId, encounterId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<ProviderSignatureDto>>builder()
                        .success(true)
                        .message(String.format("No Provider Signatures found for Patient ID: %d, Encounter ID: %d", patientId, encounterId))
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<ProviderSignatureDto>>builder()
                    .success(true)
                    .message("Provider signatures fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Provider Signatures for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<ProviderSignatureDto>>builder()
                            .success(false)
                            .message(String.format("Error fetching Provider Signatures for Patient ID: %d, Encounter ID: %d. %s", patientId, encounterId, ex.getMessage()))
                            .build());
        }
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
        try {
            validateMandatoryFields(dto);
            var saved = service.create(patientId, encounterId, dto);
            return ResponseEntity.ok(ApiResponse.<ProviderSignatureDto>builder()
                    .success(true).message("Provider signature saved").data(saved).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<ProviderSignatureDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // eSign alias endpoint (same as create)
    @PostMapping("/{patientId}/{encounterId}/{id}/esign")
    public ResponseEntity<ApiResponse<ProviderSignatureDto>> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody ProviderSignatureDto dto) {
        try {
            validateMandatoryFields(dto);
            var saved = service.eSign(patientId, encounterId, dto);
            return ResponseEntity.ok(ApiResponse.<ProviderSignatureDto>builder()
                    .success(true).message("Provider signature e-signed").data(saved).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<ProviderSignatureDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // UPDATE
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ProviderSignatureDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody ProviderSignatureDto dto) {
        try {
            validateMandatoryFields(dto);
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<ProviderSignatureDto>builder()
                    .success(true).message("Provider signature updated").data(saved).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
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
    public ResponseEntity<?> print(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            byte[] pdf = service.renderPdf(patientId, encounterId, id);
            String filename = "provider-signature-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException ex) {
            log.error("Error printing Provider Signature for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error generating Provider Signature PDF", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message("Error generating PDF: " + ex.getMessage()).build());
        }
    }
}
