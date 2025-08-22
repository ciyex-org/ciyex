package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.CodeDto;
import com.qiaben.ciyex.service.CodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codes")
@RequiredArgsConstructor
@Slf4j
public class CodeController {

    private final CodeService service;

    // READ ALL by patient
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<CodeDto>>> getAllByPatient(
            @PathVariable Long patientId, @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<CodeDto>>builder()
                .success(true).message("Codes fetched").data(list).build());
    }

    // READ ALL by encounter
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<CodeDto>>> getAllByEncounter(
            @PathVariable Long patientId, @PathVariable Long encounterId, @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<CodeDto>>builder()
                .success(true).message("Codes fetched").data(list).build());
    }

    // READ ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<CodeDto>> getOne(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        var dto = service.getOne(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<CodeDto>builder()
                .success(true).message("Code fetched").data(dto).build());
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<CodeDto>> create(
            @PathVariable Long patientId, @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody CodeDto dto) {
        var created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<CodeDto>builder()
                .success(true).message("Code created").data(created).build());
    }

    // UPDATE
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<CodeDto>> update(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody CodeDto dto) {
        var updated = service.update(orgId, patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<CodeDto>builder()
                .success(true).message("Code updated").data(updated).build());
    }

    // DELETE
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Code deleted").build());
    }

    // FILTER by type (in encounter)
    @GetMapping("/{patientId}/{encounterId}/type/{codeType}")
    public ResponseEntity<ApiResponse<List<CodeDto>>> listByType(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable String codeType,
            @RequestHeader("orgId") Long orgId,
            @RequestParam(value = "active", required = false) Boolean active) {
        var list = service.searchInEncounter(orgId, patientId, encounterId, codeType, active, "");
        return ResponseEntity.ok(ApiResponse.<List<CodeDto>>builder()
                .success(true).message("Codes filtered").data(list).build());
    }

    // SEARCH (in encounter)
    @GetMapping("/{patientId}/{encounterId}/search")
    public ResponseEntity<ApiResponse<List<CodeDto>>> search(
            @PathVariable Long patientId, @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestParam(value = "q", required = false, defaultValue = "") String q,
            @RequestParam(value = "codeType", required = false) String codeType,
            @RequestParam(value = "active", required = false) Boolean active) {
        var list = service.searchInEncounter(orgId, patientId, encounterId, codeType, active, q);
        return ResponseEntity.ok(ApiResponse.<List<CodeDto>>builder()
                .success(true).message("Codes search results").data(list).build());
    }
}
