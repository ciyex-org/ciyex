package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PlanDto;
import com.qiaben.ciyex.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
@Slf4j
public class PlanController {

    private final PlanService service;

    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<PlanDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody PlanDto dto
    ) {
        PlanDto out = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<PlanDto>builder()
                .success(true)
                .message("Plan created")
                .data(out)
                .build());
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PlanDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody PlanDto dto
    ) {
        PlanDto out = service.update(orgId, patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<PlanDto>builder()
                .success(true)
                .message("Plan updated")
                .data(out).build());
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PlanDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId
    ) {
        PlanDto out = service.getOne(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<PlanDto>builder()
                .success(true).message("Plan fetched").data(out).build());
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<PlanDto>>> getAllByPatient(
            @PathVariable Long patientId,
            @RequestHeader("orgId") Long orgId
    ) {
        List<PlanDto> out = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<PlanDto>>builder()
                .success(true).message("Plans fetched").data(out).build());
    }

    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<PlanDto>>> getAllByEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId
    ) {
        List<PlanDto> out = service.getAllByEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<PlanDto>>builder()
                .success(true).message("Plans fetched").data(out).build());
    }

    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId
    ) {
        service.delete(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Plan deleted").build());
    }
}
