package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.InvoiceDto;
import org.ciyex.ehr.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('SCOPE_user/Claim.read')")
@RestController
@RequestMapping("/api/billing/invoices")
@RequiredArgsConstructor @Slf4j
public class InvoiceController {

    private final InvoiceService service;

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getAllByPatient(
            @PathVariable Long patientId) {
        var list = service.getAllByPatient(patientId);
        return ResponseEntity.ok(ApiResponse.<List<InvoiceDto>>builder().success(true).message("Invoices fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getAllByEncounter(
            @PathVariable Long patientId, @PathVariable Long encounterId) {
        var list = service.getAllByEncounter(patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<InvoiceDto>>builder().success(true).message("Invoices fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getOne(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable String id) {
        var dto = service.getOne(patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<InvoiceDto>builder().success(true).message("Invoice fetched").data(dto).build());
    }

    @PostMapping("/{patientId}/{encounterId}")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<InvoiceDto>> create(
            @PathVariable Long patientId, @PathVariable Long encounterId, @Valid @RequestBody InvoiceDto dto) {
        var created = service.create(patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<InvoiceDto>builder().success(true).message("Invoice created").data(created).build());
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<InvoiceDto>> update(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable String id,
            @Valid @RequestBody InvoiceDto dto) {
        var updated = service.update(patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<InvoiceDto>builder().success(true).message("Invoice updated").data(updated).build());
    }

    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable String id) {
        service.delete(patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("Invoice deleted").build());
    }
}
