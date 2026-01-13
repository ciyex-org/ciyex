package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.*;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientInvoiceService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // FHIR Extension URLs
    private static final String EXT_PATIENT = "http://ciyex.com/fhir/StructureDefinition/patient-reference";
    private static final String EXT_LINE_ID = "http://ciyex.com/fhir/StructureDefinition/line-id";
    private static final String EXT_INVOICE_DATE = "http://ciyex.com/fhir/StructureDefinition/invoice-date";
    private static final String EXT_INVOICE_STATUS = "http://ciyex.com/fhir/StructureDefinition/invoice-status";
    private static final String EXT_PT_BALANCE = "http://ciyex.com/fhir/StructureDefinition/pt-balance";
    private static final String EXT_INS_BALANCE = "http://ciyex.com/fhir/StructureDefinition/ins-balance";
    private static final String EXT_TOTAL_CHARGE = "http://ciyex.com/fhir/StructureDefinition/total-charge";
    private static final String EXT_INVOICE_TYPE = "http://ciyex.com/fhir/StructureDefinition/invoice-type";
    private static final String EXT_INS_WO = "http://ciyex.com/fhir/StructureDefinition/ins-writeoff";
    private static final String EXT_BACKDATE = "http://ciyex.com/fhir/StructureDefinition/backdate";
    
    // Line component extensions
    private static final String EXT_LINE_CODE = "http://ciyex.com/fhir/StructureDefinition/line-code";
    private static final String EXT_LINE_TREATMENT = "http://ciyex.com/fhir/StructureDefinition/line-treatment";
    private static final String EXT_LINE_PROVIDER = "http://ciyex.com/fhir/StructureDefinition/line-provider";
    private static final String EXT_LINE_DOS = "http://ciyex.com/fhir/StructureDefinition/line-dos";
    private static final String EXT_LINE_CHARGE = "http://ciyex.com/fhir/StructureDefinition/line-charge";
    private static final String EXT_LINE_ALLOWED = "http://ciyex.com/fhir/StructureDefinition/line-allowed";
    private static final String EXT_LINE_INS_WRITEOFF = "http://ciyex.com/fhir/StructureDefinition/line-ins-writeoff";
    private static final String EXT_LINE_INS_PORTION = "http://ciyex.com/fhir/StructureDefinition/line-ins-portion";
    private static final String EXT_LINE_PT_PORTION = "http://ciyex.com/fhir/StructureDefinition/line-pt-portion";
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // ===================== Request Records =====================
    public record ProcedureLineRequest(String code, String description, BigDecimal rate) {}
    public record CreateInvoiceRequest(String provider, String dos, List<ProcedureLineRequest> procedures) {}
    public record UpdateInvoiceRequest(String code, String description, String provider, String dos, BigDecimal rate) {}
    public record UpdateLineAmountRequest(BigDecimal newCharge) {}
    public record PercentageAdjustmentRequest(int percent) {}
    public record BackdateRequest(String date) {}
    public record AccountAdjustmentRequest(String adjustmentType, BigDecimal flatRate, BigDecimal specificAmount, String description, Boolean includeCourtesyCredit) {}

    // ===================== Invoices =====================

    /**
     * Backdate invoice date
     */
    public PatientInvoiceDto backdateInvoice(Long patientId, Long invoiceId, BackdateRequest req) {
        Observation obs = getInvoiceOrThrow(patientId, invoiceId);
        if (req != null && req.date() != null) {
            obs.getExtension().removeIf(e -> EXT_BACKDATE.equals(e.getUrl()));
            obs.addExtension(new Extension(EXT_BACKDATE, new StringType(req.date())));
            fhirClientService.update(obs, getPracticeId());
        }
        return fromFhirObservation(obs);
    }

    /**
     * Account adjustment - applies credit adjustments based on type
     */
    public PatientAccountCreditDto accountAdjustment(Long patientId, AccountAdjustmentRequest req) {
        if (req == null || req.adjustmentType() == null) {
            throw new IllegalArgumentException("Adjustment type is required");
        }

        BigDecimal adjustmentAmount = BigDecimal.ZERO;

        switch (req.adjustmentType()) {
            case "Flat-rate" -> adjustmentAmount = nz(req.flatRate());
            case "Total Outstanding" -> {
                // Calculate total outstanding from all patient invoices
                List<PatientInvoiceDto> invoices = listInvoices(patientId);
                adjustmentAmount = invoices.stream()
                        .map(inv -> nz(inv.ptBalance()).add(nz(inv.insBalance())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            case "Patient Outstanding" -> {
                // Calculate only patient portion outstanding
                List<PatientInvoiceDto> invoices = listInvoices(patientId);
                adjustmentAmount = invoices.stream()
                        .map(inv -> nz(inv.ptBalance()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            case "Specific" -> adjustmentAmount = nz(req.specificAmount());
            default -> throw new IllegalArgumentException("Invalid adjustment type: " + req.adjustmentType());
        }

        // Apply courtesy credit if checkbox is selected
        if (Boolean.TRUE.equals(req.includeCourtesyCredit())) {
            log.info("Courtesy credit included in adjustment");
        }

        log.info("Account adjustment applied: type={}, amount={}, patientId={}",
                req.adjustmentType(), adjustmentAmount, patientId);

        return new PatientAccountCreditDto(patientId, adjustmentAmount);
    }

    /**
     * Adjust invoice with percentage discount or flat adjustment
     */
    public PatientInvoiceDto adjustInvoice(Long patientId, Long invoiceId, InvoiceAdjustmentRequest req) {
        Observation obs = getInvoiceOrThrow(patientId, invoiceId);

        if (req == null || req.adjustmentType() == null) {
            throw new IllegalArgumentException("Adjustment type is required");
        }

        // Apply percentage discount if provided
        if (req.percentageDiscount() != null && req.percentageDiscount() > 0) {
            int percent = req.percentageDiscount();
            BigDecimal discountFactor = BigDecimal.valueOf(percent)
                    .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

            for (Observation.ObservationComponentComponent component : obs.getComponent()) {
                BigDecimal originalCharge = getCompDecimal(component, EXT_LINE_CHARGE);
                BigDecimal discount = originalCharge.multiply(discountFactor);
                BigDecimal newCharge = originalCharge.subtract(discount).max(BigDecimal.ZERO);

                setCompDecimal(component, EXT_LINE_CHARGE, newCharge);
                setCompDecimal(component, EXT_LINE_ALLOWED, newCharge);

                // Recalculate portions proportionally
                BigDecimal insPortion = getCompDecimal(component, EXT_LINE_INS_PORTION);
                BigDecimal ptPortion = getCompDecimal(component, EXT_LINE_PT_PORTION);
                BigDecimal totalBefore = nz(insPortion).add(nz(ptPortion));
                
                if (totalBefore.signum() > 0) {
                    BigDecimal factor = newCharge.divide(totalBefore, 8, RoundingMode.HALF_UP);
                    setCompDecimal(component, EXT_LINE_INS_PORTION, nz(insPortion).multiply(factor));
                    setCompDecimal(component, EXT_LINE_PT_PORTION, nz(ptPortion).multiply(factor));
                } else {
                    // Default: split based on insurance vs patient responsibility
                    setCompDecimal(component, EXT_LINE_INS_PORTION, newCharge);
                    setCompDecimal(component, EXT_LINE_PT_PORTION, BigDecimal.ZERO);
                }

                setCompDecimal(component, EXT_LINE_INS_WRITEOFF, BigDecimal.ZERO);
            }
        }

        // Apply flat adjustment amount if provided
        if (req.adjustmentAmount() != null && req.adjustmentAmount().signum() != 0) {
            // Store adjustment as account credit (not implemented in FHIR yet)
            log.info("Flat adjustment amount: {}", req.adjustmentAmount());
        }

        // Recalculate invoice totals
        recalcInvoiceTotals(obs);
        fhirClientService.update(obs, getPracticeId());

        log.info("Invoice adjusted: invoiceId={}, type={}, discount={}%, amount={}",
                invoiceId, req.adjustmentType(), req.percentageDiscount(), req.adjustmentAmount());

        return fromFhirObservation(obs);
    }

    /**
     * List all invoices for a patient
     */
    public List<PatientInvoiceDto> listInvoices(Long patientId) {
        Bundle bundle = fhirClientService.search(Observation.class, getPracticeId());
        return fhirClientService.extractResources(bundle, Observation.class).stream()
                .filter(obs -> patientId.equals(getPatientIdFromObs(obs)) && "INVOICE".equals(getStringExt(obs, EXT_INVOICE_TYPE)))
                .map(this::fromFhirObservation)
                .sorted((a, b) -> Long.compare(b.id(), a.id()))
                .collect(Collectors.toList());
    }

    /**
     * Get invoice lines for a specific invoice
     */
    public List<PatientInvoiceLineDto> getInvoiceLines(Long patientId, Long invoiceId) {
        Observation obs = getInvoiceOrThrow(patientId, invoiceId);
        return extractInvoiceLines(obs);
    }

    /**
     * Create invoice from procedure request
     */
    public PatientInvoiceDto createInvoiceFromProcedure(Long patientId, CreateInvoiceRequest b) {
        if (b == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (b.dos() == null || b.dos().isEmpty()) {
            throw new IllegalArgumentException("Date of service is required");
        }
        if (b.procedures() == null || b.procedures().isEmpty()) {
            throw new IllegalArgumentException("At least one procedure is required");
        }

        // Create invoice observation
        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setCode(new CodeableConcept(new Coding("http://ciyex.com/fhir/CodeSystem/invoice", "INVOICE", "Patient Invoice")));
        obs.addExtension(new Extension(EXT_PATIENT, new StringType(patientId.toString())));
        obs.addExtension(new Extension(EXT_INVOICE_DATE, new StringType(b.dos())));
        obs.addExtension(new Extension(EXT_INVOICE_STATUS, new StringType("OPEN")));
        obs.addExtension(new Extension(EXT_INVOICE_TYPE, new StringType("INVOICE")));

        BigDecimal totalCharge = BigDecimal.ZERO;
        int lineIndex = 0;
        
        // Create invoice lines for each procedure
        for (ProcedureLineRequest proc : b.procedures()) {
            if (proc.code() == null || proc.code().isEmpty()) {
                throw new IllegalArgumentException("Procedure code is required");
            }
            if (proc.rate() == null) {
                throw new IllegalArgumentException("Rate is required for procedure " + proc.code());
            }

            Observation.ObservationComponentComponent component = new Observation.ObservationComponentComponent();
            component.setCode(new CodeableConcept(new Coding("http://ciyex.com/fhir/CodeSystem/line", "LINE_" + lineIndex, "Invoice Line")));
            component.addExtension(new Extension(EXT_LINE_ID, new IntegerType(lineIndex)));
            component.addExtension(new Extension(EXT_LINE_CODE, new StringType(proc.code())));
            component.addExtension(new Extension(EXT_LINE_TREATMENT, new StringType(proc.description())));
            component.addExtension(new Extension(EXT_LINE_PROVIDER, new StringType(b.provider())));
            component.addExtension(new Extension(EXT_LINE_DOS, new StringType(b.dos())));
            component.addExtension(new Extension(EXT_LINE_CHARGE, new DecimalType(proc.rate())));
            component.addExtension(new Extension(EXT_LINE_ALLOWED, new DecimalType(proc.rate())));
            component.addExtension(new Extension(EXT_LINE_INS_PORTION, new DecimalType(proc.rate())));
            component.addExtension(new Extension(EXT_LINE_PT_PORTION, new DecimalType(BigDecimal.ZERO)));
            component.addExtension(new Extension(EXT_LINE_INS_WRITEOFF, new DecimalType(BigDecimal.ZERO)));
            
            obs.addComponent(component);
            totalCharge = totalCharge.add(nz(proc.rate()));
            lineIndex++;
        }

        // Set invoice totals
        obs.addExtension(new Extension(EXT_TOTAL_CHARGE, new DecimalType(totalCharge)));
        obs.addExtension(new Extension(EXT_PT_BALANCE, new DecimalType(BigDecimal.ZERO)));
        obs.addExtension(new Extension(EXT_INS_BALANCE, new DecimalType(totalCharge)));

        // Save invoice
        var outcome = fhirClientService.create(obs, getPracticeId());
        String fhirId = outcome.getId().getIdPart();
        Observation created = fhirClientService.read(Observation.class, fhirId, getPracticeId());
        
        // Seed a draft claim
        try {
            Long invoiceId = Long.parseLong(fhirId);
            createDraftClaimForInvoice(patientId, invoiceId, b.dos());
        } catch (Exception ex) {
            log.error("Failed to auto-create claim for invoice {}", fhirId, ex);
        }

        return fromFhirObservation(created);
    }

    /**
     * Create a draft claim for an invoice
     */
    private void createDraftClaimForInvoice(Long patientId, Long invoiceId, String dos) {
        Claim claim = new Claim();
        claim.addExtension(new Extension(EXT_PATIENT, new StringType(patientId.toString())));
        claim.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/invoice-reference", new StringType(invoiceId.toString())));
        claim.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/claim-status", new StringType("DRAFT")));
        claim.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/created-on", 
                new StringType(dos != null ? dos : LocalDate.now().format(DATE_FORMATTER))));
        claim.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/claim-type", new StringType("Electronic")));
        fhirClientService.create(claim, getPracticeId());
    }

    /**
     * Delete an invoice
     */
    public void deleteInvoice(Long patientId, Long invoiceId) {
        Observation obs = getInvoiceOrThrow(patientId, invoiceId);

        try {
            // Delete associated claim if exists
            deleteClaim(invoiceId);
            
            // Delete invoice
            fhirClientService.delete(Observation.class, obs.getIdElement().getIdPart(), getPracticeId());
        } catch (Exception ex) {
            log.error("Error deleting invoice {} for patient {}", invoiceId, patientId, ex);
            throw new RuntimeException("Failed to delete invoice: " + ex.getMessage(), ex);
        }
    }

    /**
     * Update invoice from procedure request
     */
    public PatientInvoiceDto updateInvoiceFromProcedure(Long patientId, Long invoiceId, UpdateInvoiceRequest b) {
        if (b == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        Observation obs = getInvoiceOrThrow(patientId, invoiceId);

        // Update the first line (assuming single line invoice from procedure)
        if (obs.getComponent().isEmpty()) {
            throw new IllegalArgumentException("Invoice has no lines to update");
        }

        Observation.ObservationComponentComponent line = obs.getComponent().get(0);

        // Update line fields
        if (b.code() != null) {
            setCompString(line, EXT_LINE_CODE, b.code());
        }
        if (b.description() != null) {
            setCompString(line, EXT_LINE_TREATMENT, b.description());
        }
        if (b.provider() != null) {
            setCompString(line, EXT_LINE_PROVIDER, b.provider());
        }
        if (b.dos() != null) {
            setCompString(line, EXT_LINE_DOS, b.dos());
        }

        if (b.rate() != null) {
            BigDecimal rate = nz(b.rate());
            setCompDecimal(line, EXT_LINE_CHARGE, rate);
            setCompDecimal(line, EXT_LINE_ALLOWED, rate);
            setCompDecimal(line, EXT_LINE_INS_PORTION, rate);
            setCompDecimal(line, EXT_LINE_PT_PORTION, BigDecimal.ZERO);
        }

        recalcInvoiceTotals(obs);
        fhirClientService.update(obs, getPracticeId());

        return fromFhirObservation(obs);
    }

    /**
     * Update invoice line amount
     */
    public PatientInvoiceDto updateInvoiceLineAmount(Long patientId, Long invoiceId, Long lineId, UpdateLineAmountRequest b) {
        if (b == null || b.newCharge() == null) {
            throw new IllegalArgumentException("New charge amount is required");
        }

        Observation obs = getInvoiceOrThrow(patientId, invoiceId);

        if (!obs.getComponent().isEmpty()) {
            // For FHIR model, we find the line by its position/index
            Observation.ObservationComponentComponent component = obs.getComponent().get(0);
            
            BigDecimal amt = nz(b.newCharge());
            setCompDecimal(component, EXT_LINE_CHARGE, amt);
            setCompDecimal(component, EXT_LINE_ALLOWED, amt);
            setCompDecimal(component, EXT_LINE_INS_WRITEOFF, BigDecimal.ZERO);
            setCompDecimal(component, EXT_LINE_INS_PORTION, amt);
            setCompDecimal(component, EXT_LINE_PT_PORTION, BigDecimal.ZERO);
        }

        recalcInvoiceTotals(obs);
        fhirClientService.update(obs, getPracticeId());

        return fromFhirObservation(obs);
    }

    /**
     * Apply percentage adjustment to invoice
     */
    public PatientInvoiceDto applyInvoicePercentageAdjustment(Long patientId, Long invoiceId, PercentageAdjustmentRequest b) {
        if (b == null) {
            throw new IllegalArgumentException("Percentage adjustment request is required");
        }

        Observation obs = getInvoiceOrThrow(patientId, invoiceId);
        int percent = b.percent();
        BigDecimal p = BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

        for (Observation.ObservationComponentComponent component : obs.getComponent()) {
            BigDecimal charge = getCompDecimal(component, EXT_LINE_CHARGE);
            BigDecimal delta = charge.multiply(p);
            BigDecimal newCharge = charge.subtract(delta).max(BigDecimal.ZERO);
            
            setCompDecimal(component, EXT_LINE_CHARGE, newCharge);
            setCompDecimal(component, EXT_LINE_ALLOWED, newCharge);
            
            // Keep proportions simple
            BigDecimal insPortion = getCompDecimal(component, EXT_LINE_INS_PORTION);
            BigDecimal ptPortion = getCompDecimal(component, EXT_LINE_PT_PORTION);
            BigDecimal totalBefore = nz(insPortion).add(nz(ptPortion));
            
            if (totalBefore.signum() == 0) {
                setCompDecimal(component, EXT_LINE_INS_PORTION, BigDecimal.ZERO);
                setCompDecimal(component, EXT_LINE_PT_PORTION, BigDecimal.ZERO);
            } else {
                // Scale down proportionally
                BigDecimal factor = newCharge.divide(nz(totalBefore), 8, RoundingMode.HALF_UP);
                setCompDecimal(component, EXT_LINE_INS_PORTION, nz(insPortion).multiply(factor));
                setCompDecimal(component, EXT_LINE_PT_PORTION, nz(ptPortion).multiply(factor));
            }
            
            setCompDecimal(component, EXT_LINE_INS_WRITEOFF, BigDecimal.ZERO);
        }
        
        recalcInvoiceTotals(obs);
        fhirClientService.update(obs, getPracticeId());

        return fromFhirObservation(obs);
    }

    /**
     * Transfer INS balance to PT balance
     */
    public PatientInvoiceDto transferOutstandingToPatient(Long patientId, Long invoiceId, Double amount) {
        Observation obs = getInvoiceOrThrow(patientId, invoiceId);
        if (obs == null || amount == null || amount <= 0) {
            return fromFhirObservation(obs);
        }
        
        BigDecimal amt = BigDecimal.valueOf(amount);
        BigDecimal insBal = getDecimalExt(obs, EXT_INS_BALANCE);
        BigDecimal ptBal = getDecimalExt(obs, EXT_PT_BALANCE);
        
        // Cap the transfer amount to available insurance balance
        if (insBal.compareTo(amt) < 0) {
            amt = insBal;
        }
        
        setDecimalExt(obs, EXT_INS_BALANCE, insBal.subtract(amt));
        setDecimalExt(obs, EXT_PT_BALANCE, ptBal.add(amt));
        
        fhirClientService.update(obs, getPracticeId());
        return fromFhirObservation(obs);
    }

    /**
     * Transfer PT balance to INS balance
     */
    public PatientInvoiceDto transferOutstandingToInsurance(Long patientId, Long invoiceId, Double amount) {
        Observation obs = getInvoiceOrThrow(patientId, invoiceId);
        if (obs == null || amount == null || amount <= 0) {
            return fromFhirObservation(obs);
        }
        
        BigDecimal amt = BigDecimal.valueOf(amount);
        BigDecimal ptBal = getDecimalExt(obs, EXT_PT_BALANCE);
        BigDecimal insBal = getDecimalExt(obs, EXT_INS_BALANCE);
        
        // Cap the transfer amount to available patient balance
        if (ptBal.compareTo(amt) < 0) {
            amt = ptBal;
        }
        
        setDecimalExt(obs, EXT_PT_BALANCE, ptBal.subtract(amt));
        setDecimalExt(obs, EXT_INS_BALANCE, insBal.add(amt));
        
        fhirClientService.update(obs, getPracticeId());
        return fromFhirObservation(obs);
    }

    // ===================== Helpers =====================

    /**
     * Get invoice or throw exception if not found or doesn't belong to patient
     */
    private Observation getInvoiceOrThrow(Long patientId, Long invoiceId) {
        Observation obs = fhirClientService.read(Observation.class, invoiceId.toString(), getPracticeId());
        if (obs == null || !patientId.equals(getPatientIdFromObs(obs))) {
            throw new IllegalArgumentException(String.format(
                "Invoice not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Invoice ID are correct and that the invoice belongs to this patient.",
                invoiceId, patientId));
        }
        return obs;
    }

    /**
     * Delete claim associated with invoice
     */
    private void deleteClaim(Long invoiceId) {
        try {
            Bundle bundle = fhirClientService.search(Claim.class, getPracticeId());
            fhirClientService.extractResources(bundle, Claim.class).stream()
                    .filter(claim -> invoiceId.toString().equals(getStringExt(claim, "http://ciyex.com/fhir/StructureDefinition/invoice-reference")))
                    .forEach(claim -> {
                        try {
                            fhirClientService.delete(Claim.class, claim.getIdElement().getIdPart(), getPracticeId());
                        } catch (Exception e) {
                            log.warn("Failed to delete claim for invoice {}", invoiceId, e);
                        }
                    });
        } catch (Exception e) {
            log.warn("Failed to delete associated claim for invoice {}", invoiceId, e);
        }
    }

    /**
     * Extract patient ID from observation
     */
    private Long getPatientIdFromObs(Observation obs) {
        String val = getStringExt(obs, EXT_PATIENT);
        return val != null ? Long.parseLong(val) : null;
    }

    /**
     * Get string extension value from observation
     */
    private String getStringExt(Observation obs, String url) {
        if (obs == null) return null;
        Extension ext = obs.getExtensionByUrl(url);
        return (ext != null && ext.getValue() instanceof StringType) ? ((StringType) ext.getValue()).getValue() : null;
    }

    /**
     * Get decimal extension value from observation
     */
    private BigDecimal getDecimalExt(Observation obs, String url) {
        if (obs == null) return BigDecimal.ZERO;
        Extension ext = obs.getExtensionByUrl(url);
        return (ext != null && ext.getValue() instanceof DecimalType) ? ((DecimalType) ext.getValue()).getValue() : BigDecimal.ZERO;
    }

    /**
     * Set decimal extension on observation
     */
    private void setDecimalExt(Observation obs, String url, BigDecimal val) {
        obs.getExtension().removeIf(e -> url.equals(e.getUrl()));
        obs.addExtension(new Extension(url, new DecimalType(val)));
    }

    /**
     * Get decimal value from component extension
     */
    private BigDecimal getCompDecimal(Observation.ObservationComponentComponent comp, String url) {
        if (comp == null) return BigDecimal.ZERO;
        Extension ext = comp.getExtensionByUrl(url);
        return (ext != null && ext.getValue() instanceof DecimalType) ? ((DecimalType) ext.getValue()).getValue() : BigDecimal.ZERO;
    }

    /**
     * Set decimal value on component extension
     */
    private void setCompDecimal(Observation.ObservationComponentComponent comp, String url, BigDecimal val) {
        comp.getExtension().removeIf(e -> url.equals(e.getUrl()));
        comp.addExtension(new Extension(url, new DecimalType(val)));
    }

    /**
     * Get string value from component extension
     */
    private String getCompString(Observation.ObservationComponentComponent comp, String url) {
        if (comp == null) return null;
        Extension ext = comp.getExtensionByUrl(url);
        return (ext != null && ext.getValue() instanceof StringType) ? ((StringType) ext.getValue()).getValue() : null;
    }

    /**
     * Set string value on component extension
     */
    private void setCompString(Observation.ObservationComponentComponent comp, String url, String val) {
        comp.getExtension().removeIf(e -> url.equals(e.getUrl()));
        comp.addExtension(new Extension(url, new StringType(val)));
    }

    /**
     * Extract invoice lines from observation components
     */
    private List<PatientInvoiceLineDto> extractInvoiceLines(Observation obs) {
        List<PatientInvoiceLineDto> lines = new ArrayList<>();
        if (obs == null) return lines;
        
        for (Observation.ObservationComponentComponent component : obs.getComponent()) {
            Extension lineIdExt = component.getExtensionByUrl(EXT_LINE_ID);
            Long lineId = (lineIdExt != null && lineIdExt.getValue() instanceof IntegerType) 
                    ? ((IntegerType) lineIdExt.getValue()).getValue().longValue() 
                    : Long.valueOf(component.hashCode());
            
            String dosStr = getCompString(component, EXT_LINE_DOS);
            LocalDate dos = dosStr != null ? LocalDate.parse(dosStr, DATE_FORMATTER) : null;
            
            lines.add(new PatientInvoiceLineDto(
                    lineId,
                    dos,
                    getCompString(component, EXT_LINE_CODE),
                    getCompString(component, EXT_LINE_TREATMENT),
                    getCompString(component, EXT_LINE_PROVIDER),
                    getCompDecimal(component, EXT_LINE_CHARGE),
                    getCompDecimal(component, EXT_LINE_ALLOWED),
                    getCompDecimal(component, EXT_LINE_INS_WRITEOFF),
                    getCompDecimal(component, EXT_LINE_INS_PORTION),
                    getCompDecimal(component, EXT_LINE_PT_PORTION)
            ));
        }
        return lines;
    }

    /**
     * Null-zero helper
     */
    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * Recalculate invoice totals from component lines
     */
    private void recalcInvoiceTotals(Observation obs) {
        BigDecimal totalCharge = BigDecimal.ZERO;
        BigDecimal totalInsPortion = BigDecimal.ZERO;
        BigDecimal totalPtPortion = BigDecimal.ZERO;
        
        for (Observation.ObservationComponentComponent component : obs.getComponent()) {
            totalCharge = totalCharge.add(getCompDecimal(component, EXT_LINE_CHARGE));
            totalInsPortion = totalInsPortion.add(getCompDecimal(component, EXT_LINE_INS_PORTION));
            totalPtPortion = totalPtPortion.add(getCompDecimal(component, EXT_LINE_PT_PORTION));
        }
        
        setDecimalExt(obs, EXT_TOTAL_CHARGE, totalCharge);
        setDecimalExt(obs, EXT_INS_BALANCE, totalInsPortion);
        setDecimalExt(obs, EXT_PT_BALANCE, totalPtPortion);
    }

    /**
     * Convert FHIR observation to DTO
     */
    private PatientInvoiceDto fromFhirObservation(Observation obs) {
        if (obs == null) return null;
        
        String fhirId = obs.getIdElement().getIdPart();
        Long patientId = getPatientIdFromObs(obs);
        String statusStr = getStringExt(obs, EXT_INVOICE_STATUS);
        PatientInvoiceStatus status = statusStr != null ? PatientInvoiceStatus.valueOf(statusStr) : PatientInvoiceStatus.OPEN;
        // Parse invoice date if present
        String invDateStr = getStringExt(obs, EXT_INVOICE_DATE);
        java.time.LocalDateTime invoiceDate = null;
        if (invDateStr != null) {
            try {
                invoiceDate = java.time.LocalDateTime.parse(invDateStr);
            } catch (Exception ex) {
                try {
                    invoiceDate = java.time.LocalDate.parse(invDateStr, DATE_FORMATTER).atStartOfDay();
                } catch (Exception inner) {
                    invoiceDate = null;
                }
            }
        }
        
        BigDecimal insWO = getDecimalExt(obs, EXT_INS_WO);
        BigDecimal appliedWO = BigDecimal.ZERO;
        BigDecimal ptBalance = getDecimalExt(obs, EXT_PT_BALANCE);
        BigDecimal insBalance = getDecimalExt(obs, EXT_INS_BALANCE);
        BigDecimal totalCharge = getDecimalExt(obs, EXT_TOTAL_CHARGE);
        
        List<PatientInvoiceLineDto> lines = extractInvoiceLines(obs);
        
        return new PatientInvoiceDto(
                Long.parseLong(fhirId),
                patientId,
                invoiceDate,
                status,
                insWO,
                appliedWO,
                ptBalance,
                insBalance,
                totalCharge,
                lines
        );
    }

    /**
     * Public accessor used by other services to retrieve an invoice DTO
     */
    public PatientInvoiceDto getPatientInvoice(Long patientId, Long invoiceId) {
        Observation obs = getInvoiceOrThrow(patientId, invoiceId);
        return fromFhirObservation(obs);
    }

    // ===================== Claim Methods (Attachment/EOB) =====================

    /**
     * Upload claim attachment
     */
    public void uploadClaimAttachment(Long patientId, Long claimId, MultipartFile file) throws Exception {
        log.warn("Claim attachment upload not yet fully implemented for FHIR backend");
    }

    /**
     * Get claim attachment
     */
    public byte[] getClaimAttachment(Long patientId, Long claimId) {
        log.warn("Claim attachment download not yet fully implemented for FHIR backend");
        return new byte[0];
    }

    /**
     * Extract patient ID from claim
     */
    public Long getPatientIdFromClaim(Claim claim) {
        String val = getStringExt(claim, EXT_PATIENT);
        return val != null ? Long.parseLong(val) : null;
    }

    /**
     * Get string extension from claim
     */
    private String getStringExt(Claim claim, String url) {
        if (claim == null) return null;
        Extension ext = claim.getExtensionByUrl(url);
        return (ext != null && ext.getValue() instanceof StringType) ? ((StringType) ext.getValue()).getValue() : null;
    }
}
