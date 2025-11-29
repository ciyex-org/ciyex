package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ProcedureDto;
import com.qiaben.ciyex.service.ProcedureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedures")
@RequiredArgsConstructor
@Slf4j
public class ProcedureController {


    private final ProcedureService service;

//    @GetMapping("/{patientId}")
//    public ResponseEntity<ApiResponse<List<ProcedureDto>>> getAllByPatient(
//            @PathVariable Long patientId) {
//        var list = service.getAllByPatient(patientId);
//        return ResponseEntity.ok(ApiResponse.<List<ProcedureDto>>builder()
//                .success(true).message("Procedures fetched").data(list).build());
//    }
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<ProcedureDto>>> getAllByPatient(@PathVariable Long patientId) {
        try {
            var items = service.getAllByPatient(patientId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<ProcedureDto>>builder()
                        .success(true)
                        .message("No Procedures found for Patient ID: " + patientId)
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<ProcedureDto>>builder()
                    .success(true)
                    .message("Procedures fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Procedures for Patient ID: " + patientId, ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<ProcedureDto>>builder()
                            .success(false)
                            .message("Error fetching Procedures for Patient ID: " + patientId + ". " + ex.getMessage())
                            .build());
        }
    }

    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<ProcedureDto>>> getAllByEncounter(
            @PathVariable Long patientId, @PathVariable Long encounterId) {
        try {
            var list = service.getAllByEncounter(patientId, encounterId);
            if (list.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<ProcedureDto>>builder()
                        .success(true)
                        .message(String.format("No Procedures found for Patient ID: %d, Encounter ID: %d", patientId, encounterId))
                        .data(list)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<ProcedureDto>>builder()
                    .success(true)
                    .message("Procedures fetched successfully")
                    .data(list)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Procedures for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<ProcedureDto>>builder()
                            .success(false)
                            .message(String.format("Error fetching Procedures for Patient ID: %d, Encounter ID: %d. %s", patientId, encounterId, ex.getMessage()))
                            .build());
        }
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ProcedureDto>> getOne(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id) {
        try {
            var dto = service.getOne(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<ProcedureDto>builder()
                    .success(true)
                    .message("Procedure fetched successfully")
                    .data(dto)
                    .build());
        } catch (IllegalArgumentException ex) {
            log.error("Error fetching Procedure: " + ex.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ProcedureDto>builder()
                            .success(false)
                            .message(ex.getMessage())
                            .build());
        } catch (Exception ex) {
            log.error("Error fetching Procedure for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<ProcedureDto>builder()
                            .success(false)
                            .message("Error fetching Procedure: " + ex.getMessage())
                            .build());
        }
    }

    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<ProcedureDto>> create(
            @PathVariable Long patientId, @PathVariable Long encounterId, @RequestBody ProcedureDto dto) {
        try {
            // Validate mandatory fields
            String validationError = validateMandatoryFields(dto);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(ApiResponse.<ProcedureDto>builder()
                        .success(false).message(validationError).build());
            }
            var created = service.create(patientId, encounterId, dto);
            return ResponseEntity.ok(ApiResponse.<ProcedureDto>builder()
                    .success(true).message("Procedure created").data(created).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<ProcedureDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.LOCKED)
                    .body(ApiResponse.<ProcedureDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error creating Procedure for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<ProcedureDto>builder().success(false).message("Error creating Procedure: " + ex.getMessage()).build());
        }
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ProcedureDto>> update(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestBody ProcedureDto dto) {
        try {
            // Validate mandatory fields
            String validationError = validateMandatoryFields(dto);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(ApiResponse.<ProcedureDto>builder()
                        .success(false).message(validationError).build());
            }

            var updated = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<ProcedureDto>builder()
                    .success(true)
                    .message("Procedure updated successfully")
                    .data(updated)
                    .build());
        } catch (IllegalArgumentException ex) {
            log.error("Error updating Procedure: " + ex.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ProcedureDto>builder()
                            .success(false)
                            .message(ex.getMessage())
                            .build());
        } catch (Exception ex) {
            log.error("Error updating Procedure for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<ProcedureDto>builder()
                            .success(false)
                            .message("Error updating Procedure: " + ex.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id) {
        try {
            service.delete(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Procedure deleted successfully")
                    .build());
        } catch (IllegalArgumentException ex) {
            log.error("Error deleting Procedure: " + ex.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message(ex.getMessage())
                            .build());
        } catch (Exception ex) {
            log.error("Error deleting Procedure for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Error deleting Procedure: " + ex.getMessage())
                            .build());
        }
    }

    /**
     * Validates mandatory fields: cpt4, rate, and units
     * @param dto ProcedureDto to validate
     * @return error message if validation fails, null if validation passes
     */
    private String validateMandatoryFields(ProcedureDto dto) {
        StringBuilder missingFields = new StringBuilder();

        if (dto.getCpt4() == null || dto.getCpt4().trim().isEmpty()) {
            missingFields.append("cpt4, ");
        }

        if (dto.getRate() == null || dto.getRate().trim().isEmpty()) {
            missingFields.append("rate, ");
        }

        if (dto.getUnits() == null) {
            missingFields.append("units, ");
        }

        if (!missingFields.isEmpty()) {
            // Remove the trailing comma and space
            missingFields.setLength(missingFields.length() - 2);
            return "Missing mandatory fields: " + missingFields;
        }

        return null;
    }
}
