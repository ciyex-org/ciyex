package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.qiaben.ciyex.dto.*;
import com.qiaben.ciyex.entity.PatientClaim;
import com.qiaben.ciyex.entity.PatientInvoice;
import com.qiaben.ciyex.dto.CourtesyCreditRequest;
import com.qiaben.ciyex.dto.PatientDepositRequest;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientBillingService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String EXT_PATIENT = "http://ciyex.com/fhir/StructureDefinition/patient-reference";
    private static final String EXT_INS_WO = "http://ciyex.com/fhir/StructureDefinition/ins-writeoff";
    private static final String EXT_APPLIED_WO = "http://ciyex.com/fhir/StructureDefinition/applied-writeoff";
    private static final String EXT_PT_BALANCE = "http://ciyex.com/fhir/StructureDefinition/pt-balance";
    private static final String EXT_INS_BALANCE = "http://ciyex.com/fhir/StructureDefinition/ins-balance";
    private static final String EXT_TOTAL_CHARGE = "http://ciyex.com/fhir/StructureDefinition/total-charge";
    private static final String EXT_LINE_CODE = "http://ciyex.com/fhir/StructureDefinition/line-code";
    private static final String EXT_LINE_TREATMENT = "http://ciyex.com/fhir/StructureDefinition/line-treatment";
    private static final String EXT_LINE_PROVIDER = "http://ciyex.com/fhir/StructureDefinition/line-provider";
    private static final String EXT_LINE_CHARGE = "http://ciyex.com/fhir/StructureDefinition/line-charge";
    private static final String EXT_LINE_ALLOWED = "http://ciyex.com/fhir/StructureDefinition/line-allowed";
    private static final String EXT_LINE_INS_WO = "http://ciyex.com/fhir/StructureDefinition/line-ins-writeoff";
    private static final String EXT_LINE_INS_PORTION = "http://ciyex.com/fhir/StructureDefinition/line-ins-portion";
    private static final String EXT_LINE_PT_PORTION = "http://ciyex.com/fhir/StructureDefinition/line-pt-portion";
    private static final String EXT_BACKDATE = "http://ciyex.com/fhir/StructureDefinition/backdate";
    private static final String EXT_INVOICE_ID = "http://ciyex.com/fhir/StructureDefinition/invoice-id";
    private static final String EXT_PAYER_NAME = "http://ciyex.com/fhir/StructureDefinition/payer-name";
    private static final String EXT_TREATING_PROVIDER = "http://ciyex.com/fhir/StructureDefinition/treating-provider";
    private static final String EXT_BILLING_ENTITY = "http://ciyex.com/fhir/StructureDefinition/billing-entity";
    private static final String EXT_CLAIM_TYPE = "http://ciyex.com/fhir/StructureDefinition/claim-type";
    private static final String EXT_NOTES = "http://ciyex.com/fhir/StructureDefinition/notes";
    private static final String EXT_ATTACHMENTS = "http://ciyex.com/fhir/StructureDefinition/attachments";
    private static final String EXT_EOB_ATTACHED = "http://ciyex.com/fhir/StructureDefinition/eob-attached";
    private static final String EXT_PATIENT_NAME = "http://ciyex.com/fhir/StructureDefinition/patient-name";
    private static final String EXT_PLAN_NAME = "http://ciyex.com/fhir/StructureDefinition/plan-name";
    private static final String EXT_POLICY_NUMBER = "http://ciyex.com/fhir/StructureDefinition/policy-number";
    private static final String EXT_CREDIT_BALANCE = "http://ciyex.com/fhir/StructureDefinition/credit-balance";

    private String getPracticeId() { return practiceContextService.getPracticeId(); }

    public record ProcedureLineRequest(String code, String description, BigDecimal rate) {}
    public record CreateInvoiceRequest(String provider, String dos, List<ProcedureLineRequest> procedures) {}
    public record UpdateInvoiceRequest(String code, String description, String provider, String dos, BigDecimal rate) {}
    public record UpdateLineAmountRequest(BigDecimal newCharge) {}
    public record PercentageAdjustmentRequest(int percent) {}
    public record ApplyCreditRequest(BigDecimal amount) {}
    public record PatientClaimCoreUpdate(String treatingProviderId, String billingEntity, String type, String notes,
            String attachmentIndicator, String attachmentType, String attachmentTransmissionCode, String claimSubmissionReasonCode) {}
    public record VoidReason(String reason) {}
    public record RefundRequest(BigDecimal amount, String reason) {}
    public record TransferCreditRequest(BigDecimal amount, String note) {}
    public record BackdateRequest(String date) {}
    public record AccountAdjustmentRequest(String adjustmentType, BigDecimal flatRate, BigDecimal specificAmount, 
            String description, Boolean includeCourtesyCredit) {
        public BigDecimal flatRate() { return flatRate; }
        public BigDecimal specificAmount() { return specificAmount; }
        public Boolean includeCourtesyCredit() { return includeCourtesyCredit; }
    }

    public PatientStatementDto getPatientStatement(Long patientId) {
        PatientStatementDto dto = new PatientStatementDto();
        dto.patientId = patientId;
        dto.patientName = "[Patient Name]";
        dto.statementDate = LocalDate.now().toString();
        List<PatientStatementDto.StatementLine> lines = new ArrayList<>();
        List<PatientInvoiceDto> invoices = listInvoices(patientId);
        for (PatientInvoiceDto inv : invoices) {
            PatientStatementDto.StatementLine line = new PatientStatementDto.StatementLine();
            line.date = LocalDate.now().toString();
            line.description = "Invoice #" + inv.id();
            line.amount = inv.totalCharge();
            line.balance = inv.ptBalance();
            lines.add(line);
        }
        dto.lines = lines;
        PatientStatementDto.Summary summary = new PatientStatementDto.Summary();
        summary.totalCharges = invoices.stream().map(PatientInvoiceDto::totalCharge).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.outstandingBalance = invoices.stream().map(PatientInvoiceDto::ptBalance).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.summary = summary;
        dto.yourPortion = new PatientStatementDto.YourPortion();
        dto.yourPortion.balance0_30 = summary.outstandingBalance;
        dto.appointments = new PatientStatementDto.AppointmentSummary();
        dto.notes = List.of();
        return dto;
    }

    public PatientInvoiceDto transferOutstandingToPatient(Long patientId, String invoiceId, Double amount) {
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        if (amount != null && amount > 0) {
            BigDecimal amt = BigDecimal.valueOf(amount);
            BigDecimal insBal = getDecimalExt(invoice, EXT_INS_BALANCE);
            BigDecimal ptBal = getDecimalExt(invoice, EXT_PT_BALANCE);
            if (insBal.compareTo(amt) < 0) amt = insBal;
            setDecimalExt(invoice, EXT_INS_BALANCE, insBal.subtract(amt));
            setDecimalExt(invoice, EXT_PT_BALANCE, ptBal.add(amt));
            fhirClientService.update(invoice, getPracticeId());
        }
        return fromFhirInvoice(invoice);
    }

    public PatientInvoiceDto transferOutstandingToInsurance(Long patientId, String invoiceId, Double amount) {
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        if (amount != null && amount > 0) {
            BigDecimal amt = BigDecimal.valueOf(amount);
            BigDecimal ptBal = getDecimalExt(invoice, EXT_PT_BALANCE);
            BigDecimal insBal = getDecimalExt(invoice, EXT_INS_BALANCE);
            if (ptBal.compareTo(amt) < 0) amt = ptBal;
            setDecimalExt(invoice, EXT_PT_BALANCE, ptBal.subtract(amt));
            setDecimalExt(invoice, EXT_INS_BALANCE, insBal.add(amt));
            fhirClientService.update(invoice, getPracticeId());
        }
        return fromFhirInvoice(invoice);
    }

    public PatientInvoiceDto backdateInvoice(Long patientId, String invoiceId, BackdateRequest req) {
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        if (req != null && req.date() != null) {
            setStringExt(invoice, EXT_BACKDATE, req.date());
            fhirClientService.update(invoice, getPracticeId());
        }
        return fromFhirInvoice(invoice);
    }

    public PatientAccountCreditDto accountAdjustment(Long patientId, AccountAdjustmentRequest req) {
        if (req == null || req.adjustmentType() == null) throw new IllegalArgumentException("Adjustment type is required");
        BigDecimal adjustmentAmount = switch (req.adjustmentType()) {
            case "Flat-rate" -> nz(req.flatRate());
            case "Total Outstanding" -> listInvoices(patientId).stream().map(inv -> nz(inv.ptBalance()).add(nz(inv.insBalance()))).reduce(BigDecimal.ZERO, BigDecimal::add);
            case "Patient Outstanding" -> listInvoices(patientId).stream().map(inv -> nz(inv.ptBalance())).reduce(BigDecimal.ZERO, BigDecimal::add);
            case "Specific" -> nz(req.specificAmount());
            default -> throw new IllegalArgumentException("Invalid adjustment type: " + req.adjustmentType());
        };
        BigDecimal newBalance = getAccountCreditBalance(patientId).add(adjustmentAmount);
        saveAccountCredit(patientId, newBalance);
        return new PatientAccountCreditDto(patientId, newBalance);
    }

    public PatientInvoiceDto adjustInvoice(Long patientId, String invoiceId, InvoiceAdjustmentRequest req) {
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        if (req == null || req.adjustmentType() == null) throw new IllegalArgumentException("Adjustment type is required");
        if (req.percentageDiscount() != null && req.percentageDiscount() > 0) {
            BigDecimal discountFactor = BigDecimal.valueOf(req.percentageDiscount()).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
            for (Invoice.InvoiceLineItemComponent line : invoice.getLineItem()) {
                BigDecimal originalCharge = getLineCharge(line);
                BigDecimal newCharge = originalCharge.subtract(originalCharge.multiply(discountFactor)).max(BigDecimal.ZERO);
                setLineCharge(line, newCharge);
            }
        }
        if (req.adjustmentAmount() != null && req.adjustmentAmount().signum() != 0) addCredit(patientId, req.adjustmentAmount());
        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());
        return fromFhirInvoice(invoice);
    }

    public List<PatientInvoiceDto> listInvoices(Long patientId) {
        Bundle bundle = fhirClientService.getClient(getPracticeId()).search().forResource(Invoice.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId)).returnBundle(Bundle.class).execute();
        return fhirClientService.extractResources(bundle, Invoice.class).stream().map(this::fromFhirInvoice).collect(Collectors.toList());
    }

    public List<PatientInvoiceLineDto> getInvoiceLines(Long patientId, String invoiceId) {
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        return invoice.getLineItem().stream().map(this::fromFhirInvoiceLine).collect(Collectors.toList());
    }

    public PatientInvoiceDto createInvoiceFromProcedure(Long patientId, CreateInvoiceRequest b) {
        if (b == null) throw new IllegalArgumentException("Request body is required");
        if (b.dos() == null || b.dos().isEmpty()) throw new IllegalArgumentException("Date of service is required");
        if (b.procedures() == null || b.procedures().isEmpty()) throw new IllegalArgumentException("At least one procedure is required");
        Invoice invoice = new Invoice();
        invoice.setStatus(Invoice.InvoiceStatus.ISSUED);
        invoice.setSubject(new Reference("Patient/" + patientId));
        for (ProcedureLineRequest proc : b.procedures()) {
            if (proc.code() == null) throw new IllegalArgumentException("Procedure code is required");
            if (proc.rate() == null) throw new IllegalArgumentException("Rate is required for procedure " + proc.code());
            Invoice.InvoiceLineItemComponent line = invoice.addLineItem();
            line.addExtension(new Extension(EXT_LINE_CODE, new StringType(proc.code())));
            line.addExtension(new Extension(EXT_LINE_TREATMENT, new StringType(proc.description() != null ? proc.description() : "")));
            line.addExtension(new Extension(EXT_LINE_PROVIDER, new StringType(b.provider() != null ? b.provider() : "")));
            line.addExtension(new Extension(EXT_LINE_CHARGE, new DecimalType(proc.rate())));
            line.addExtension(new Extension(EXT_LINE_ALLOWED, new DecimalType(proc.rate())));
            line.addExtension(new Extension(EXT_LINE_INS_WO, new DecimalType(BigDecimal.ZERO)));
            line.addExtension(new Extension(EXT_LINE_INS_PORTION, new DecimalType(proc.rate())));
            line.addExtension(new Extension(EXT_LINE_PT_PORTION, new DecimalType(BigDecimal.ZERO)));
        }
        recalcInvoiceTotals(invoice);
        var outcome = fhirClientService.create(invoice, getPracticeId());
        String fhirId = outcome.getId().getIdPart();
        invoice.setId(fhirId);
        createDraftClaim(patientId, fhirId, b.dos());
        return fromFhirInvoice(invoice);
    }

    public void deleteInvoice(Long patientId, String invoiceId) {
        Bundle claimBundle = fhirClientService.getClient(getPracticeId()).search().forResource(Claim.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patientId)).returnBundle(Bundle.class).execute();
        for (Claim claim : fhirClientService.extractResources(claimBundle, Claim.class)) {
            if (invoiceId.equals(getStringExt(claim, EXT_INVOICE_ID))) {
                fhirClientService.delete(Claim.class, claim.getIdElement().getIdPart(), getPracticeId());
            }
        }
        fhirClientService.delete(Invoice.class, invoiceId, getPracticeId());
    }

    public PatientInvoiceDto updateInvoiceFromProcedure(Long patientId, String invoiceId, UpdateInvoiceRequest b) {
        if (b == null) throw new IllegalArgumentException("Request body is required");
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        if (invoice.getLineItem().isEmpty()) throw new IllegalArgumentException("Invoice has no lines to update");
        Invoice.InvoiceLineItemComponent line = invoice.getLineItem().get(0);
        if (b.code() != null) setLineExt(line, EXT_LINE_CODE, b.code());
        if (b.description() != null) setLineExt(line, EXT_LINE_TREATMENT, b.description());
        if (b.provider() != null) setLineExt(line, EXT_LINE_PROVIDER, b.provider());
        if (b.rate() != null) { setLineCharge(line, b.rate()); setLineExt(line, EXT_LINE_ALLOWED, b.rate().toString()); }
        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());
        return fromFhirInvoice(invoice);
    }

    public PatientInvoiceDto updateInvoiceLineAmount(Long patientId, String invoiceId, String lineId, UpdateLineAmountRequest b) {
        if (b == null || b.newCharge() == null) throw new IllegalArgumentException("New charge amount is required");
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        int lineIndex = Integer.parseInt(lineId);
        if (lineIndex < 0 || lineIndex >= invoice.getLineItem().size()) throw new IllegalArgumentException("Invoice line not found");
        Invoice.InvoiceLineItemComponent line = invoice.getLineItem().get(lineIndex);
        setLineCharge(line, b.newCharge());
        setLineExt(line, EXT_LINE_ALLOWED, b.newCharge().toString());
        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());
        return fromFhirInvoice(invoice);
    }

    public PatientInvoiceDto applyInvoicePercentageAdjustment(Long patientId, String invoiceId, PercentageAdjustmentRequest b) {
        if (b == null) throw new IllegalArgumentException("Percentage adjustment request is required");
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        BigDecimal p = BigDecimal.valueOf(b.percent()).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        for (Invoice.InvoiceLineItemComponent line : invoice.getLineItem()) {
            BigDecimal charge = getLineCharge(line);
            setLineCharge(line, charge.subtract(charge.multiply(p)).max(BigDecimal.ZERO));
        }
        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());
        return fromFhirInvoice(invoice);
    }

    public List<PatientClaimDto> listAllClaims() {
        return fhirClientService.extractResources(fhirClientService.search(Claim.class, getPracticeId()), Claim.class).stream().map(this::fromFhirClaim).collect(Collectors.toList());
    }

    public List<PatientClaimDto> listAllClaimsForPatient(Long patientId) {
        Bundle bundle = fhirClientService.getClient(getPracticeId()).search().forResource(Claim.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patientId)).returnBundle(Bundle.class).execute();
        return fhirClientService.extractResources(bundle, Claim.class).stream().map(this::fromFhirClaim).collect(Collectors.toList());
    }

    public PatientClaimDto getActiveClaimForInvoice(Long patientId, String invoiceId) {
        List<PatientClaimDto> claims = listClaimsForInvoice(patientId, invoiceId);
        return claims.isEmpty() ? null : claims.get(0);
    }

    public List<PatientClaimDto> listClaimsForInvoice(Long patientId, String invoiceId) {
        Bundle bundle = fhirClientService.getClient(getPracticeId()).search().forResource(Claim.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patientId)).returnBundle(Bundle.class).execute();
        return fhirClientService.extractResources(bundle, Claim.class).stream()
                .filter(c -> invoiceId.equals(getStringExt(c, EXT_INVOICE_ID))).map(this::fromFhirClaim).collect(Collectors.toList());
    }

    public PatientClaimDto promoteClaim(Long patientId, String invoiceId) {
        Claim claim = findClaimByInvoice(patientId, invoiceId);
        if (claim != null && claim.getStatus() == Claim.ClaimStatus.DRAFT) { claim.setStatus(Claim.ClaimStatus.ACTIVE); fhirClientService.update(claim, getPracticeId()); }
        return claim != null ? fromFhirClaim(claim) : null;
    }

    public PatientClaimDto sendClaimToBatch(Long patientId, String invoiceId) {
        Claim claim = findClaimByInvoice(patientId, invoiceId);
        if (claim != null) { claim.setStatus(Claim.ClaimStatus.ACTIVE); fhirClientService.update(claim, getPracticeId()); }
        return claim != null ? fromFhirClaim(claim) : null;
    }

    public PatientClaimDto submitClaim(Long patientId, String invoiceId) {
        Claim claim = findClaimByInvoice(patientId, invoiceId);
        if (claim != null) { claim.setStatus(Claim.ClaimStatus.ACTIVE); fhirClientService.update(claim, getPracticeId()); }
        return claim != null ? fromFhirClaim(claim) : null;
    }

    public PatientClaimDto closeClaim(Long patientId, String invoiceId) {
        Claim claim = findClaimByInvoice(patientId, invoiceId);
        if (claim != null) { claim.setStatus(Claim.ClaimStatus.ENTEREDINERROR); fhirClientService.update(claim, getPracticeId()); }
        return claim != null ? fromFhirClaim(claim) : null;
    }

    public PatientClaimDto voidAndRecreateClaim(Long patientId, String invoiceId) {
        Claim existing = findClaimByInvoice(patientId, invoiceId);
        if (existing != null) fhirClientService.delete(Claim.class, existing.getIdElement().getIdPart(), getPracticeId());
        createDraftClaim(patientId, invoiceId, LocalDate.now().toString());
        Claim newClaim = findClaimByInvoice(patientId, invoiceId);
        return newClaim != null ? fromFhirClaim(newClaim) : null;
    }

    public PatientClaimDto voidAndRecreateClaimById(String claimId) {
        Claim existing = fhirClientService.read(Claim.class, claimId, getPracticeId());
        Long patientId = extractPatientId(existing);
        String invoiceId = getStringExt(existing, EXT_INVOICE_ID);
        fhirClientService.delete(Claim.class, claimId, getPracticeId());
        createDraftClaim(patientId, invoiceId, LocalDate.now().toString());
        Claim newClaim = findClaimByInvoice(patientId, invoiceId);
        return newClaim != null ? fromFhirClaim(newClaim) : null;
    }

    public PatientClaimDto updateClaim(Long patientId, String invoiceId, PatientClaimCoreUpdate p) {
        if (p == null) throw new IllegalArgumentException("Claim update request is required");
        Claim claim = findClaimByInvoice(patientId, invoiceId);
        if (claim != null) {
            setStringExt(claim, EXT_TREATING_PROVIDER, p.treatingProviderId());
            setStringExt(claim, EXT_BILLING_ENTITY, p.billingEntity());
            setStringExt(claim, EXT_CLAIM_TYPE, p.type());
            setStringExt(claim, EXT_NOTES, p.notes());
            fhirClientService.update(claim, getPracticeId());
        }
        return claim != null ? fromFhirClaim(claim) : null;
    }

    public PatientClaimDto convertClaimType(String claimId, String targetType) {
        Claim claim = fhirClientService.read(Claim.class, claimId, getPracticeId());
        setStringExt(claim, EXT_CLAIM_TYPE, targetType);
        fhirClientService.update(claim, getPracticeId());
        return fromFhirClaim(claim);
    }

    public PatientClaimDto getClaimDtoById(String claimId) {
        return fromFhirClaim(fhirClientService.read(Claim.class, claimId, getPracticeId()));
    }

    public List<ClaimLineDetailDto> getClaimLineDetails(String claimId) {
        Claim claim = fhirClientService.read(Claim.class, claimId, getPracticeId());
        String invoiceId = getStringExt(claim, EXT_INVOICE_ID);
        if (invoiceId == null) return List.of();
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        List<ClaimLineDetailDto> result = new ArrayList<>();
        int idx = 0;
        for (Invoice.InvoiceLineItemComponent line : invoice.getLineItem()) {
            result.add(new ClaimLineDetailDto((long) idx++, LocalDate.now(), getLineExt(line, EXT_LINE_CODE), getLineExt(line, EXT_LINE_TREATMENT), getLineExt(line, EXT_LINE_PROVIDER), getLineCharge(line)));
        }
        return result;
    }

    public PatientInvoiceDto applyInsurancePayment(Long patientId, String invoiceId, PatientInsurancePaymentRequestDto req) {
        if (req == null || req.lines() == null || req.lines().isEmpty()) throw new IllegalArgumentException("Payment request with lines is required");
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        for (PatientInsuranceRemitLineDto r : req.lines()) {
            int lineIdx = r.invoiceLineId().intValue();
            if (lineIdx < 0 || lineIdx >= invoice.getLineItem().size()) continue;
            Invoice.InvoiceLineItemComponent line = invoice.getLineItem().get(lineIdx);
            BigDecimal submitted = nz(r.submitted()), allowed = nz(r.allowed()), insPay = nz(r.insPay());
            setLineCharge(line, submitted);
            setLineExt(line, EXT_LINE_ALLOWED, allowed.toString());
            setLineExt(line, EXT_LINE_INS_WO, submitted.subtract(allowed).max(BigDecimal.ZERO).toString());
            setLineExt(line, EXT_LINE_INS_PORTION, "0");
            setLineExt(line, EXT_LINE_PT_PORTION, allowed.subtract(insPay).max(BigDecimal.ZERO).toString());
        }
        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());
        return fromFhirInvoice(invoice);
    }

    public List<PatientInsuranceRemitLineDto> listInsurancePayments(Long patientId, String invoiceId, String claimId, Long insuranceId) {
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        List<PatientInsuranceRemitLineDto> result = new ArrayList<>();
        int idx = 0;
        for (Invoice.InvoiceLineItemComponent line : invoice.getLineItem()) {
            result.add(new PatientInsuranceRemitLineDto((long) idx, (long) idx++, getLineCharge(line), new BigDecimal(getLineExt(line, EXT_LINE_PT_PORTION)), BigDecimal.ZERO, new BigDecimal(getLineExt(line, EXT_LINE_ALLOWED)), new BigDecimal(getLineExt(line, EXT_LINE_INS_WO)), BigDecimal.ZERO, null, null, null));
        }
        return result;
    }

    public PatientInvoiceDto editInsuranceRemitLine(Long patientId, String invoiceId, String remitId, PatientInsuranceRemitLineDto dto) {
        if (dto == null) throw new IllegalArgumentException("Request body is required");
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        int lineIdx = Integer.parseInt(remitId);
        if (lineIdx >= 0 && lineIdx < invoice.getLineItem().size()) {
            Invoice.InvoiceLineItemComponent line = invoice.getLineItem().get(lineIdx);
            if (dto.submitted() != null) setLineCharge(line, dto.submitted());
            if (dto.allowed() != null) setLineExt(line, EXT_LINE_ALLOWED, dto.allowed().toString());
            if (dto.insWriteOff() != null) setLineExt(line, EXT_LINE_INS_WO, dto.insWriteOff().toString());
        }
        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());
        return fromFhirInvoice(invoice);
    }

    public PatientInvoiceDto voidInsurancePayment(Long patientId, String invoiceId, String remitId, VoidReason reason) {
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());
        return fromFhirInvoice(invoice);
    }

    public PatientInvoiceDto refundInsurancePayment(Long patientId, String invoiceId, String remitId, RefundRequest req) {
        BigDecimal amount = Optional.ofNullable(req).map(RefundRequest::amount).orElseThrow(() -> new IllegalArgumentException("Refund amount required"));
        if (amount.signum() <= 0) throw new IllegalArgumentException("Refund amount must be > 0");
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        if (!invoice.getLineItem().isEmpty()) {
            Invoice.InvoiceLineItemComponent line = invoice.getLineItem().get(0);
            BigDecimal insPortion = new BigDecimal(getLineExt(line, EXT_LINE_INS_PORTION));
            setLineExt(line, EXT_LINE_INS_PORTION, insPortion.add(amount).toString());
        }
        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());
        return fromFhirInvoice(invoice);
    }

    public PatientInvoiceDto transferInsuranceCreditToPatient(Long patientId, String invoiceId, String remitId, TransferCreditRequest req) {
        BigDecimal amount = Optional.ofNullable(req).map(TransferCreditRequest::amount).orElseThrow(() -> new IllegalArgumentException("Transfer amount required"));
        if (amount.signum() <= 0) throw new IllegalArgumentException("Transfer amount must be > 0");
        addCredit(patientId, amount);
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());
        return fromFhirInvoice(invoice);
    }

    public InsurancePaymentDetailDto getInsurancePaymentDetails(Long patientId, String invoiceId, String remitId) {
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        List<InsurancePaymentDetailDto.InsurancePaymentLineDetailDto> lineDetails = invoice.getLineItem().stream()
                .map(line -> InsurancePaymentDetailDto.InsurancePaymentLineDetailDto.builder().lineId(0L).description(getLineExt(line, EXT_LINE_TREATMENT)).providerName(getLineExt(line, EXT_LINE_PROVIDER)).amount(getLineCharge(line)).patient(new BigDecimal(getLineExt(line, EXT_LINE_PT_PORTION))).insurance(new BigDecimal(getLineExt(line, EXT_LINE_INS_PORTION))).previousBalance(BigDecimal.ZERO).payment(BigDecimal.ZERO).build()).toList();
        return InsurancePaymentDetailDto.builder().remitId(0L).invoiceId(0L).invoiceNumber(invoiceId).paymentDate(LocalDate.now()).chequeNumber("").bankBranchNumber("").insWriteoff(getDecimalExt(invoice, EXT_INS_WO)).patientAmount(getDecimalExt(invoice, EXT_PT_BALANCE)).insuranceAmount(getDecimalExt(invoice, EXT_INS_BALANCE)).previousTotalBalance(getDecimalExt(invoice, EXT_TOTAL_CHARGE)).paymentAmount(BigDecimal.ZERO).appliedWO(getDecimalExt(invoice, EXT_APPLIED_WO)).ptPaid(BigDecimal.ZERO).insPaid(BigDecimal.ZERO).lineDetails(lineDetails).build();
    }

    public PatientPaymentDetailDto getPatientPaymentDetails(Long patientId, String invoiceId, String paymentId) {
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        List<PatientPaymentDetailDto.PatientPaymentLineDetailDto> lineDetails = invoice.getLineItem().stream()
                .map(line -> PatientPaymentDetailDto.PatientPaymentLineDetailDto.builder().lineId(0L).description(getLineExt(line, EXT_LINE_TREATMENT)).providerName(getLineExt(line, EXT_LINE_PROVIDER)).amount(getLineCharge(line)).patient(new BigDecimal(getLineExt(line, EXT_LINE_PT_PORTION))).insurance(new BigDecimal(getLineExt(line, EXT_LINE_INS_PORTION))).previousBalance(BigDecimal.ZERO).payment(BigDecimal.ZERO).build()).toList();
        return PatientPaymentDetailDto.builder().paymentId(0L).invoiceId(0L).invoiceNumber(invoiceId).paymentDate(LocalDateTime.now()).paymentMethod("").chequeNumber("").bankBranchNumber("").patientAmount(getDecimalExt(invoice, EXT_PT_BALANCE)).insuranceAmount(getDecimalExt(invoice, EXT_INS_BALANCE)).previousTotalBalance(getDecimalExt(invoice, EXT_TOTAL_CHARGE)).paymentAmount(BigDecimal.ZERO).ptPaid(BigDecimal.ZERO).insPaid(BigDecimal.ZERO).lineDetails(lineDetails).build();
    }

    public PatientInvoiceDto applyPatientPayment(Long patientId, String invoiceId, PatientPatientPaymentRequestDto req) {
        if (req == null) throw new IllegalArgumentException("Payment request is required");
        if (req.paymentMethod() == null || req.paymentMethod().isEmpty()) throw new IllegalArgumentException("Payment method is required");
        if (req.allocations() == null || req.allocations().isEmpty()) throw new IllegalArgumentException("Payment allocations are required");
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        for (var allocReq : req.allocations()) {
            int lineIdx = allocReq.invoiceLineId().intValue();
            if (lineIdx < 0 || lineIdx >= invoice.getLineItem().size()) continue;
            Invoice.InvoiceLineItemComponent line = invoice.getLineItem().get(lineIdx);
            BigDecimal currentPtPortion = new BigDecimal(getLineExt(line, EXT_LINE_PT_PORTION));
            setLineExt(line, EXT_LINE_PT_PORTION, currentPtPortion.subtract(nz(allocReq.amount())).max(BigDecimal.ZERO).toString());
        }
        recalcInvoiceTotals(invoice);
        if (getDecimalExt(invoice, EXT_PT_BALANCE).add(getDecimalExt(invoice, EXT_INS_BALANCE)).compareTo(BigDecimal.ZERO) <= 0) invoice.setStatus(Invoice.InvoiceStatus.BALANCED);
        fhirClientService.update(invoice, getPracticeId());
        return fromFhirInvoice(invoice);
    }

    public List<PatientPatientPaymentAllocationDto> getAllPatientPayments(Long patientId) { return List.of(); }
    public List<PatientPatientPaymentAllocationDto> getPatientPaymentsByInvoice(Long patientId, String invoiceId) { return List.of(); }

    public PatientInvoiceDto editPatientPayment(Long patientId, String invoiceId, String paymentId, PatientPaymentDto dto) {
        if (dto == null) throw new IllegalArgumentException("Request body is required");
        return fromFhirInvoice(fhirClientService.read(Invoice.class, invoiceId, getPracticeId()));
    }

    public PatientInvoiceDto voidPatientPayment(Long patientId, String invoiceId, String paymentId, VoidReason reason) {
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());
        return fromFhirInvoice(invoice);
    }

    public PatientInvoiceDto refundPatientPayment(Long patientId, String invoiceId, String paymentId, RefundRequest req) {
        BigDecimal amount = Optional.ofNullable(req).map(RefundRequest::amount).orElseThrow(() -> new IllegalArgumentException("Refund amount required"));
        if (amount.signum() <= 0) throw new IllegalArgumentException("Refund amount must be > 0");
        addCredit(patientId, amount);
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());
        return fromFhirInvoice(invoice);
    }

    public PatientAccountCreditDto[] transferPatientCreditToPatient(Long fromPatientId, Long toPatientId, BigDecimal amount) {
        if (fromPatientId.equals(toPatientId)) throw new IllegalArgumentException("Source and destination patients must differ");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive");
        BigDecimal fromBalance = getAccountCreditBalance(fromPatientId);
        if (fromBalance.compareTo(amount) < 0) throw new IllegalArgumentException("Insufficient credit in source account");
        BigDecimal toBalance = getAccountCreditBalance(toPatientId);
        saveAccountCredit(fromPatientId, fromBalance.subtract(amount));
        saveAccountCredit(toPatientId, toBalance.add(amount));
        return new PatientAccountCreditDto[] { new PatientAccountCreditDto(fromPatientId, fromBalance.subtract(amount)), new PatientAccountCreditDto(toPatientId, toBalance.add(amount)) };
    }

    public PatientAccountCreditDto getAccountCredit(Long patientId) { return new PatientAccountCreditDto(patientId, getAccountCreditBalance(patientId)); }

    public PatientAccountCreditDto applyAccountCredit(Long patientId, ApplyCreditRequest b) {
        BigDecimal amount = (b == null) ? BigDecimal.ZERO : nz(b.amount());
        BigDecimal balance = getAccountCreditBalance(patientId);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return new PatientAccountCreditDto(patientId, balance);
        if (balance.compareTo(amount) < 0) throw new IllegalArgumentException("Insufficient credit");
        BigDecimal newBalance = balance.subtract(amount);
        saveAccountCredit(patientId, newBalance);
        return new PatientAccountCreditDto(patientId, newBalance);
    }

    public PatientDepositDto addPatientDeposit(Long patientId, PatientDepositRequest request) {
        if (request == null) throw new IllegalArgumentException("Deposit request is required");
        if (request.amount() == null) throw new IllegalArgumentException("Deposit amount is required");
        if (request.amount().signum() <= 0) throw new IllegalArgumentException("Deposit amount must be positive");
        saveAccountCredit(patientId, getAccountCreditBalance(patientId).add(request.amount()));
        return new PatientDepositDto(0L, patientId, request.amount(), request.depositDate() != null ? request.depositDate() : LocalDate.now(), request.description(), request.paymentMethod());
    }

    public List<PatientDepositDto> getPatientDeposits(Long patientId) { return List.of(); }

    public InsuranceDepositDto addInsuranceDeposit(Long patientId, InsuranceDepositDto request) {
        return new InsuranceDepositDto(0L, patientId, request.policyId(), request.depositAmount() != null ? request.depositAmount() : BigDecimal.ZERO, request.depositDate() != null ? request.depositDate() : LocalDate.now(), request.paymentMethod(), request.providerId(), request.description());
    }

    public InsuranceDepositDto getInsuranceDeposit(Long patientId, String depositId) { return new InsuranceDepositDto(0L, patientId, null, BigDecimal.ZERO, LocalDate.now(), null, null, null); }
    public List<InsuranceDepositDto> getInsuranceDeposits(Long patientId) { return List.of(); }
    public InsuranceDepositDto updateInsuranceDeposit(Long patientId, String depositId, InsuranceDepositDto request) { return request; }
    public void deleteInsuranceDeposit(Long patientId, String depositId) { }

    public CreditAdjustmentDetailDto getCreditAdjustment(Long patientId, String invoiceId) {
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        List<CreditAdjustmentDetailDto.LineDetail> lines = invoice.getLineItem().stream()
                .map(line -> new CreditAdjustmentDetailDto.LineDetail(getLineExt(line, EXT_LINE_CODE), getLineExt(line, EXT_LINE_TREATMENT), getLineExt(line, EXT_LINE_PROVIDER), new BigDecimal(getLineExt(line, EXT_LINE_INS_WO)), new BigDecimal(getLineExt(line, EXT_LINE_PT_PORTION)), new BigDecimal(getLineExt(line, EXT_LINE_INS_PORTION)), getLineCharge(line), new BigDecimal(getLineExt(line, EXT_LINE_INS_WO)))).toList();
        return new CreditAdjustmentDetailDto(0L, LocalDate.now(), "Write Off", BigDecimal.ZERO, getDecimalExt(invoice, EXT_INS_WO), getDecimalExt(invoice, EXT_PT_BALANCE), getDecimalExt(invoice, EXT_INS_BALANCE), getDecimalExt(invoice, EXT_TOTAL_CHARGE), BigDecimal.ZERO, lines);
    }

    public TransferOfCreditDetailDto getTransferOfCredit(Long patientId, String invoiceId) {
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        List<TransferOfCreditDetailDto.LineDetail> lines = invoice.getLineItem().stream()
                .map(line -> new TransferOfCreditDetailDto.LineDetail(getLineExt(line, EXT_LINE_CODE), getLineExt(line, EXT_LINE_TREATMENT), new BigDecimal(getLineExt(line, EXT_LINE_INS_WO)), new BigDecimal(getLineExt(line, EXT_LINE_PT_PORTION)), new BigDecimal(getLineExt(line, EXT_LINE_INS_PORTION)), getLineCharge(line))).toList();
        return new TransferOfCreditDetailDto(0L, LocalDate.now(), "Transfer of credits", getDecimalExt(invoice, EXT_TOTAL_CHARGE), lines);
    }

    public void uploadClaimAttachment(Long patientId, String claimId, MultipartFile file) throws Exception {
        Claim claim = fhirClientService.read(Claim.class, claimId, getPracticeId());
        setIntExt(claim, EXT_ATTACHMENTS, getIntExt(claim, EXT_ATTACHMENTS) + 1);
        fhirClientService.update(claim, getPracticeId());
    }

    public byte[] getClaimAttachment(Long patientId, String claimId) { return new byte[0]; }

    public void uploadClaimEob(Long patientId, String claimId, MultipartFile file) throws Exception {
        Claim claim = fhirClientService.read(Claim.class, claimId, getPracticeId());
        setBooleanExt(claim, EXT_EOB_ATTACHED, true);
        fhirClientService.update(claim, getPracticeId());
    }

    public byte[] getClaimEob(Long patientId, String claimId) { return new byte[0]; }

    public List<PatientBillingNoteDto> listInvoiceNotes(Long patientId, String invoiceId) { return List.of(); }
    public PatientBillingNoteDto createInvoiceNote(Long patientId, String invoiceId, PatientBillingNoteDto dto) { return dto; }
    public PatientBillingNoteDto updateInvoiceNote(Long patientId, String invoiceId, String noteId, PatientBillingNoteDto dto) { return dto; }
    public void deleteInvoiceNote(Long patientId, String invoiceId, String noteId) { }

    public PatientDepositDto getPatientDeposit(Long patientId, String depositId) { return new PatientDepositDto(0L, patientId, BigDecimal.ZERO, LocalDate.now(), null, null); }
    public PatientDepositDto updatePatientDeposit(Long patientId, String depositId, PatientDepositRequest req) { return new PatientDepositDto(0L, patientId, req.amount(), req.depositDate(), req.description(), req.paymentMethod()); }
    public void deletePatientDeposit(Long patientId, String depositId) { }

    public PatientAccountCreditDto addCourtesyCredit(Long patientId, CourtesyCreditRequest req) {
        BigDecimal amount = req != null && req.amount() != null ? req.amount() : BigDecimal.ZERO;
        saveAccountCredit(patientId, getAccountCreditBalance(patientId).add(amount));
        return new PatientAccountCreditDto(patientId, getAccountCreditBalance(patientId));
    }

    public InvoiceCourtesyCreditDto applyCourtesyCreditToInvoice(Long patientId, String invoiceId, CourtesyCreditRequest req) {
        return new InvoiceCourtesyCreditDto(0L, patientId, 0L, req != null ? req.adjustmentType() : "Courtesy", req != null ? req.amount() : BigDecimal.ZERO, req != null ? req.description() : null, true, null, null, null, null);
    }

    public List<InvoiceCourtesyCreditDto> getInvoiceWithCourtesyCredit(Long patientId, String invoiceId) { return List.of(); }
    public InvoiceCourtesyCreditDto updateInvoiceCourtesyCredit(Long patientId, String invoiceId, CourtesyCreditRequest req) { return new InvoiceCourtesyCreditDto(0L, patientId, 0L, req != null ? req.adjustmentType() : "Courtesy", req != null ? req.amount() : BigDecimal.ZERO, req != null ? req.description() : null, true, null, null, null, null); }
    public InvoiceCourtesyCreditDto removeInvoiceCourtesyCredit(Long patientId, String invoiceId) { return new InvoiceCourtesyCreditDto(0L, patientId, 0L, "Courtesy", BigDecimal.ZERO, null, false, null, null, null, null); }

    public void lockClaim(Long patientId, String claimId) { }
    public Claim getClaimOrThrow(Long patientId, String claimId) { return fhirClientService.read(Claim.class, claimId, getPracticeId()); }
    public PatientClaimDto toClaimDto(Claim claim) { return fromFhirClaim(claim); }

    public void changeClaimStatus(Long patientId, String claimId, ClaimStatusUpdateDto dto) {
        Claim claim = fhirClientService.read(Claim.class, claimId, getPracticeId());
        if (dto != null && dto.getStatus() != null) {
            claim.setStatus(switch (dto.getStatus()) {
                case "DRAFT" -> Claim.ClaimStatus.DRAFT;
                case "IN_PROCESS", "ACTIVE" -> Claim.ClaimStatus.ACTIVE;
                case "VOID", "CANCELLED" -> Claim.ClaimStatus.ENTEREDINERROR;
                default -> Claim.ClaimStatus.DRAFT;
            });
            fhirClientService.update(claim, getPracticeId());
        }
    }

    public void submitClaimAttachment(Long patientId, String claimId, MultipartFile file) throws Exception { uploadClaimAttachment(patientId, claimId, file); }

    public PatientInvoicePrintDto getPrintableInvoice(Long patientId, String invoiceId) {
        Invoice invoice = fhirClientService.read(Invoice.class, invoiceId, getPracticeId());
        PatientInvoicePrintDto dto = new PatientInvoicePrintDto();
        dto.invoiceId = (long) Math.abs(invoiceId.hashCode());
        dto.patientId = patientId;
        dto.patientName = "[Patient Name]";
        dto.transactions = List.of();
        dto.financialSummary = new PatientInvoicePrintDto.FinancialSummary();
        dto.financialSummary.totalCharges = getDecimalExt(invoice, EXT_TOTAL_CHARGE);
        dto.financialSummary.outstandingBalance = getDecimalExt(invoice, EXT_PT_BALANCE).add(getDecimalExt(invoice, EXT_INS_BALANCE));
        return dto;
    }

    public EhrClaimFormDataDto getEhrClaimFormData(Long patientId, String claimId) {
        Claim claim = fhirClientService.read(Claim.class, claimId, getPracticeId());
        return EhrClaimFormDataDto.builder()
                .claimId((long) Math.abs(claimId.hashCode()))
                .claimNumber(claimId)
                .claimStatus(claim.getStatus() != null ? claim.getStatus().toCode() : "draft")
                .claimDate(claim.hasCreated() ? LocalDate.ofInstant(claim.getCreated().toInstant(), java.time.ZoneId.systemDefault()) : LocalDate.now())
                .insuranceInfo(EhrClaimFormDataDto.InsuranceInfo.builder().planName(getStringExt(claim, EXT_PLAN_NAME)).policyNumber(getStringExt(claim, EXT_POLICY_NUMBER)).build())
                .providerInfo(EhrClaimFormDataDto.ProviderInfo.builder().providerName(getStringExt(claim, EXT_TREATING_PROVIDER)).build())
                .patientInfo(EhrClaimFormDataDto.PatientInfo.builder().patientId(patientId).fullName("[Patient Name]").build())
                .serviceRecords(List.of())
                .build();
    }

    public String getInsuranceEmailForClaim(String claimId) { return ""; }
    public boolean sendClaimDetailsToInsuranceEmail(PatientClaimDto claim, String contact) { log.info("Sending claim to: {}", contact); return true; }

    public List<PatientInsuranceRemitLineDto> listInsurancePayments(Long patientId, Long invoiceId, Long claimId, Long insuranceId) {
        if (invoiceId == null) return List.of();
        return listInsurancePayments(patientId, invoiceId.toString(), claimId != null ? claimId.toString() : null, insuranceId);
    }

    private void createDraftClaim(Long patientId, String invoiceId, String dos) {
        Claim claim = new Claim();
        claim.setStatus(Claim.ClaimStatus.DRAFT);
        claim.setUse(Claim.Use.CLAIM);
        claim.setPatient(new Reference("Patient/" + patientId));
        claim.setCreated(new Date());
        claim.addExtension(new Extension(EXT_INVOICE_ID, new StringType(invoiceId)));
        claim.addExtension(new Extension(EXT_CLAIM_TYPE, new StringType("Electronic")));
        fhirClientService.create(claim, getPracticeId());
    }

    private Claim findClaimByInvoice(Long patientId, String invoiceId) {
        Bundle bundle = fhirClientService.getClient(getPracticeId()).search().forResource(Claim.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patientId)).returnBundle(Bundle.class).execute();
        return fhirClientService.extractResources(bundle, Claim.class).stream().filter(c -> invoiceId.equals(getStringExt(c, EXT_INVOICE_ID))).findFirst().orElse(null);
    }

    private void recalcInvoiceTotals(Invoice invoice) {
        BigDecimal totalCharge = BigDecimal.ZERO, totalPtBalance = BigDecimal.ZERO, totalInsBalance = BigDecimal.ZERO, totalInsWO = BigDecimal.ZERO;
        for (Invoice.InvoiceLineItemComponent line : invoice.getLineItem()) {
            totalCharge = totalCharge.add(getLineCharge(line));
            totalPtBalance = totalPtBalance.add(new BigDecimal(getLineExt(line, EXT_LINE_PT_PORTION)));
            totalInsBalance = totalInsBalance.add(new BigDecimal(getLineExt(line, EXT_LINE_INS_PORTION)));
            totalInsWO = totalInsWO.add(new BigDecimal(getLineExt(line, EXT_LINE_INS_WO)));
        }
        setDecimalExt(invoice, EXT_TOTAL_CHARGE, totalCharge);
        setDecimalExt(invoice, EXT_PT_BALANCE, totalPtBalance);
        setDecimalExt(invoice, EXT_INS_BALANCE, totalInsBalance);
        setDecimalExt(invoice, EXT_INS_WO, totalInsWO);
    }

    private void addCredit(Long patientId, BigDecimal amount) { saveAccountCredit(patientId, getAccountCreditBalance(patientId).add(amount)); }

    private BigDecimal getAccountCreditBalance(Long patientId) {
        try {
            Bundle bundle = fhirClientService.getClient(getPracticeId()).search().forResource(Account.class)
                    .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId)).returnBundle(Bundle.class).execute();
            List<Account> accounts = fhirClientService.extractResources(bundle, Account.class);
            if (!accounts.isEmpty()) return getDecimalExt(accounts.get(0), EXT_CREDIT_BALANCE);
        } catch (Exception e) { log.debug("No account found for patient {}", patientId); }
        return BigDecimal.ZERO;
    }

    private void saveAccountCredit(Long patientId, BigDecimal balance) {
        try {
            Bundle bundle = fhirClientService.getClient(getPracticeId()).search().forResource(Account.class)
                    .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId)).returnBundle(Bundle.class).execute();
            List<Account> accounts = fhirClientService.extractResources(bundle, Account.class);
            Account account = accounts.isEmpty() ? new Account() : accounts.get(0);
            if (accounts.isEmpty()) { account.setStatus(Account.AccountStatus.ACTIVE); account.addSubject(new Reference("Patient/" + patientId)); }
            setDecimalExt(account, EXT_CREDIT_BALANCE, balance);
            if (account.hasId()) fhirClientService.update(account, getPracticeId()); else fhirClientService.create(account, getPracticeId());
        } catch (Exception e) { log.error("Failed to save account credit for patient {}", patientId, e); }
    }

    private Long extractPatientId(Claim claim) {
        if (claim.hasPatient() && claim.getPatient().hasReference()) {
            String ref = claim.getPatient().getReference();
            if (ref.startsWith("Patient/")) { try { return Long.parseLong(ref.substring("Patient/".length())); } catch (NumberFormatException ignored) {} }
        }
        return 0L;
    }

    private PatientInvoiceDto fromFhirInvoice(Invoice invoice) {
        Long id = 0L;
        try { id = (long) Math.abs(invoice.getIdElement().getIdPart().hashCode()); } catch (Exception ignored) {}
        Long patientId = 0L;
        if (invoice.hasSubject() && invoice.getSubject().hasReference()) {
            String ref = invoice.getSubject().getReference();
            if (ref.startsWith("Patient/")) { try { patientId = Long.parseLong(ref.substring("Patient/".length())); } catch (NumberFormatException ignored) {} }
        }
        PatientInvoice.Status status = invoice.getStatus() == Invoice.InvoiceStatus.BALANCED ? PatientInvoice.Status.PAID : invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED ? PatientInvoice.Status.VOID : PatientInvoice.Status.OPEN;
        List<PatientInvoiceLineDto> lines = invoice.getLineItem().stream().map(this::fromFhirInvoiceLine).collect(Collectors.toList());
        return new PatientInvoiceDto(id, patientId, status, getDecimalExt(invoice, EXT_INS_WO), getDecimalExt(invoice, EXT_APPLIED_WO), getDecimalExt(invoice, EXT_PT_BALANCE), getDecimalExt(invoice, EXT_INS_BALANCE), getDecimalExt(invoice, EXT_TOTAL_CHARGE), lines);
    }

    private PatientInvoiceLineDto fromFhirInvoiceLine(Invoice.InvoiceLineItemComponent line) {
        return new PatientInvoiceLineDto(0L, LocalDate.now(), getLineExt(line, EXT_LINE_CODE), getLineExt(line, EXT_LINE_TREATMENT), getLineExt(line, EXT_LINE_PROVIDER), getLineCharge(line), new BigDecimal(getLineExt(line, EXT_LINE_ALLOWED)), new BigDecimal(getLineExt(line, EXT_LINE_INS_WO)), new BigDecimal(getLineExt(line, EXT_LINE_INS_PORTION)), new BigDecimal(getLineExt(line, EXT_LINE_PT_PORTION)));
    }

    private PatientClaimDto fromFhirClaim(Claim claim) {
        Long id = 0L;
        try { id = (long) Math.abs(claim.getIdElement().getIdPart().hashCode()); } catch (Exception ignored) {}
        Long invoiceId = 0L;
        String invIdStr = getStringExt(claim, EXT_INVOICE_ID);
        if (invIdStr != null) { try { invoiceId = (long) Math.abs(invIdStr.hashCode()); } catch (Exception ignored) {} }
        Long patientId = extractPatientId(claim);
        PatientClaim.Status status = switch (claim.getStatus()) {
            case DRAFT -> PatientClaim.Status.DRAFT;
            case ACTIVE -> PatientClaim.Status.IN_PROCESS;
            case ENTEREDINERROR -> PatientClaim.Status.VOID;
            default -> PatientClaim.Status.DRAFT;
        };
        return new PatientClaimDto(id, invoiceId, patientId, getStringExt(claim, EXT_PAYER_NAME), getStringExt(claim, EXT_TREATING_PROVIDER), getStringExt(claim, EXT_BILLING_ENTITY), getStringExt(claim, EXT_CLAIM_TYPE), getStringExt(claim, EXT_NOTES), status, getIntExt(claim, EXT_ATTACHMENTS), getBooleanExt(claim, EXT_EOB_ATTACHED), claim.hasCreated() ? LocalDate.ofInstant(claim.getCreated().toInstant(), java.time.ZoneId.systemDefault()) : LocalDate.now(), getIntExt(claim, EXT_ATTACHMENTS) > 0, getBooleanExt(claim, EXT_EOB_ATTACHED), getStringExt(claim, EXT_PATIENT_NAME), getStringExt(claim, EXT_PLAN_NAME), "", getStringExt(claim, EXT_POLICY_NUMBER));
    }

    private BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private BigDecimal getDecimalExt(DomainResource r, String url) {
        Extension ext = r.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof DecimalType) return ((DecimalType) ext.getValue()).getValue();
        return BigDecimal.ZERO;
    }

    private void setDecimalExt(DomainResource r, String url, BigDecimal value) {
        r.getExtension().removeIf(e -> url.equals(e.getUrl()));
        r.addExtension(new Extension(url, new DecimalType(value != null ? value : BigDecimal.ZERO)));
    }

    private String getStringExt(DomainResource r, String url) {
        Extension ext = r.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) return ((StringType) ext.getValue()).getValue();
        return "";
    }

    private void setStringExt(DomainResource r, String url, String value) {
        r.getExtension().removeIf(e -> url.equals(e.getUrl()));
        if (value != null) r.addExtension(new Extension(url, new StringType(value)));
    }

    private int getIntExt(DomainResource r, String url) {
        Extension ext = r.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof IntegerType) return ((IntegerType) ext.getValue()).getValue();
        return 0;
    }

    private void setIntExt(DomainResource r, String url, int value) {
        r.getExtension().removeIf(e -> url.equals(e.getUrl()));
        r.addExtension(new Extension(url, new IntegerType(value)));
    }

    private boolean getBooleanExt(DomainResource r, String url) {
        Extension ext = r.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof BooleanType) return ((BooleanType) ext.getValue()).booleanValue();
        return false;
    }

    private void setBooleanExt(DomainResource r, String url, boolean value) {
        r.getExtension().removeIf(e -> url.equals(e.getUrl()));
        r.addExtension(new Extension(url, new BooleanType(value)));
    }

    private BigDecimal getLineCharge(Invoice.InvoiceLineItemComponent line) {
        Extension ext = line.getExtensionByUrl(EXT_LINE_CHARGE);
        if (ext != null && ext.getValue() instanceof DecimalType) return ((DecimalType) ext.getValue()).getValue();
        return BigDecimal.ZERO;
    }

    private void setLineCharge(Invoice.InvoiceLineItemComponent line, BigDecimal value) {
        line.getExtension().removeIf(e -> EXT_LINE_CHARGE.equals(e.getUrl()));
        line.addExtension(new Extension(EXT_LINE_CHARGE, new DecimalType(value != null ? value : BigDecimal.ZERO)));
    }

    private String getLineExt(Invoice.InvoiceLineItemComponent line, String url) {
        Extension ext = line.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) return ((StringType) ext.getValue()).getValue();
        if (ext != null && ext.getValue() instanceof DecimalType) return ((DecimalType) ext.getValue()).getValue().toString();
        return "0";
    }

    private void setLineExt(Invoice.InvoiceLineItemComponent line, String url, String value) {
        line.getExtension().removeIf(e -> url.equals(e.getUrl()));
        if (value != null) line.addExtension(new Extension(url, new StringType(value)));
    }
}
