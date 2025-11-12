



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
    var items = service.getAllByPatient(patientId);
    return ResponseEntity.ok(ApiResponse.<List<ProcedureDto>>builder().success(true).message("Fetched").data(items).build());
}

    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<ProcedureDto>>> getAllByEncounter(
            @PathVariable Long patientId, @PathVariable Long encounterId) {
        var list = service.getAllByEncounter(patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<ProcedureDto>>builder()
                .success(true).message("Procedures fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ProcedureDto>> getOne(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id) {
        var dto = service.getOne(patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<ProcedureDto>builder()
                .success(true).message("Procedure fetched").data(dto).build());
    }

    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<ProcedureDto>> create(
            @PathVariable Long patientId, @PathVariable Long encounterId, @RequestBody ProcedureDto dto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(dto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<ProcedureDto>builder()
                    .success(false).message(validationError).build());
        }

        var created = service.create(patientId, encounterId, dto);

        return ResponseEntity.ok(ApiResponse.<ProcedureDto>builder()
                .success(true).message("Procedure created").data(created).build());
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ProcedureDto>> update(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestBody ProcedureDto dto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(dto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<ProcedureDto>builder()
                    .success(false).message(validationError).build());
        }

        var updated = service.update(patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<ProcedureDto>builder()
                .success(true).message("Procedure updated").data(updated).build());
    }

    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id) {
        service.delete(patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Procedure deleted").build());
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
