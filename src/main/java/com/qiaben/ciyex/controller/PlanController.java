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

    /**
     * Validates that all mandatory fields are present: diagnosticPlan, plan, sectionsJson, and notes.
     * Throws IllegalArgumentException if any field is missing.
     */
    private void validateMandatoryFields(PlanDto dto) {
        List<String> missingFields = new java.util.ArrayList<>();

        if (dto.diagnosticPlan == null || dto.diagnosticPlan.trim().isEmpty()) {
            missingFields.add("diagnosticPlan");
        }
        if (dto.plan == null || dto.plan.trim().isEmpty()) {
            missingFields.add("plan");
        }
        if (dto.section1 == null || dto.section1.trim().isEmpty()) {
            missingFields.add("section1");
        }
        if (dto.section2 == null || dto.section2.trim().isEmpty()) {
            missingFields.add("section2");
        }
        if (dto.notes == null || dto.notes.trim().isEmpty()) {
            missingFields.add("notes");
        }

        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Missing mandatory fields: " + String.join(", ", missingFields));
        }
    }

    // LIST (used by PlanList)  :contentReference[oaicite:1]{index=1}
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<PlanDto>>> list(
            @PathVariable Long patientId, @PathVariable Long encounterId) {
        try {
            var items = service.list(patientId, encounterId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<PlanDto>>builder()
                        .success(true)
                        .message(String.format("No Plan found for Patient ID: %d, Encounter ID: %d", patientId, encounterId))
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<PlanDto>>builder()
                    .success(true)
                    .message("Plans fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Plans for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<PlanDto>>builder()
                            .success(false)
                            .message(String.format("Error fetching Plans for Patient ID: %d, Encounter ID: %d. %s", patientId, encounterId, ex.getMessage()))
                            .build());
        }
    }

        // GET ALL BY PATIENT
        @GetMapping("/{patientId}")
        public ResponseEntity<ApiResponse<List<PlanDto>>> getAllByPatient(@PathVariable Long patientId) {
            try {
                var items = service.getAllByPatient(patientId);
                if (items.isEmpty()) {
                    return ResponseEntity.ok(ApiResponse.<List<PlanDto>>builder()
                            .success(true)
                            .message("No Plan found for Patient ID: " + patientId)
                            .data(items)
                            .build());
                }
                return ResponseEntity.ok(ApiResponse.<List<PlanDto>>builder()
                        .success(true)
                        .message("Plans fetched successfully")
                        .data(items)
                        .build());
            } catch (Exception ex) {
                log.error("Error fetching Plans for Patient ID: " + patientId, ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.<List<PlanDto>>builder()
                                .success(false)
                                .message("Error fetching Plans for Patient ID: " + patientId + ". " + ex.getMessage())
                                .build());
            }
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
        try {
            validateMandatoryFields(dto);
            var saved = service.create(patientId, encounterId, dto);
            return ResponseEntity.ok(ApiResponse.<PlanDto>builder().success(true).message("Plan created").data(saved).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<PlanDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // UPDATE (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PlanDto>> update(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id, @RequestBody PlanDto dto) {
        try {
            validateMandatoryFields(dto);
            var saved = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<PlanDto>builder().success(true).message("Plan updated").data(saved).build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423).body(ApiResponse.<PlanDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<PlanDto>builder().success(false).message(ex.getMessage()).build());
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
    public ResponseEntity<?> print(@PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id) {
        try {
            byte[] pdf = service.renderPdf(patientId, encounterId, id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"plan-" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException ex) {
            log.error("Error printing Plan for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error generating Plan PDF", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message("Error generating PDF: " + ex.getMessage()).build());
        }
    }
}
