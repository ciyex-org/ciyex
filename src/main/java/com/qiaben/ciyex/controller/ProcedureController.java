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

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<ProcedureDto>>> getAllByPatient(
            @PathVariable Long patientId, @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<ProcedureDto>>builder()
                .success(true).message("Procedures fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<ProcedureDto>>> getAllByEncounter(
            @PathVariable Long patientId, @PathVariable Long encounterId, @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<ProcedureDto>>builder()
                .success(true).message("Procedures fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ProcedureDto>> getOne(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        var dto = service.getOne(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<ProcedureDto>builder()
                .success(true).message("Procedure fetched").data(dto).build());
    }

    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<ProcedureDto>> create(
            @PathVariable Long patientId, @PathVariable Long encounterId, @RequestHeader("orgId") Long orgId,
            @RequestBody ProcedureDto dto) {
        var created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<ProcedureDto>builder()
                .success(true).message("Procedure created").data(created).build());
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ProcedureDto>> update(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId, @RequestBody ProcedureDto dto) {
        var updated = service.update(orgId, patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<ProcedureDto>builder()
                .success(true).message("Procedure updated").data(updated).build());
    }

    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Procedure deleted").build());
    }
}
