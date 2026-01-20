package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.MedicationRequestDto;
import com.qiaben.ciyex.service.MedicationRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medication-requests")
@RequiredArgsConstructor
@Slf4j
public class MedicationRequestController {


    private final MedicationRequestService service;

    // ✅ Create Medication Request
    @PostMapping
    public ResponseEntity<ApiResponse<MedicationRequestDto>> create(@RequestBody MedicationRequestDto dto) {
        try {
            MedicationRequestDto created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<MedicationRequestDto>builder()
                    .success(true)
                    .message("Medication request created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create medication request", e);
            return ResponseEntity.ok(ApiResponse.<MedicationRequestDto>builder()
                    .success(false)
                    .message("Failed to create medication request: " + e.getMessage())
                    .build());
        }
    }

    // ✅ Get Medication Request by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicationRequestDto>> get(@PathVariable("id") Long id) {
        try {
            MedicationRequestDto dto = service.getById(id);
            if (dto == null) {
                return ResponseEntity.ok(ApiResponse.<MedicationRequestDto>builder()
                        .success(false)
                        .message("Medication request not found with id: " + id)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<MedicationRequestDto>builder()
                    .success(true)
                    .message("Medication request retrieved successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve medication request with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<MedicationRequestDto>builder()
                    .success(false)
                    .message("Failed to retrieve medication request: " + e.getMessage())
                    .build());
        }
    }

    // ✅ Update Medication Request
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicationRequestDto>> update(@PathVariable("id") Long id, @RequestBody MedicationRequestDto dto) {
        try {
            MedicationRequestDto updated = service.update(id, dto);
            if (updated == null) {
                return ResponseEntity.ok(ApiResponse.<MedicationRequestDto>builder()
                        .success(false)
                        .message("Medication request not found with id: " + id)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<MedicationRequestDto>builder()
                    .success(true)
                    .message("Medication request updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update medication request with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<MedicationRequestDto>builder()
                    .success(false)
                    .message("Failed to update medication request: " + e.getMessage())
                    .build());
        }
    }

    // ✅ Delete Medication Request
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Medication request deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete medication request with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete medication request: " + e.getMessage())
                    .build());
        }
    }

    // ✅ Get All Medication Requests (filter by patientId or encounterId)
    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicationRequestDto>>> getAll(
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "encounterId", required = false) Long encounterId) {
        try {
            List<MedicationRequestDto> list = service.getAllByPatientIdOrEncounterId(patientId, encounterId);
            return ResponseEntity.ok(ApiResponse.<List<MedicationRequestDto>>builder()
                    .success(true)
                    .message("Medication requests retrieved successfully")
                    .data(list)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve medication requests", e);
            return ResponseEntity.ok(ApiResponse.<List<MedicationRequestDto>>builder()
                    .success(false)
                    .message("Failed to retrieve medication requests: " + e.getMessage())
                    .build());
        }
    }


}
