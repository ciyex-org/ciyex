package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.InvoiceBillDto;
import com.qiaben.ciyex.entity.InvoiceBill;
import com.qiaben.ciyex.entity.InvoiceStatus;
import com.qiaben.ciyex.repository.InvoiceBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceBillService {

    private final InvoiceBillRepository repository;

    /* ------------ Mapping Helpers ------------ */
    private InvoiceBillDto toDto(InvoiceBill e) {
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
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .dueDate(e.getDueDate())
                .paidAt(e.getPaidAt())
                .build();
    }

    private InvoiceBill toEntity(InvoiceBillDto dto) {
        return InvoiceBill.builder()
                .id(dto.getId())
                .orgId(dto.getOrgId())
                .userId(dto.getUserId()) // ⚡ ensure not null
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
        if (dto.getUserId() == null) {
            throw new RuntimeException("Invoice must have a userId (not null)");
        }

        InvoiceBill entity = toEntity(dto);
        entity.setStatus(InvoiceStatus.UNPAID);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        InvoiceBill saved = repository.save(entity);

        // generate identifiers after initial save
        saved.setExternalId("INV-" + saved.getId());
        saved.setInvoiceNumber("INV-" + saved.getId());

        return toDto(repository.save(saved));
    }

    public InvoiceBillDto updateInvoice(Long id, InvoiceBillDto dto) {
        InvoiceBill existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

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
        repository.deleteById(id);
    }

    public InvoiceBillDto getInvoiceById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
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
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setUpdatedAt(LocalDateTime.now());
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setReceiptUrl("receipt-" + invoice.getId() + ".pdf");

        return toDto(repository.save(invoice));
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
}
