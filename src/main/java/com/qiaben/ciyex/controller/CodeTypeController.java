package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.CodeTypeDto;
import com.qiaben.ciyex.service.CodeTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codetypes")
@RequiredArgsConstructor
@Slf4j
public class CodeTypeController {

    private final CodeTypeService service;

    // READ ALL by encounter
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<CodeTypeDto>>> getAllByEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        try {
            var list = service.getAllByEncounter(patientId, encounterId);
            return ResponseEntity.ok(ApiResponse.<List<CodeTypeDto>>builder()
                    .success(true).message("CodeTypes fetched successfully").data(list).build());
        } catch (IllegalArgumentException e) {
            log.warn("No CodeTypes found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<List<CodeTypeDto>>builder()
                            .success(false).message(e.getMessage()).data(null).build());
        } catch (Exception e) {
            log.error("Error fetching CodeTypes for patientId={}, encounterId={}: {}", patientId, encounterId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<CodeTypeDto>>builder()
                            .success(false).message("Failed to fetch CodeTypes: " + e.getMessage()).data(null).build());
        }
    }

    // READ ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<CodeTypeDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id) {
        try {
            var dto = service.getOne(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<CodeTypeDto>builder()
                    .success(true).message("CodeType fetched successfully").data(dto).build());
        } catch (IllegalArgumentException e) {
            log.warn("CodeType not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<CodeTypeDto>builder()
                            .success(false).message(e.getMessage()).data(null).build());
        } catch (Exception e) {
            log.error("Error fetching CodeType for id={}, patientId={}, encounterId={}: {}", id, patientId, encounterId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<CodeTypeDto>builder()
                            .success(false).message("Failed to fetch CodeType: " + e.getMessage()).data(null).build());
        }
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<CodeTypeDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody CodeTypeDto dto) {
        try {
            var created = service.create(patientId, encounterId, dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<CodeTypeDto>builder()
                            .success(true).message("CodeType created successfully").data(created).build());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for CodeType creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<CodeTypeDto>builder()
                            .success(false).message(e.getMessage()).data(null).build());
        } catch (Exception e) {
            log.error("Error creating CodeType for patientId={}, encounterId={}: {}", patientId, encounterId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<CodeTypeDto>builder()
                            .success(false).message("Failed to create CodeType: " + e.getMessage()).data(null).build());
        }
    }

    // UPDATE
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<CodeTypeDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id,
            @RequestBody CodeTypeDto dto) {
        try {
            var updated = service.update(patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<CodeTypeDto>builder()
                    .success(true).message("CodeType updated successfully").data(updated).build());
        } catch (IllegalArgumentException e) {
            log.warn("CodeType not found or invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<CodeTypeDto>builder()
                            .success(false).message(e.getMessage()).data(null).build());
        } catch (Exception e) {
            log.error("Error updating CodeType for id={}, patientId={}, encounterId={}: {}", id, patientId, encounterId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<CodeTypeDto>builder()
                            .success(false).message("Failed to update CodeType: " + e.getMessage()).data(null).build());
        }
    }

    // DELETE
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable String id) {
        try {
            service.delete(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("CodeType deleted successfully").build());
        } catch (IllegalArgumentException e) {
            log.warn("CodeType not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder()
                            .success(false).message(e.getMessage()).build());
        } catch (Exception e) {
            log.error("Error deleting CodeType for id={}, patientId={}, encounterId={}: {}", id, patientId, encounterId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .success(false).message("Failed to delete CodeType: " + e.getMessage()).build());
        }
    }

    // SEARCH (in encounter)
    @GetMapping("/{patientId}/{encounterId}/search")
    public ResponseEntity<ApiResponse<List<CodeTypeDto>>> search(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestParam(value = "q", required = false, defaultValue = "") String q,
            @RequestParam(value = "codeTypeKey", required = false) String codeTypeKey,
            @RequestParam(value = "active", required = false) Boolean active) {
        try {
            var list = service.searchInEncounter(patientId, encounterId, codeTypeKey, active, q);
            return ResponseEntity.ok(ApiResponse.<List<CodeTypeDto>>builder()
                    .success(true).message("CodeTypes search results retrieved successfully").data(list).build());
        } catch (Exception e) {
            log.error("Error searching CodeTypes for patientId={}, encounterId={}: {}", patientId, encounterId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<CodeTypeDto>>builder()
                            .success(false).message("Failed to search CodeTypes: " + e.getMessage()).data(null).build());
        }
    }
}
