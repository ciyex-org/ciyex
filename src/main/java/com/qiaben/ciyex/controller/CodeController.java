//package com.qiaben.ciyex.controller;
//
//import com.qiaben.ciyex.dto.ApiResponse;
//import com.qiaben.ciyex.dto.CodeDto;
//import com.qiaben.ciyex.service.CodeService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/codes")
//@RequiredArgsConstructor
//@Slf4j
//public class CodeController {
//
//    private final CodeService service;
//
//    // READ ALL by patient
//    @GetMapping("/{patientId}")
//    public ResponseEntity<ApiResponse<List<CodeDto>>> getAllByPatient(
//            @PathVariable Long patientId) {
//        var list = service.getAllByPatient(patientId);
//        return ResponseEntity.ok(ApiResponse.<List<CodeDto>>builder()
//                .success(true).message("Codes fetched").data(list).build());
//    }
//
//    // READ ALL by encounter
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<CodeDto>>> getAllByEncounter(
//            @PathVariable Long patientId, @PathVariable Long encounterId) {
//        var list = service.getAllByEncounter(patientId, encounterId);
//        return ResponseEntity.ok(ApiResponse.<List<CodeDto>>builder()
//                .success(true).message("Codes fetched").data(list).build());
//    }
//
//    // READ ONE
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<CodeDto>> getOne(
//            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
//            ) {
//        var dto = service.getOne(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<CodeDto>builder()
//                .success(true).message("Code fetched").data(dto).build());
//    }
//
//    // CREATE
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<CodeDto>> create(
//            @PathVariable Long patientId, @PathVariable Long encounterId,
//            //            @RequestBody CodeDto dto) {
//        var created = service.create(patientId, encounterId, dto);
//        return ResponseEntity.ok(ApiResponse.<CodeDto>builder()
//                .success(true).message("Code created").data(created).build());
//    }
//
//    // UPDATE
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<CodeDto>> update(
//            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
//            //            @RequestBody CodeDto dto) {
//        var updated = service.update(patientId, encounterId, id, dto);
//        return ResponseEntity.ok(ApiResponse.<CodeDto>builder()
//                .success(true).message("Code updated").data(updated).build());
//    }
//
//    // DELETE
//    @DeleteMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(
//            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
//            ) {
//        service.delete(patientId, encounterId, id);
//        return ResponseEntity.ok(ApiResponse.<Void>builder()
//                .success(true).message("Code deleted").build());
//    }
//
//    // FILTER by type (in encounter)
//    @GetMapping("/{patientId}/{encounterId}/type/{codeType}")
//    public ResponseEntity<ApiResponse<List<CodeDto>>> listByType(
//            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable String codeType,
//            //            @RequestParam(value = "active", required = false) Boolean active) {
//        var list = service.searchInEncounter(patientId, encounterId, codeType, active, "");
//        return ResponseEntity.ok(ApiResponse.<List<CodeDto>>builder()
//                .success(true).message("Codes filtered").data(list).build());
//    }
//
//    // SEARCH (in encounter)
//    @GetMapping("/{patientId}/{encounterId}/search")
//    public ResponseEntity<ApiResponse<List<CodeDto>>> search(
//            @PathVariable Long patientId, @PathVariable Long encounterId,
//            //            @RequestParam(value = "q", required = false, defaultValue = "") String q,
//            @RequestParam(value = "codeType", required = false) String codeType,
//            @RequestParam(value = "active", required = false) Boolean active) {
//        var list = service.searchInEncounter(patientId, encounterId, codeType, active, q);
//        return ResponseEntity.ok(ApiResponse.<List<CodeDto>>builder()
//                .success(true).message("Codes search results").data(list).build());
//    }
//}






package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.CodeDto;

import com.qiaben.ciyex.service.CodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;



@RestController
@RequestMapping("/api/codes")
@RequiredArgsConstructor
@Slf4j
public class CodeController {

    private final CodeService service;

   
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<CodeDto>>> getAllByPatient(@PathVariable Long patientId) {
        var list = service.getAllByPatient(patientId);
        return ResponseEntity.ok(ApiResponse.<List<CodeDto>>builder()
                .success(true).message("Codes fetched").data(list).build());
    }

    
    // LIST
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<CodeDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        var items = service.list(patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<CodeDto>>builder()
                .success(true).message("Codes fetched").data(items).build());
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<CodeDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id) {
        try {
            var dto = service.getOne(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<CodeDto>builder()
                    .success(true).message("Code fetched").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<CodeDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<CodeDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody CodeDto dto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(dto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<CodeDto>builder()
                    .success(false).message(validationError).build());
        }

        try {
            var saved = service.create(patientId, encounterId, dto);
            return ResponseEntity.ok(ApiResponse.<CodeDto>builder()
                    .success(true).message("Code created").data(saved).build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error during Code creation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<CodeDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            log.error("Business rule violation during Code creation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(ApiResponse.<CodeDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error creating Code for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<CodeDto>builder()
                            .success(false)
                            .message("Error creating Code: " + ex.getMessage())
                            .build());
        }
    }

    // UPDATE (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<CodeDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id,
            @RequestBody CodeDto dto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(dto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<CodeDto>builder()
                    .success(false).message(validationError).build());
        }

        try {
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<CodeDto>builder()
                    .success(true).message("Code updated").data(saved).build());
        } catch (IllegalArgumentException ex) {
            log.error("Validation error during Code update: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<CodeDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            log.error("Business rule violation during Code update: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED) // 423 LOCKED
                    .body(ApiResponse.<CodeDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error updating Code for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<CodeDto>builder()
                            .success(false)
                            .message("Error updating Code: " + ex.getMessage())
                            .build());
        }
    }

    // DELETE (423 if signed)
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id) {
        try {
            service.delete(patientId, encounterId, id);
            // Your UI tolerates empty 204 via safeJson() fallback. :contentReference[oaicite:1]{index=1}
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder().success(false).message(ex.getMessage()).build());
        }
    }

    // ESIGN (no JSON body)
    @PostMapping("/{patientId}/{encounterId}/{id}/esign")
    public ResponseEntity<ApiResponse<CodeDto>> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id,
            Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(patientId, encounterId, id, user);
            return ResponseEntity.ok(ApiResponse.<CodeDto>builder()
                    .success(true).message("Code e-signed").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<CodeDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("eSign failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<CodeDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // PRINT (PDF)
    @GetMapping("/{patientId}/{encounterId}/{id}/print")
    public ResponseEntity<byte[]> print(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id) {
        byte[] pdf = service.renderPdf(patientId, encounterId, id);
        String filename = "code-" + id + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Validates mandatory fields for Code creation and update
     * @param dto CodeDto to validate
     * @return error message if validation fails, null if validation passes
     */
    private String validateMandatoryFields(CodeDto dto) {
        StringBuilder missingFields = new StringBuilder();

        if (dto.getCodeType() == null || dto.getCodeType().trim().isEmpty()) {
            missingFields.append("codeType, ");
        }

        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            missingFields.append("code, ");
        }

        if (dto.getShortDescription() == null || dto.getShortDescription().trim().isEmpty()) {
            missingFields.append("shortDescription, ");
        }

        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            missingFields.append("category, ");
        }

        if (dto.getFeeStandard() == null) {
            missingFields.append("feeStandard, ");
        }

        if (!missingFields.isEmpty()) {
            // Remove the trailing ", "
            missingFields.setLength(missingFields.length() - 2);
            return "Missing mandatory fields: " + missingFields.toString();
        }

        return null;
    }
}
