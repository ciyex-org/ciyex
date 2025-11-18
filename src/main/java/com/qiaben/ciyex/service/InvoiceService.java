package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.InvoiceDto;
import com.qiaben.ciyex.dto.InvoiceDto.LineDto;
import com.qiaben.ciyex.dto.InvoiceDto.PaymentDto;

import com.qiaben.ciyex.entity.Invoice;
import com.qiaben.ciyex.entity.InvoiceLine;
import com.qiaben.ciyex.entity.InvoicePayment;
import com.qiaben.ciyex.exception.ResourceNotFoundException;
import com.qiaben.ciyex.repository.ImmunizationRepository;
import com.qiaben.ciyex.repository.InvoiceRepository;
import com.qiaben.ciyex.storage.ExternalInvoiceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j

public class InvoiceService {

    private final InvoiceRepository repo;
    private final Optional<ExternalInvoiceStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public InvoiceDto create(Long patientId, Long encounterId, InvoiceDto in) {
        // Validate mandatory fields
        validateInvoiceDto(in);

        Invoice inv = new Invoice(); inv.setPatientId(patientId); inv.setEncounterId(encounterId);
        inv.setInvoiceNumber(in.getInvoiceNumber());
        inv.setStatus(in.getStatus());
        inv.setCurrency(in.getCurrency());
        inv.setIssueDate(in.getIssueDate());
        inv.setDueDate(in.getDueDate());
        inv.setPayer(in.getPayer());
        inv.setNotes(in.getNotes());

        if (in.getLines()!=null) for (LineDto l: in.getLines()) {
            InvoiceLine il = InvoiceLine.builder()
                    .invoice(inv)
                    .description(l.getDescription())
                    .code(l.getCode())
                    .quantity(l.getQuantity())
                    .unitPrice(dec(l.getUnitPrice()))
                    .amount(dec(l.getAmount()))
                    .build();
            inv.getLines().add(il);
        }
        if (in.getPayments()!=null) for (PaymentDto p: in.getPayments()) {
            InvoicePayment ip = InvoicePayment.builder()
                    .invoice(inv)
                    .date(p.getDate())
                    .amount(dec(p.getAmount()))
                    .method(p.getMethod())
                    .reference(p.getReference())
                    .note(p.getNote())
                    .build();
            inv.getPayments().add(ip);
        }

        // totals
        BigDecimal gross = inv.getLines().stream().map(InvoiceLine::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid  = inv.getPayments().stream().map(InvoicePayment::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        inv.setTotalGross(gross);
        inv.setTotalNet(gross.subtract(paid));

        Invoice saved = repo.save(inv);

        external.ifPresent(ext -> {
            final Invoice ref = saved;
            String extId = ext.create(mapToDto(ref));
            ref.setExternalId(extId);
            repo.save(ref);
        });
        return mapToDto(saved);
    }

    public InvoiceDto update(Long patientId, Long encounterId, Long id, InvoiceDto in) {
        // Validate mandatory fields
        validateInvoiceDto(in);

        Invoice inv = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with patientId: " + patientId + ", encounterId: " + encounterId + ", id: " + id));

        inv.setInvoiceNumber(in.getInvoiceNumber());
        inv.setStatus(in.getStatus());
        inv.setCurrency(in.getCurrency());
        inv.setIssueDate(in.getIssueDate());
        inv.setDueDate(in.getDueDate());
        inv.setPayer(in.getPayer());
        inv.setNotes(in.getNotes());

        inv.getLines().clear();
        if (in.getLines()!=null) for (LineDto l: in.getLines()) {
            inv.getLines().add(InvoiceLine.builder()
                    .invoice(inv)
                    .description(l.getDescription())
                    .code(l.getCode())
                    .quantity(l.getQuantity())
                    .unitPrice(dec(l.getUnitPrice()))
                    .amount(dec(l.getAmount()))
                    .build());
        }

        inv.getPayments().clear();
        if (in.getPayments()!=null) for (PaymentDto p: in.getPayments()) {
            inv.getPayments().add(InvoicePayment.builder()
                    .invoice(inv)
                    .date(p.getDate())
                    .amount(dec(p.getAmount()))
                    .method(p.getMethod())
                    .reference(p.getReference())
                    .note(p.getNote())
                    .build());
        }

        BigDecimal gross = inv.getLines().stream().map(InvoiceLine::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid  = inv.getPayments().stream().map(InvoicePayment::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        inv.setTotalGross(gross);
        inv.setTotalNet(gross.subtract(paid));

        Invoice updated = repo.save(inv);
        external.ifPresent(ext -> { final Invoice ref = updated;
            if (ref.getExternalId()!=null) external.get().update(ref.getExternalId(), mapToDto(ref));
        });
        return mapToDto(updated);
    }

    public void delete(Long patientId, Long encounterId, Long id) {
        Invoice inv = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with patientId: " + patientId + ", encounterId: " + encounterId + ", id: " + id));
        external.ifPresent(ext -> {/* Line 137 omitted */});
        repo.delete(inv);
    }

    public InvoiceDto getOne(Long patientId, Long encounterId, Long id) {
        Invoice inv = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with patientId: " + patientId + ", encounterId: " + encounterId + ", id: " + id));
        return mapToDto(inv);
    }

    public List<InvoiceDto> getAllByPatient(Long patientId) {
        return repo.findByPatientId(patientId).stream().map(this::mapToDto).toList();
    }

    public List<InvoiceDto> getAllByEncounter(Long patientId, Long encounterId) {
        return repo.findByPatientIdAndEncounterId(patientId, encounterId)
                .stream().map(this::mapToDto).toList();
    }

    private InvoiceDto mapToDto(Invoice e) {
        InvoiceDto dto = new InvoiceDto();
        dto.setId(e.getId()); dto.setExternalId(e.getExternalId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());
        dto.setInvoiceNumber(e.getInvoiceNumber());
        dto.setStatus(e.getStatus()); dto.setCurrency(e.getCurrency());
        dto.setIssueDate(e.getIssueDate()); dto.setDueDate(e.getDueDate());
        dto.setPayer(e.getPayer()); dto.setNotes(e.getNotes());
        dto.setTotalGross(val(e.getTotalGross())); dto.setTotalNet(val(e.getTotalNet()));

        List<InvoiceDto.LineDto> lines = new ArrayList<>();
        for (InvoiceLine l : e.getLines()) {
            InvoiceDto.LineDto ld = new InvoiceDto.LineDto();
            ld.setId(l.getId()); ld.setDescription(l.getDescription()); ld.setCode(l.getCode());
            ld.setQuantity(l.getQuantity()); ld.setUnitPrice(val(l.getUnitPrice())); ld.setAmount(val(l.getAmount()));
            lines.add(ld);
        }
        dto.setLines(lines);

        List<InvoiceDto.PaymentDto> pays = new ArrayList<>();
        for (InvoicePayment p : e.getPayments()) {
            InvoiceDto.PaymentDto pd = new InvoiceDto.PaymentDto();
            pd.setId(p.getId()); pd.setDate(p.getDate()); pd.setAmount(val(p.getAmount()));
            pd.setMethod(p.getMethod()); pd.setReference(p.getReference()); pd.setNote(p.getNote());
            pays.add(pd);
        }
        dto.setPayments(pays);

        InvoiceDto.Audit a = new InvoiceDto.Audit();
        if (e.getCreatedAt()!=null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt()!=null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        dto.setAudit(a);
        return dto;
    }

    private BigDecimal dec(String s){ return (s==null||s.isBlank())? null : new BigDecimal(s); }
    private String val(BigDecimal d){ return d==null? null : d.toPlainString(); }

    /**
     * Validates mandatory fields in InvoiceDto.
     * Throws IllegalArgumentException if any mandatory field is missing or empty.
     */
    private void validateInvoiceDto(InvoiceDto dto) {
        List<String> errors = new ArrayList<>();

        // Validate main invoice fields
        if (!StringUtils.hasText(dto.getInvoiceNumber())) {
            errors.add("Invoice number is mandatory");
        }
        if (!StringUtils.hasText(dto.getStatus())) {
            errors.add("Status is mandatory");
        }
        if (!StringUtils.hasText(dto.getCurrency())) {
            errors.add("Currency is mandatory");
        }
        if (!StringUtils.hasText(dto.getIssueDate())) {
            errors.add("Issue date is mandatory");
        }
        if (!StringUtils.hasText(dto.getDueDate())) {
            errors.add("Due date is mandatory");
        }
        if (!StringUtils.hasText(dto.getPayer())) {
            errors.add("Payer is mandatory");
        }

        // Validate lines (optional, but if provided must be valid)
        if (dto.getLines() != null && !dto.getLines().isEmpty()) {
            for (int i = 0; i < dto.getLines().size(); i++) {
                LineDto line = dto.getLines().get(i);
                String prefix = "Line " + (i + 1) + ": ";

                if (!StringUtils.hasText(line.getDescription())) {
                    errors.add(prefix + "Description is mandatory");
                }
                if (!StringUtils.hasText(line.getCode())) {
                    errors.add(prefix + "Code is mandatory");
                }
                if (line.getQuantity() == null || line.getQuantity() <= 0) {
                    errors.add(prefix + "Quantity must be greater than 0");
                }
                if (!StringUtils.hasText(line.getUnitPrice())) {
                    errors.add(prefix + "Unit price is mandatory");
                } else {
                    try {
                        BigDecimal price = new BigDecimal(line.getUnitPrice());
                        if (price.compareTo(BigDecimal.ZERO) <= 0) {
                            errors.add(prefix + "Unit price must be greater than 0");
                        }
                    } catch (NumberFormatException e) {
                        errors.add(prefix + "Unit price must be a valid number");
                    }
                }
                if (!StringUtils.hasText(line.getAmount())) {
                    errors.add(prefix + "Amount is mandatory");
                } else {
                    try {
                        BigDecimal amount = new BigDecimal(line.getAmount());
                        if (amount.compareTo(BigDecimal.ZERO) < 0) {
                            errors.add(prefix + "Amount cannot be negative");
                        }
                    } catch (NumberFormatException e) {
                        errors.add(prefix + "Amount must be a valid number");
                    }
                }
            }
        }

        // If there are any validation errors, throw exception
        if (!errors.isEmpty()) {
            String errorMessage = "Validation failed: " + String.join("; ", errors);
            log.error("Invoice validation failed: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
