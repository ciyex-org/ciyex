package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.InvoiceBillDto;
import com.qiaben.ciyex.entity.BillingHistory;
import com.qiaben.ciyex.entity.BillingHistory.BillingProvider;
import com.qiaben.ciyex.entity.BillingHistory.BillingStatus;
import com.qiaben.ciyex.entity.InvoiceBill;
import com.qiaben.ciyex.entity.InvoiceStatus;
import com.qiaben.ciyex.repository.BillingHistoryRepository;
import com.qiaben.ciyex.repository.InvoiceBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceBillService {

    private final InvoiceBillRepository repository;
    private final BillingHistoryRepository billingRepo;

    /* ------------ Mapping Helpers ------------ */
    private InvoiceBillDto toDto(InvoiceBill e) {
        // Provide sensible fallbacks for date fields if the DB row has nulls.
        java.time.LocalDateTime createdAt = e.getCreatedAt();
        java.time.LocalDateTime updatedAt = e.getUpdatedAt();

        // If createdAt is missing, fallback to updatedAt
        if (createdAt == null && updatedAt != null) createdAt = updatedAt;

        // dueDate fallback: if missing, use createdAt + 30 days when possible
        java.time.LocalDateTime dueDate = e.getDueDate();
        if (dueDate == null && createdAt != null) {
            dueDate = createdAt.plusDays(30);
        }

        // paidAt fallback: if status is PAID and paidAt missing, use updatedAt
        java.time.LocalDateTime paidAt = e.getPaidAt();
        if (paidAt == null && e.getStatus() == com.qiaben.ciyex.entity.InvoiceStatus.PAID && updatedAt != null) {
            paidAt = updatedAt;
        }

        return InvoiceBillDto.builder()
                .id(e.getId())
                .orgId(e.getOrgId())
                .userId(e.getUserId())
                .subscriptionId(e.getSubscriptionId())
                .amount(e.getAmount())
                .status(e.getStatus())
                .invoiceUrl(e.getInvoiceUrl())
                .receiptUrl(e.getReceiptUrl())
                .externalId(e.getExternalId())
                .invoiceNumber(e.getInvoiceNumber())
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .dueDate(dueDate)
                .paidAt(paidAt)
                .build();
    }

    private InvoiceBill toEntity(InvoiceBillDto dto) {
        return InvoiceBill.builder()
                .id(dto.getId())
                .orgId(dto.getOrgId())
                .userId(dto.getUserId())
                .subscriptionId(dto.getSubscriptionId())
                .amount(dto.getAmount())
                .status(dto.getStatus())
                .invoiceUrl(dto.getInvoiceUrl())
                .receiptUrl(dto.getReceiptUrl())
                .externalId(dto.getExternalId())
                .invoiceNumber(dto.getInvoiceNumber())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .dueDate(dto.getDueDate())
                .paidAt(dto.getPaidAt())
                .build();
    }

    /* ------------ CRUD ------------ */
    public InvoiceBillDto createInvoice(InvoiceBillDto dto) {
        if (dto.getUserId() == null && dto.getOrgId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invoice must have a userId or orgId (not both null)");
        }

        InvoiceBill entity = toEntity(dto);
        entity.setStatus(InvoiceStatus.UNPAID);

        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        if (entity.getDueDate() == null) {
            entity.setDueDate(now.plusDays(30));
        }

        InvoiceBill saved = repository.save(entity);

        // Generate identifiers
        String paddedId = String.format("%05d", saved.getId());
        saved.setExternalId("INV-" + paddedId);
        saved.setInvoiceNumber("INV-" + paddedId);
        saved.setUpdatedAt(LocalDateTime.now());
        saved = repository.save(saved);

        // Also insert into billing history for regular invoices.
        // If this invoice was generated from a subscription, don't create a PENDING billing history
        // because subscription creation should only create the invoice record. Actual payment
        // will create the appropriate billing history entry (PAID) when it occurs.
        if (saved.getSubscriptionId() == null) {
            BillingHistory history = BillingHistory.builder()
                    .orgId(saved.getOrgId())
                    .userId(saved.getUserId())
                    .provider(BillingProvider.INVOICE)
                    .amount(saved.getAmount())
                    .status(BillingStatus.PENDING)
                    .invoiceBill(saved)
                    .createdAt(saved.getCreatedAt())
                    .updatedAt(saved.getUpdatedAt())
                    .build();

            billingRepo.save(history);
        }

        return toDto(saved);
    }

    public InvoiceBillDto updateInvoice(Long id, InvoiceBillDto dto) {
        InvoiceBill existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (dto.getAmount() != null) {
            existing.setAmount(dto.getAmount());
        }
        if (dto.getDueDate() != null) {
            existing.setDueDate(dto.getDueDate());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return toDto(repository.save(existing));
    }

    public void deleteInvoice(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found");
        }
        repository.deleteById(id);

        // Also delete from billing history
        billingRepo.findByInvoiceBill_Id(id).ifPresent(billingRepo::delete); // ✅ fixed method name
    }

    public InvoiceBillDto getInvoiceById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    }

    public List<InvoiceBillDto> getAllInvoices() {
        return repository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /* ------------ Payment ------------ */
    public InvoiceBillDto payInvoice(Long id) {
        InvoiceBill invoice = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice is already paid");
        }

        LocalDateTime now = LocalDateTime.now();
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setUpdatedAt(now);
        invoice.setPaidAt(now);
        invoice.setReceiptUrl("receipt-" + invoice.getInvoiceNumber() + ".pdf");

        InvoiceBill saved = repository.save(invoice);

        // Update billing history
        billingRepo.findByInvoiceBill_Id(saved.getId()).ifPresent(history -> {
            history.setStatus(BillingStatus.SUCCEEDED);
            history.setUpdatedAt(now);
            history.setReceiptUrl(saved.getReceiptUrl());
            billingRepo.save(history);
        });

        return toDto(saved);
    }

    /**
     * Archive an invoice record (set InvoiceStatus.ARCHIVED) without changing other rows.
     */
    public void archiveInvoiceRecord(Long id) {
        InvoiceBill invoice = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        invoice.setStatus(InvoiceStatus.ARCHIVED);
        invoice.setUpdatedAt(LocalDateTime.now());
        repository.save(invoice);
    }

    /**
     * Unarchive an invoice record. If it was paid (paidAt present or status PAID) restore PAID,
     * otherwise set to UNPAID.
     */
    public void unarchiveInvoiceRecord(Long id) {
        InvoiceBill invoice = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        if (invoice.getStatus() == InvoiceStatus.ARCHIVED) {
            if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getPaidAt() != null || (invoice.getReceiptUrl() != null && !invoice.getReceiptUrl().isBlank())) {
                invoice.setStatus(InvoiceStatus.PAID);
            } else {
                invoice.setStatus(InvoiceStatus.UNPAID);
            }
            invoice.setUpdatedAt(LocalDateTime.now());
            repository.save(invoice);
        }
    }

    /* ------------ Org-based Queries ------------ */
    public List<InvoiceBillDto> getAllByOrg(Long orgId) {
        return repository.findByOrgId(orgId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<InvoiceBillDto> getByStatus(Long orgId, InvoiceStatus status) {
        return repository.findByOrgIdAndStatus(orgId, status)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Render a simple receipt PDF for the given invoice id. This will produce
     * a minimal one-page PDF containing invoice number, amount and date.
     */
    public byte[] renderReceiptPdf(Long id) {
        InvoiceBill invoice = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(50, 700);
                String title = "Receipt" + (invoice.getInvoiceNumber() != null ? (": " + invoice.getInvoiceNumber()) : "");
                cs.showText(title);
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, 660);
                cs.showText("Amount: $" + (invoice.getAmount() != null ? invoice.getAmount().toString() : "0.00"));
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, 640);
                String dateOnly = "";
                if (invoice.getCreatedAt() != null) {
                    try {
                        dateOnly = invoice.getCreatedAt().toLocalDate().toString();
                    } catch (Exception ex) {
                        dateOnly = invoice.getCreatedAt().toString();
                    }
                }
                cs.showText("Date: " + dateOnly);
                cs.endText();

                // small footer
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
                cs.newLineAtOffset(50, 80);
                cs.showText("Generated by CIYEX");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to render PDF: " + e.getMessage(), e);
        }
    }
}
