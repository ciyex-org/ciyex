



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
            @PathVariable Long patientId) {
        var list = service.getAllByPatient(patientId);
        return ResponseEntity.ok(ApiResponse.<List<ProcedureDto>>builder()
                .success(true).message("Procedures fetched").data(list).build());
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
        var created = service.create(patientId, encounterId, dto);


        return ResponseEntity.ok(ApiResponse.<ProcedureDto>builder()
                .success(true).message("Procedure created").data(created).build());
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ProcedureDto>> update(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestBody ProcedureDto dto) {
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
}
