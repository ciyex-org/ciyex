package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.InvoiceBillDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.InvoiceStatus;
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
            @RequestHeader("X-Org-Id") Long orgId) {
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
            @RequestHeader("X-Org-Id") Long orgId,
            @RequestParam(value = "status", required = false) String status) {
        return withContext(orgId, () -> {
            List<InvoiceBillDto> invoices;
            if (status != null) {
                invoices = service.getByStatus(orgId, InvoiceStatus.valueOf(status.toUpperCase()));
            } else {
                invoices = service.getAllByOrg(orgId);
            }
            return ApiResponse.<List<InvoiceBillDto>>builder()
                    .success(true)
                    .message("Invoices retrieved")
                    .data(invoices)
                    .build();
        });
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<InvoiceBillDto>>> getBillingHistory(
            @RequestHeader("X-Org-Id") Long orgId) {
        return withContext(orgId, () ->
                ApiResponse.<List<InvoiceBillDto>>builder()
                        .success(true)
                        .message("Billing history retrieved")
                        .data(service.getByStatus(orgId, InvoiceStatus.PAID))
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceBillDto>> getById(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long orgId) {
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
            @RequestHeader("X-Org-Id") Long orgId) {
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
            @RequestHeader("X-Org-Id") Long orgId) {
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
            @RequestHeader("X-Org-Id") Long orgId) {
        return withContext(orgId, () -> {
            service.deleteInvoice(id);
            return ApiResponse.<Void>builder()
                    .success(true)
                    .message("Invoice deleted")
                    .build();
        });
    }

    /* ---------- Utility wrapper ---------- */
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
