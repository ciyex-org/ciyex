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
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<SignoffDto>>> getAllByPatient(@PathVariable Long patientId) {
        try {
            var items = service.getAllByPatient(patientId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<SignoffDto>>builder()
                        .success(true)
                        .message("No Sign-offs found for Patient ID: " + patientId)
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<SignoffDto>>builder()
                    .success(true)
                    .message("Sign-offs fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Sign-offs for Patient ID: " + patientId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<SignoffDto>>builder()
                            .success(false)
                            .message("Error fetching Sign-offs for Patient ID: " + patientId + ". " + ex.getMessage())
                            .build());
        }
    }

    private final SignoffService service;

    /**
     * Validates mandatory fields for Signoff creation and update
     * @param dto SignoffDto to validate
     * @return error message if validation fails, null if validation passes
     */
    private String validateMandatoryFields(SignoffDto dto) {
        StringBuilder missingFields = new StringBuilder();

        if (dto.getAttestationText() == null || dto.getAttestationText().trim().isEmpty()) {
            missingFields.append("attestationText, ");
        }

        if (dto.getComments() == null || dto.getComments().trim().isEmpty()) {
            missingFields.append("comments, ");
        }

        if (!missingFields.isEmpty()) {
            // Remove the trailing comma and space
            missingFields.setLength(missingFields.length() - 2);
            return "Missing mandatory fields: " + missingFields;
        }

        return null;
    }

    // LIST (SignoffCard loads here first)
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<SignoffDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        try {
            var items = service.list(patientId, encounterId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<SignoffDto>>builder()
                        .success(true)
                        .message(String.format("No Sign-offs found for Patient ID: %d, Encounter ID: %d", patientId, encounterId))
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<SignoffDto>>builder()
                    .success(true)
                    .message("Sign-offs fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Sign-offs for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<SignoffDto>>builder()
                            .success(false)
                            .message(String.format("Error fetching Sign-offs for Patient ID: %d, Encounter ID: %d. %s", patientId, encounterId, ex.getMessage()))
                            .build());
        }
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<SignoffDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id) {
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
        SignoffDto signoffDto = (dto != null ? dto : new SignoffDto());

        // Validate mandatory fields
        String validationError = validateMandatoryFields(signoffDto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<SignoffDto>builder()
                    .success(false).message(validationError).build());
        }

        try {
            var saved = service.create(patientId, encounterId, signoffDto);
            return ResponseEntity.ok(ApiResponse.<SignoffDto>builder()
                    .success(true).message("Sign-off created").data(saved).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<SignoffDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // UPDATE (423 if signed/locked)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<SignoffDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id,
            @RequestBody SignoffDto dto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(dto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<SignoffDto>builder()
                    .success(false).message(validationError).build());
        }

        try {
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<SignoffDto>builder()
                    .success(true).message("Sign-off updated").data(saved).build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.<SignoffDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<SignoffDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // DELETE (SignoffCard expects 204 to short-circuit)  :contentReference[oaicite:1]{index=1}
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id) {
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
            @PathVariable String id,
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
    public ResponseEntity<?> print(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id) {
        try {
            byte[] pdf = service.renderPdf(patientId, encounterId, id);
            String filename = "signoff-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException ex) {
            log.error("Error printing Sign-off for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error generating Sign-off PDF", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message("Error generating PDF: " + ex.getMessage()).build());
        }
    }
}
