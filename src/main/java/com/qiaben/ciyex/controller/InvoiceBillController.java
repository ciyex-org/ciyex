package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.InvoiceBillDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.InvoiceBillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoice-bills")
@RequiredArgsConstructor
public class InvoiceBillController {

    private final InvoiceBillService service;

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceBillDto>> create(
            @RequestBody InvoiceBillDto dto,
            @RequestHeader("x-org-id") Long orgId) {
        return withContext(orgId, () ->
                ApiResponse.<InvoiceBillDto>builder()
                        .success(true)
                        .message("Invoice created")
                        .data(service.createInvoice(dto))
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceBillDto>>> getAll(
            @RequestHeader("x-org-id") Long orgId) {
        return withContext(orgId, () ->
                ApiResponse.<List<InvoiceBillDto>>builder()
                        .success(true)
                        .message("Invoices retrieved")
                        .data(service.getAllInvoices())
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceBillDto>> getById(
            @PathVariable Long id,
            @RequestHeader("x-org-id") Long orgId) {
        return withContext(orgId, () ->
                ApiResponse.<InvoiceBillDto>builder()
                        .success(true)
                        .message("Invoice retrieved")
                        .data(service.getInvoiceById(id))
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceBillDto>> update(
            @PathVariable Long id,
            @RequestBody InvoiceBillDto dto,
            @RequestHeader("x-org-id") Long orgId) {
        return withContext(orgId, () ->
                ApiResponse.<InvoiceBillDto>builder()
                        .success(true)
                        .message("Invoice updated")
                        .data(service.updateInvoice(id, dto))
                        .build()
        );
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<InvoiceBillDto>> pay(
            @PathVariable Long id,
            @RequestHeader("x-org-id") Long orgId) {
        return withContext(orgId, () ->
                ApiResponse.<InvoiceBillDto>builder()
                        .success(true)
                        .message("Invoice paid")
                        .data(service.payInvoice(id))
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestHeader("x-org-id") Long orgId) {
        return withContext(orgId, () -> {
            service.deleteInvoice(id);
            return ApiResponse.<Void>builder()
                    .success(true)
                    .message("Invoice deleted")
                    .build();
        });
    }

    /* ---------- Utility wrapper to avoid repetition ---------- */
    private <T> ResponseEntity<T> withContext(Long orgId, java.util.concurrent.Callable<T> action) {
        RequestContext ctx = new RequestContext();
        ctx.setOrgId(orgId);
        RequestContext.set(ctx);
        try {
            return ResponseEntity.ok(action.call());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            RequestContext.clear();
        }
    }
}
