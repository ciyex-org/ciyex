package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.qiaben.ciyex.dto.InvoiceDto;
import com.qiaben.ciyex.dto.InvoiceDto.LineDto;
import com.qiaben.ciyex.dto.InvoiceDto.PaymentDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FHIR-only Invoice Service.
 * Uses FHIR Invoice resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // Extension URLs
    private static final String EXT_ENCOUNTER = "http://ciyex.com/fhir/StructureDefinition/encounter-reference";
    private static final String EXT_INVOICE_NUMBER = "http://ciyex.com/fhir/StructureDefinition/invoice-number";
    private static final String EXT_DUE_DATE = "http://ciyex.com/fhir/StructureDefinition/due-date";
    private static final String EXT_PAYER = "http://ciyex.com/fhir/StructureDefinition/payer";
    private static final String EXT_CURRENCY = "http://ciyex.com/fhir/StructureDefinition/currency";
    private static final String EXT_LINE_CODE = "http://ciyex.com/fhir/StructureDefinition/line-code";
    private static final String EXT_LINE_QUANTITY = "http://ciyex.com/fhir/StructureDefinition/line-quantity";
    private static final String EXT_LINE_UNIT_PRICE = "http://ciyex.com/fhir/StructureDefinition/line-unit-price";
    private static final String EXT_PAYMENTS = "http://ciyex.com/fhir/StructureDefinition/payments";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public InvoiceDto create(Long patientId, Long encounterId, InvoiceDto in) {
        validateInvoiceDto(in);

        in.setPatientId(patientId);
        in.setEncounterId(encounterId);

        // Calculate totals
        BigDecimal gross = calculateGross(in.getLines());
        BigDecimal paid = calculatePaid(in.getPayments());
        in.setTotalGross(gross.toPlainString());
        in.setTotalNet(gross.subtract(paid).toPlainString());

        log.debug("Creating FHIR Invoice for patient: {} encounter: {}", patientId, encounterId);

        Invoice invoice = toFhirInvoice(in);
        var outcome = fhirClientService.create(invoice, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        in.setExternalId(fhirId);
        log.info("Created FHIR Invoice with id: {}", fhirId);

        return in;
    }

    // GET ONE
    public InvoiceDto getOne(Long patientId, Long encounterId, String fhirId) {
        log.debug("Getting FHIR Invoice: {}", fhirId);
        Invoice invoice = fhirClientService.read(Invoice.class, fhirId, getPracticeId());
        return fromFhirInvoice(invoice);
    }

    // GET ALL BY PATIENT
    public List<InvoiceDto> getAllByPatient(Long patientId) {
        log.debug("Getting FHIR Invoices for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Invoice.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<Invoice> invoices = fhirClientService.extractResources(bundle, Invoice.class);
        return invoices.stream().map(this::fromFhirInvoice).collect(Collectors.toList());
    }

    // GET ALL BY ENCOUNTER
    public List<InvoiceDto> getAllByEncounter(Long patientId, Long encounterId) {
        log.debug("Getting FHIR Invoices for patient: {} encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Invoice.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<Invoice> invoices = fhirClientService.extractResources(bundle, Invoice.class);
        
        // Filter by encounter
        return invoices.stream()
                .filter(inv -> hasEncounter(inv, encounterId))
                .map(this::fromFhirInvoice)
                .collect(Collectors.toList());
    }

    // UPDATE
    public InvoiceDto update(Long patientId, Long encounterId, String fhirId, InvoiceDto in) {
        validateInvoiceDto(in);

        in.setPatientId(patientId);
        in.setEncounterId(encounterId);
        in.setExternalId(fhirId);

        // Calculate totals
        BigDecimal gross = calculateGross(in.getLines());
        BigDecimal paid = calculatePaid(in.getPayments());
        in.setTotalGross(gross.toPlainString());
        in.setTotalNet(gross.subtract(paid).toPlainString());

        log.debug("Updating FHIR Invoice: {}", fhirId);

        Invoice invoice = toFhirInvoice(in);
        invoice.setId(fhirId);
        fhirClientService.update(invoice, getPracticeId());

        return in;
    }

    // DELETE
    public void delete(Long patientId, Long encounterId, String fhirId) {
        log.debug("Deleting FHIR Invoice: {}", fhirId);
        fhirClientService.delete(Invoice.class, fhirId, getPracticeId());
    }

    // -------- FHIR Mapping --------

    private Invoice toFhirInvoice(InvoiceDto dto) {
        Invoice inv = new Invoice();

        // Status
        if (dto.getStatus() != null) {
            switch (dto.getStatus().toLowerCase()) {
                case "draft" -> inv.setStatus(Invoice.InvoiceStatus.DRAFT);
                case "issued" -> inv.setStatus(Invoice.InvoiceStatus.ISSUED);
                case "balanced" -> inv.setStatus(Invoice.InvoiceStatus.BALANCED);
                case "cancelled" -> inv.setStatus(Invoice.InvoiceStatus.CANCELLED);
                case "entered-in-error" -> inv.setStatus(Invoice.InvoiceStatus.ENTEREDINERROR);
                default -> inv.setStatus(Invoice.InvoiceStatus.DRAFT);
            }
        }

        // Subject (Patient)
        if (dto.getPatientId() != null) {
            inv.setSubject(new Reference("Patient/" + dto.getPatientId()));
        }

        // Encounter extension
        if (dto.getEncounterId() != null) {
            inv.addExtension(new Extension(EXT_ENCOUNTER, new Reference("Encounter/" + dto.getEncounterId())));
        }

        // Invoice number
        if (dto.getInvoiceNumber() != null) {
            inv.addIdentifier().setValue(dto.getInvoiceNumber());
            inv.addExtension(new Extension(EXT_INVOICE_NUMBER, new StringType(dto.getInvoiceNumber())));
        }

        // Issue date
        if (dto.getIssueDate() != null) {
            inv.setDateElement(new DateTimeType(dto.getIssueDate()));
        }

        // Due date extension
        if (dto.getDueDate() != null) {
            inv.addExtension(new Extension(EXT_DUE_DATE, new StringType(dto.getDueDate())));
        }

        // Payer extension
        if (dto.getPayer() != null) {
            inv.addExtension(new Extension(EXT_PAYER, new StringType(dto.getPayer())));
        }

        // Currency extension
        if (dto.getCurrency() != null) {
            inv.addExtension(new Extension(EXT_CURRENCY, new StringType(dto.getCurrency())));
        }

        // Notes
        if (dto.getNotes() != null) {
            inv.addNote().setText(dto.getNotes());
        }

        // Total gross
        if (dto.getTotalGross() != null) {
            Money totalGross = new Money();
            totalGross.setValue(new BigDecimal(dto.getTotalGross()));
            totalGross.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "USD");
            inv.setTotalGross(totalGross);
        }

        // Total net
        if (dto.getTotalNet() != null) {
            Money totalNet = new Money();
            totalNet.setValue(new BigDecimal(dto.getTotalNet()));
            totalNet.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "USD");
            inv.setTotalNet(totalNet);
        }

        // Line items
        if (dto.getLines() != null) {
            for (LineDto line : dto.getLines()) {
                Invoice.InvoiceLineItemComponent lineItem = inv.addLineItem();
                
                // Description as ChargeItemDefinition reference or text
                lineItem.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-description", 
                        new StringType(line.getDescription())));
                
                if (line.getCode() != null) {
                    lineItem.addExtension(new Extension(EXT_LINE_CODE, new StringType(line.getCode())));
                }
                if (line.getQuantity() != null) {
                    lineItem.addExtension(new Extension(EXT_LINE_QUANTITY, new IntegerType(line.getQuantity())));
                }
                if (line.getUnitPrice() != null) {
                    lineItem.addExtension(new Extension(EXT_LINE_UNIT_PRICE, new StringType(line.getUnitPrice())));
                }

                // Price component for amount
                if (line.getAmount() != null) {
                    Invoice.InvoiceLineItemPriceComponentComponent priceComponent = lineItem.addPriceComponent();
                    priceComponent.setType(Invoice.InvoicePriceComponentType.BASE);
                    Money amount = new Money();
                    amount.setValue(new BigDecimal(line.getAmount()));
                    amount.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "USD");
                    priceComponent.setAmount(amount);
                }
            }
        }

        // Payments as extension (FHIR Invoice doesn't have native payment tracking)
        if (dto.getPayments() != null && !dto.getPayments().isEmpty()) {
            StringBuilder paymentsJson = new StringBuilder("[");
            for (int i = 0; i < dto.getPayments().size(); i++) {
                PaymentDto p = dto.getPayments().get(i);
                if (i > 0) paymentsJson.append(",");
                paymentsJson.append("{")
                        .append("\"date\":\"").append(p.getDate() != null ? p.getDate() : "").append("\",")
                        .append("\"amount\":\"").append(p.getAmount() != null ? p.getAmount() : "").append("\",")
                        .append("\"method\":\"").append(p.getMethod() != null ? p.getMethod() : "").append("\",")
                        .append("\"reference\":\"").append(p.getReference() != null ? p.getReference() : "").append("\",")
                        .append("\"note\":\"").append(p.getNote() != null ? p.getNote() : "").append("\"")
                        .append("}");
            }
            paymentsJson.append("]");
            inv.addExtension(new Extension(EXT_PAYMENTS, new StringType(paymentsJson.toString())));
        }

        return inv;
    }

    private InvoiceDto fromFhirInvoice(Invoice inv) {
        InvoiceDto dto = new InvoiceDto();
        dto.setExternalId(inv.getIdElement().getIdPart());

        // Status
        if (inv.hasStatus()) {
            dto.setStatus(inv.getStatus().toCode());
        }

        // Subject -> patientId
        if (inv.hasSubject() && inv.getSubject().hasReference()) {
            String ref = inv.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring("Patient/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Encounter extension
        Extension encExt = inv.getExtensionByUrl(EXT_ENCOUNTER);
        if (encExt != null && encExt.getValue() instanceof Reference) {
            String ref = ((Reference) encExt.getValue()).getReference();
            if (ref != null && ref.startsWith("Encounter/")) {
                try {
                    dto.setEncounterId(Long.parseLong(ref.substring("Encounter/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Invoice number
        dto.setInvoiceNumber(getExtensionString(inv, EXT_INVOICE_NUMBER));

        // Issue date
        if (inv.hasDate()) {
            dto.setIssueDate(inv.getDateElement().getValueAsString());
        }

        // Due date
        dto.setDueDate(getExtensionString(inv, EXT_DUE_DATE));

        // Payer
        dto.setPayer(getExtensionString(inv, EXT_PAYER));

        // Currency
        dto.setCurrency(getExtensionString(inv, EXT_CURRENCY));

        // Notes
        if (inv.hasNote()) {
            dto.setNotes(inv.getNoteFirstRep().getText());
        }

        // Total gross
        if (inv.hasTotalGross()) {
            dto.setTotalGross(inv.getTotalGross().getValue().toPlainString());
        }

        // Total net
        if (inv.hasTotalNet()) {
            dto.setTotalNet(inv.getTotalNet().getValue().toPlainString());
        }

        // Line items
        List<LineDto> lines = new ArrayList<>();
        for (Invoice.InvoiceLineItemComponent lineItem : inv.getLineItem()) {
            LineDto line = new LineDto();
            
            Extension descExt = lineItem.getExtensionByUrl("http://ciyex.com/fhir/StructureDefinition/line-description");
            if (descExt != null && descExt.getValue() instanceof StringType) {
                line.setDescription(((StringType) descExt.getValue()).getValue());
            }
            
            Extension codeExt = lineItem.getExtensionByUrl(EXT_LINE_CODE);
            if (codeExt != null && codeExt.getValue() instanceof StringType) {
                line.setCode(((StringType) codeExt.getValue()).getValue());
            }
            
            Extension qtyExt = lineItem.getExtensionByUrl(EXT_LINE_QUANTITY);
            if (qtyExt != null && qtyExt.getValue() instanceof IntegerType) {
                line.setQuantity(((IntegerType) qtyExt.getValue()).getValue());
            }
            
            Extension priceExt = lineItem.getExtensionByUrl(EXT_LINE_UNIT_PRICE);
            if (priceExt != null && priceExt.getValue() instanceof StringType) {
                line.setUnitPrice(((StringType) priceExt.getValue()).getValue());
            }

            if (lineItem.hasPriceComponent()) {
                line.setAmount(lineItem.getPriceComponentFirstRep().getAmount().getValue().toPlainString());
            }
            
            lines.add(line);
        }
        dto.setLines(lines);

        // Payments from extension
        String paymentsJson = getExtensionString(inv, EXT_PAYMENTS);
        if (paymentsJson != null) {
            dto.setPayments(parsePaymentsJson(paymentsJson));
        }

        return dto;
    }

    // -------- Helpers --------

    private boolean hasEncounter(Invoice inv, Long encounterId) {
        Extension encExt = inv.getExtensionByUrl(EXT_ENCOUNTER);
        if (encExt != null && encExt.getValue() instanceof Reference) {
            String ref = ((Reference) encExt.getValue()).getReference();
            return ref != null && ref.equals("Encounter/" + encounterId);
        }
        return false;
    }

    private String getExtensionString(Invoice inv, String url) {
        Extension ext = inv.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private BigDecimal calculateGross(List<LineDto> lines) {
        if (lines == null) return BigDecimal.ZERO;
        return lines.stream()
                .map(l -> l.getAmount() != null ? new BigDecimal(l.getAmount()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculatePaid(List<PaymentDto> payments) {
        if (payments == null) return BigDecimal.ZERO;
        return payments.stream()
                .map(p -> p.getAmount() != null ? new BigDecimal(p.getAmount()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<PaymentDto> parsePaymentsJson(String json) {
        List<PaymentDto> payments = new ArrayList<>();
        // Simple JSON parsing for payments array
        if (json == null || json.equals("[]")) return payments;
        
        try {
            String content = json.substring(1, json.length() - 1); // Remove [ ]
            if (content.isEmpty()) return payments;
            
            // Split by },{ pattern
            String[] items = content.split("\\},\\{");
            for (String item : items) {
                item = item.replace("{", "").replace("}", "");
                PaymentDto p = new PaymentDto();
                for (String pair : item.split(",")) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].replace("\"", "").trim();
                        String value = kv[1].replace("\"", "").trim();
                        switch (key) {
                            case "date" -> p.setDate(value.isEmpty() ? null : value);
                            case "amount" -> p.setAmount(value.isEmpty() ? null : value);
                            case "method" -> p.setMethod(value.isEmpty() ? null : value);
                            case "reference" -> p.setReference(value.isEmpty() ? null : value);
                            case "note" -> p.setNote(value.isEmpty() ? null : value);
                        }
                    }
                }
                payments.add(p);
            }
        } catch (Exception e) {
            log.warn("Failed to parse payments JSON: {}", e.getMessage());
        }
        return payments;
    }

    private void validateInvoiceDto(InvoiceDto dto) {
        List<String> errors = new ArrayList<>();

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
                }
                if (!StringUtils.hasText(line.getAmount())) {
                    errors.add(prefix + "Amount is mandatory");
                }
            }
        }

        if (!errors.isEmpty()) {
            String errorMessage = "Validation failed: " + String.join("; ", errors);
            log.error("Invoice validation failed: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
