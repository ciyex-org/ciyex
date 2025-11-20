package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PatientCodeListDto;
import com.qiaben.ciyex.service.PatientCodeListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/patient-codes")
@RequiredArgsConstructor
@Slf4j
public class PatientCodeListController {

    private final PatientCodeListService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientCodeListDto>>> list() {
        try {
            List<PatientCodeListDto> data = service.findAll();
            return ResponseEntity.ok(ApiResponse.<List<PatientCodeListDto>>builder()
                    .success(true).message("Patient code lists retrieved successfully").data(data).build());
        } catch (Exception e) {
            log.error("Failed to list patient code lists", e);
            return ResponseEntity.ok(ApiResponse.<List<PatientCodeListDto>>builder()
                    .success(false).message("Failed to list patient code lists: " + e.getMessage()).build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientCodeListDto>> get(
            @PathVariable Long id) {
        try {
            PatientCodeListDto dto = service.getById(id);
            if (dto == null) {
                return ResponseEntity.ok(ApiResponse.<PatientCodeListDto>builder()
                        .success(false).message("Patient code list not found with id: " + id).build());
            }
            return ResponseEntity.ok(ApiResponse.<PatientCodeListDto>builder()
                    .success(true).message("Patient code list retrieved successfully").data(dto).build());
        } catch (Exception e) {
            log.error("Failed to retrieve patient code list {}", id, e);
            return ResponseEntity.ok(ApiResponse.<PatientCodeListDto>builder()
                    .success(false).message("Failed to retrieve patient code list: " + e.getMessage()).build());
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PatientCodeListDto>> create(
            @RequestBody PatientCodeListDto dto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(dto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<PatientCodeListDto>builder()
                    .success(false).message(validationError).build());
        }

        try {
            PatientCodeListDto created = service.create(dto);
            return ResponseEntity.created(URI.create("/api/patient-codes/" + created.id))
                    .body(ApiResponse.<PatientCodeListDto>builder()
                            .success(true).message("Patient code list created successfully").data(created).build());
        } catch (Exception e) {
            log.error("Failed to create patient code list", e);
            return ResponseEntity.ok(ApiResponse.<PatientCodeListDto>builder()
                    .success(false).message("Failed to create patient code list: " + e.getMessage()).build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientCodeListDto>> update(
            @PathVariable Long id,
            @RequestBody PatientCodeListDto dto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(dto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<PatientCodeListDto>builder()
                    .success(false).message(validationError).build());
        }

        try {
            PatientCodeListDto updated = service.update(id, dto);
            if (updated == null) {
                return ResponseEntity.ok(ApiResponse.<PatientCodeListDto>builder()
                        .success(false).message("Patient code list not found with id: " + id).build());
            }
            return ResponseEntity.ok(ApiResponse.<PatientCodeListDto>builder()
                    .success(true).message("Patient code list updated successfully").data(updated).build());
        } catch (Exception e) {
            log.error("Failed to update patient code list {}", id, e);
            return ResponseEntity.ok(ApiResponse.<PatientCodeListDto>builder()
                    .success(false).message("Failed to update patient code list: " + e.getMessage()).build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id) {
        try {
            boolean ok = service.delete(id);
            if (!ok) {
                return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .success(false).message("Patient code list not found with id: " + id).build());
            }
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("Patient code list deleted successfully").build());
        } catch (Exception e) {
            log.error("Failed to delete patient code list {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false).message("Failed to delete patient code list: " + e.getMessage()).build());
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<PatientCodeListDto>>> bulkUpsert(
            @RequestBody List<PatientCodeListDto> rows) {
        try {
            List<PatientCodeListDto> data = service.saveBulk(rows);
            return ResponseEntity.ok(ApiResponse.<List<PatientCodeListDto>>builder()
                    .success(true).message("Patient code lists saved successfully").data(data).build());
        } catch (Exception e) {
            log.error("Failed to bulk save patient code lists", e);
            return ResponseEntity.ok(ApiResponse.<List<PatientCodeListDto>>builder()
                    .success(false).message("Failed to bulk save patient code lists: " + e.getMessage()).build());
        }
    }

    @PostMapping("/{id}/set-default")
    public ResponseEntity<ApiResponse<PatientCodeListDto>> setDefault(
            @PathVariable Long id) {
        try {
            PatientCodeListDto data = service.setDefault(id);
            if (data == null) {
                return ResponseEntity.ok(ApiResponse.<PatientCodeListDto>builder()
                        .success(false).message("Patient code list not found with id: " + id).build());
            }
            return ResponseEntity.ok(ApiResponse.<PatientCodeListDto>builder()
                    .success(true).message("Default list set successfully").data(data).build());
        } catch (Exception e) {
            log.error("Failed to set default for patient code list {}", id, e);
            return ResponseEntity.ok(ApiResponse.<PatientCodeListDto>builder()
                    .success(false).message("Failed to set default: " + e.getMessage()).build());
        }
    }

    /**
     * Validates mandatory fields for PatientCodeList creation and update
     * @param dto PatientCodeListDto to validate
     * @return error message if validation fails, null if validation passes
     */
    private String validateMandatoryFields(PatientCodeListDto dto) {
        StringBuilder missingFields = new StringBuilder();

        if (dto.title == null || dto.title.trim().isEmpty()) {
            missingFields.append("title, ");
        }

        if (dto.order == null) {
            missingFields.append("order, ");
        }

        if (dto.codes == null || dto.codes.trim().isEmpty()) {
            missingFields.append("codes, ");
        }

        if (!missingFields.isEmpty()) {
            // Remove the trailing comma and space
            missingFields.setLength(missingFields.length() - 2);
            return "Missing mandatory fields: " + missingFields;
        }

        return null;
    }
}
