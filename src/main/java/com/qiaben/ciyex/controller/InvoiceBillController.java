package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.BillingHistoryDto;
import com.qiaben.ciyex.dto.InvoiceBillDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.BillingHistory.BillingStatus;
import com.qiaben.ciyex.entity.InvoiceStatus;
import com.qiaben.ciyex.service.BillingHistoryService;
import com.qiaben.ciyex.service.InvoiceBillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invoice-bills")
@RequiredArgsConstructor
@Slf4j
public class InvoiceBillController {

    private final InvoiceBillService service;
    private final BillingHistoryService billingHistoryService;

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
        return withContext(orgId, () -> {
            try {
                List<BillingHistoryDto> hist = billingHistoryService.getAll();
                if (hist != null && !hist.isEmpty()) {
                    // Map BillingHistoryDto -> InvoiceBillDto
                    List<InvoiceBillDto> mapped = hist.stream().map(h -> {
                        InvoiceBillDto dto = new InvoiceBillDto();
                        dto.setId(h.getId());
                        dto.setExternalId(h.getExternalId());
                        dto.setInvoiceUrl(h.getInvoiceUrl());
                        dto.setReceiptUrl(h.getReceiptUrl());
                        dto.setAmount(h.getAmount() != null ? h.getAmount() : BigDecimal.ZERO);

                        // Map BillingStatus → InvoiceStatus
                        if (h.getStatus() != null) {
                            switch (h.getStatus()) {
                                case SUCCEEDED:
                                    dto.setStatus(InvoiceStatus.PAID);
                                    break;
                                case ARCHIVED:
                                    dto.setStatus(InvoiceStatus.ARCHIVED);
                                    break;
                                default:
                                    dto.setStatus(InvoiceStatus.PENDING);
                                    break;
                            }
                        } else {
                            dto.setStatus(InvoiceStatus.PENDING);
                        }

                        dto.setCreatedAt(h.getCreatedAt());

                        // Build invoiceNumber with provider + payment method
                        String invoiceNumber = (h.getProvider() != null ? h.getProvider().name() : "UNKNOWN");
                        if (h.getStripePaymentMethodId() != null) {
                            invoiceNumber += "-" + h.getStripePaymentMethodId();
                        }
                        dto.setInvoiceNumber(invoiceNumber);
                        return dto;
                    }).collect(Collectors.toList());

                    return ApiResponse.<List<InvoiceBillDto>>builder()
                            .success(true)
                            .message("Billing history retrieved")
                            .data(mapped)
                            .build();
                }
            } catch (Exception ignored) {
                // fallback
            }

            // Fallback → just show paid invoices
            List<InvoiceBillDto> invoices = service.getByStatus(orgId, InvoiceStatus.PAID);
            return ApiResponse.<List<InvoiceBillDto>>builder()
                    .success(true)
                    .message("Billing history retrieved")
                    .data(invoices)
                    .build();
        });
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
            try {
                // Try to delete invoice record first
                service.deleteInvoice(id);
            } catch (RuntimeException e) {
                // If invoice not found, attempt to delete billing history row with same id
                // (UI sometimes calls delete on items coming from billing history view)
                try {
                    billingHistoryService.delete(id);
                } catch (RuntimeException ex) {
                    // rethrow original to indicate not found
                    throw e;
                }
            }
            return ApiResponse.<Void>builder()
                    .success(true)
                    .message("Invoice deleted")
                    .build();
        });
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveInvoice(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long orgId) {
        return withContext(orgId, () -> {
            try {
                // If an invoice exists with this id, mark as ARCHIVED
                service.getInvoiceById(id); // will throw if not found
                service.archiveInvoiceRecord(id);
            } catch (RuntimeException e) {
                // fallback: try archive on billing history directly
                billingHistoryService.archive(id);
            }
            return ApiResponse.<Void>builder().success(true).message("Archived").build();
        });
    }

    @PutMapping("/{id}/unarchive")
    public ResponseEntity<ApiResponse<Void>> unarchiveInvoice(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long orgId) {
        return withContext(orgId, () -> {
            try {
                log.info("Controller unarchive request for id={}, orgId={}", id, orgId);
                // Try invoice first
                service.getInvoiceById(id);
                service.unarchiveInvoiceRecord(id);
                // Also unarchive billing history if present
                billingHistoryService.unarchive(id);
            } catch (RuntimeException e) {
                try {
                    // fallback to billing history only
                    billingHistoryService.unarchive(id);
                } catch (RuntimeException ex) {
                    log.error("Failed to unarchive id={}: {}", id, ex.getMessage());
                }
            }
            return ApiResponse.<Void>builder().success(true).message("Unarchived").build();
        });
    }

    /**
     * Serve the receipt PDF for the given invoice id. If a stored receipt is
     * not available, dynamically render a simple receipt PDF.
     */
    @GetMapping("/{id}/receipt")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id, @RequestHeader("X-Org-Id") Long orgId) {
        // replicate withContext behavior so we can return a binary ResponseEntity
        com.qiaben.ciyex.dto.integration.RequestContext ctx = new com.qiaben.ciyex.dto.integration.RequestContext();
        ctx.setOrgId(orgId);
        com.qiaben.ciyex.dto.integration.RequestContext.set(ctx);
        try {
            byte[] pdf = service.renderReceiptPdf(id);
            String filename = "receipt-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } finally {
            com.qiaben.ciyex.dto.integration.RequestContext.clear();
        }
    }

    /* ---------- Utility wrapper ---------- */
    private <T> ResponseEntity<T> withContext(Long orgId, Callable<T> action) {
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
