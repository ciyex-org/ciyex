package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ReviewOfSystemDto;
import com.qiaben.ciyex.service.ReviewOfSystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviewofsystems") // short path to match your Bruno screenshot
@RequiredArgsConstructor
@Slf4j
public class ReviewOfSystemController {

    private final ReviewOfSystemService service;

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<ReviewOfSystemDto>>> getAllByPatient(
            @PathVariable Long patientId, @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<ReviewOfSystemDto>>builder()
                .success(true).message("ROS fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<ReviewOfSystemDto>>> getAllByEncounter(
            @PathVariable Long patientId, @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<ReviewOfSystemDto>>builder()
                .success(true).message("ROS fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> getOne(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        var dto = service.getOne(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
                .success(true).message("ROS fetched").data(dto).build());
    }

    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> create(
            @PathVariable Long patientId, @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId, @RequestBody ReviewOfSystemDto dto) {
        var created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
                .success(true).message("ROS created").data(created).build());
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ReviewOfSystemDto>> update(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId, @RequestBody ReviewOfSystemDto dto) {
        var updated = service.update(orgId, patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<ReviewOfSystemDto>builder()
                .success(true).message("ROS updated").data(updated).build());
    }

    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("ROS deleted").build());
    }
}
