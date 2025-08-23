package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.InvoiceDto;
import com.qiaben.ciyex.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/billing/invoices")
@RequiredArgsConstructor @Slf4j
public class InvoiceController {

    private final InvoiceService service;

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getAllByPatient(
            @PathVariable Long patientId, @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<InvoiceDto>>builder().success(true).message("Invoices fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getAllByEncounter(
            @PathVariable Long patientId, @PathVariable Long encounterId, @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<InvoiceDto>>builder().success(true).message("Invoices fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getOne(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        var dto = service.getOne(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<InvoiceDto>builder().success(true).message("Invoice fetched").data(dto).build());
    }

    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<InvoiceDto>> create(
            @PathVariable Long patientId, @PathVariable Long encounterId, @RequestHeader("orgId") Long orgId,
            @RequestBody InvoiceDto dto) {
        var created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<InvoiceDto>builder().success(true).message("Invoice created").data(created).build());
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> update(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId, @RequestBody InvoiceDto dto) {
        var updated = service.update(orgId, patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<InvoiceDto>builder().success(true).message("Invoice updated").data(updated).build());
    }

    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("Invoice deleted").build());
    }
}
