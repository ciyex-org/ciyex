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
import java.util.*;
import java.util.stream.Collectors;

/**
 * PatientInsurancePaymentService: FHIR-backed service for managing insurance payments/remit records.
 * 
 * Stores insurance payment records (EOB lines) as FHIR Observation resources with extensions.
 * Coordinates with PatientInvoiceService for invoice line details.
 * 
 * Extension URLs for remit records:
 * - patient-reference: Patient ID
 * - invoice-reference: Invoice ID
 * - invoice-line-id: Line ID being remitted
 * - remit-id: Unique remit record ID
 * - remit-submitted: Submitted amount from EOB
 * - remit-balance: Patient responsibility (balance)
 * - remit-deductible: Deductible amount from EOB
 * - remit-allowed: Allowed amount from EOB
 * - remit-ins-writeoff: Insurance write-off (submitted - allowed)
 * - remit-ins-pay: Insurance payment amount
 * - update-allowed: Flag to update allowed amount
 * - update-flat-portion: Flag for flat portion update
 * - apply-writeoff: Flag to apply write-off
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientInsurancePaymentService {
    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final PatientInvoiceService invoiceService;

    // FHIR Extension URLs
    private static final String EXT_PATIENT = "http://ciyex.com/fhir/StructureDefinition/patient-reference";
    private static final String EXT_INVOICE = "http://ciyex.com/fhir/StructureDefinition/invoice-reference";
    private static final String EXT_LINE_ID = "http://ciyex.com/fhir/StructureDefinition/line-id";
    private static final String EXT_REMIT_ID = "http://ciyex.com/fhir/StructureDefinition/remit-id";
    private static final String EXT_REMIT_SUBMITTED = "http://ciyex.com/fhir/StructureDefinition/remit-submitted";
    private static final String EXT_REMIT_BALANCE = "http://ciyex.com/fhir/StructureDefinition/remit-balance";
    private static final String EXT_REMIT_DEDUCTIBLE = "http://ciyex.com/fhir/StructureDefinition/remit-deductible";
    private static final String EXT_REMIT_ALLOWED = "http://ciyex.com/fhir/StructureDefinition/remit-allowed";
    private static final String EXT_REMIT_INS_WRITEOFF = "http://ciyex.com/fhir/StructureDefinition/remit-ins-writeoff";
    private static final String EXT_REMIT_INS_PAY = "http://ciyex.com/fhir/StructureDefinition/remit-ins-pay";
    private static final String EXT_UPDATE_ALLOWED = "http://ciyex.com/fhir/StructureDefinition/update-allowed";
    private static final String EXT_UPDATE_FLAT_PORTION = "http://ciyex.com/fhir/StructureDefinition/update-flat-portion";
    private static final String EXT_APPLY_WRITEOFF = "http://ciyex.com/fhir/StructureDefinition/apply-writeoff";
    private static final String EXT_ATTACHMENT_BINARY = "http://ciyex.com/fhir/StructureDefinition/attachment-binary";
    private static final String EXT_EOB_BINARY = "http://ciyex.com/fhir/StructureDefinition/eob-binary";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    /* ====== Request DTOs ====== */
    public record ProcedureLineRequest(String code, String description, BigDecimal rate) {}
    public record CreateInvoiceRequest(String provider, String dos, List<ProcedureLineRequest> procedures) {}
    public record UpdateInvoiceRequest(String code, String description, String provider, String dos, BigDecimal rate) {}
    public record UpdateLineAmountRequest(BigDecimal newCharge) {}
    public record PercentageAdjustmentRequest(int percent) {}
    public record ApplyCreditRequest(BigDecimal amount) {}
    public record PatientClaimCoreUpdate(
            String treatingProviderId,
            String billingEntity,
            String type,
            String notes,
            String attachmentIndicator,
            String attachmentType,
            String attachmentTransmissionCode,
            String claimSubmissionReasonCode
    ) {}
    public record VoidReason(String reason) {}
    public record RefundRequest(BigDecimal amount, String reason) {}
    public record TransferCreditRequest(BigDecimal amount, String note) {}
    public record BackdateRequest(String date) {}
    public record AccountAdjustmentRequest(
            String adjustmentType, BigDecimal flatRate, BigDecimal specificAmount, 
            String description, Boolean includeCourtesyCredit) {}

    /* ================ Insurance Payment (FHIR-backed) ================ */

    /**
     * List insurance payments with optional filters.
     */
    public List<PatientInsuranceRemitLineDto> listInsurancePayments(Long patientId, Long invoiceId, Long claimId, Long insuranceId) {
        if (patientId != null) validatePatientExists(patientId);
        if (invoiceId != null) validateInvoiceExists(invoiceId);
        if (claimId != null) validateClaimExists(claimId);
        log.debug("Listing insurance payments for patient {} invoice {}", patientId, invoiceId);
        
        List<Observation> allObs = new ArrayList<>();
        Bundle bundle = fhirClientService.search(Observation.class, getPracticeId());
        
        // Handle pagination
        while (bundle != null) {
            List<Observation> pageObs = fhirClientService.extractResources(bundle, Observation.class);
            allObs.addAll(pageObs);
            
            String nextLink = bundle.getLink(Bundle.LINK_NEXT) != null 
                ? bundle.getLink(Bundle.LINK_NEXT).getUrl() 
                : null;
            
            if (nextLink != null) {
                bundle = fhirClientService.loadPage(nextLink, getPracticeId());
            } else {
                break;
            }
        }
        
        log.info("Total observations loaded: {}, filtering for remit records", allObs.size());
        
        return allObs.stream()
                .filter(this::isRemitObservation)
                .filter(obs -> patientId == null || patientId.equals(getPatientIdFromObs(obs)))
                .filter(obs -> invoiceId == null || invoiceId.equals(getInvoiceIdFromObs(obs)))
                .map(this::toRemitDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * List insurance payments with line details included.
     */
    public List<PatientInsuranceRemitWithLinesDto> listInsurancePaymentsWithLines(Long patientId, Long invoiceId, Long claimId, Long insuranceId) {
        if (patientId != null) validatePatientExists(patientId);
        if (invoiceId != null) validateInvoiceExists(invoiceId);
        if (claimId != null) validateClaimExists(claimId);
        log.debug("Listing insurance payments with lines for patient {} invoice {}", patientId, invoiceId);
        
        List<Observation> allObs = new ArrayList<>();
        Bundle bundle = fhirClientService.search(Observation.class, getPracticeId());
        
        // Handle pagination
        while (bundle != null) {
            List<Observation> pageObs = fhirClientService.extractResources(bundle, Observation.class);
            allObs.addAll(pageObs);
            
            String nextLink = bundle.getLink(Bundle.LINK_NEXT) != null 
                ? bundle.getLink(Bundle.LINK_NEXT).getUrl() 
                : null;
            
            if (nextLink != null) {
                bundle = fhirClientService.loadPage(nextLink, getPracticeId());
            } else {
                break;
            }
        }
        
        log.info("Total observations loaded: {}, filtering for remit records with lines", allObs.size());
        
        return allObs.stream()
                .filter(this::isRemitObservation)
                .filter(obs -> patientId == null || patientId.equals(getPatientIdFromObs(obs)))
                .filter(obs -> invoiceId == null || invoiceId.equals(getInvoiceIdFromObs(obs)))
                .map(this::toRemitDtoWithLines)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Apply insurance payment to invoice lines.
     * Creates a single remit record for all lines (batch payment) and updates invoice line amounts based on EOB logic:
     * - Applied Write-off = Submitted - Allowed
     * - Patient Responsibility = Allowed - Insurance Paid
     */
    public InsurancePaymentResponseDto applyInsurancePayment(Long patientId, Long invoiceId, 
                                                    PatientInsurancePaymentRequestDto req) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        if (req == null || req.lines() == null || req.lines().isEmpty()) {
            throw new IllegalArgumentException("Payment request with lines is required");
        }

        // Verify invoice exists and belongs to patient
        Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(invoice, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Invoice ID: %d does not belong to Patient ID: %d", invoiceId, patientId)
            );
        }

        // Create single remit record for all lines
        Observation remit = new Observation();
        remit.setStatus(Observation.ObservationStatus.FINAL);
        remit.setCode(new CodeableConcept().addCoding(
            new Coding("http://ciyex.com/fhir/CodeSystem/remit", "remit", "Insurance Remit Record")
        ));
        remit.addExtension(new Extension(EXT_PATIENT, new StringType(String.valueOf(patientId))));
        remit.addExtension(new Extension(EXT_INVOICE, new StringType(String.valueOf(invoiceId))));
        
        // Add cheque and bank info if provided
        if (req.chequeNumber() != null && !req.chequeNumber().isEmpty()) {
            remit.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/cheque-number", 
                    new StringType(req.chequeNumber())));
        }
        if (req.bankBranch() != null && !req.bankBranch().isEmpty()) {
            remit.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/bank-branch", 
                    new StringType(req.bankBranch())));
        }
        
        // Process each line and add as component to single remit
        BigDecimal totalSubmitted = BigDecimal.ZERO;
        BigDecimal totalAllowed = BigDecimal.ZERO;
        BigDecimal totalInsPay = BigDecimal.ZERO;
        BigDecimal totalDeductible = BigDecimal.ZERO;
        BigDecimal totalInsWriteOff = BigDecimal.ZERO;
        BigDecimal totalBalance = BigDecimal.ZERO;
        
        for (PatientInsuranceRemitLineDto r : req.lines()) {
            // Verify line exists in invoice
            if (invoice.getComponent() == null || invoice.getComponent().isEmpty()) {
                throw new IllegalArgumentException("Invoice has no line items");
            }

            Long invoiceLineId = r.invoiceLineId();
            if (invoiceLineId == null) {
                throw new IllegalArgumentException("Invoice line ID is required in payment request");
            }
            
            // Find line by ID, with fallback to index-based lookup
            Observation.ObservationComponentComponent line = null;
            int lineIndex = -1;
            for (int i = 0; i < invoice.getComponent().size(); i++) {
                Observation.ObservationComponentComponent c = invoice.getComponent().get(i);
                Long lineId = extractLineId(c);
                if (lineId != null && lineId.equals(invoiceLineId)) {
                    line = c;
                    lineIndex = i;
                    break;
                } else if (lineId == null && i == invoiceLineId.intValue()) {
                    // Fallback: if no ID extension, match by index
                    line = c;
                    lineIndex = i;
                    break;
                }
            }
            
            if (line == null) {
                throw new IllegalArgumentException(
                    String.format("Invoice line not found with ID: %d", invoiceLineId)
                );
            }

            // EOB Logic: Submitted - Allowed = Applied Write-off (discount)
            BigDecimal submitted = nz(r.submitted());
            BigDecimal allowed = nz(r.allowed());
            BigDecimal insPay = nz(r.insPay());

            BigDecimal appliedWO = submitted.subtract(allowed).max(BigDecimal.ZERO);

            // Patient Responsibility = Allowed - Insurance Paid
            BigDecimal ptResp = allowed.subtract(insPay).max(BigDecimal.ZERO);

            // Update invoice line with payment amounts
            line.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-charge".equals(e.getUrl()));
            line.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-charge", new DecimalType(submitted)));

            line.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-allowed".equals(e.getUrl()));
            line.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-allowed", new DecimalType(allowed)));

            line.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-ins-writeoff".equals(e.getUrl()));
            line.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-ins-writeoff", new DecimalType(appliedWO)));

            line.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-ins-portion".equals(e.getUrl()));
            line.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-ins-portion", new DecimalType(insPay)));

            line.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-pt-portion".equals(e.getUrl()));
            line.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-pt-portion", new DecimalType(ptResp)));

            // Ensure line has proper ID - use actual line index if ID not set
            line.getExtension().removeIf(e -> EXT_LINE_ID.equals(e.getUrl()));
            line.addExtension(new Extension(EXT_LINE_ID, new IntegerType(lineIndex)));
            
            // Store the line index as the canonical line ID for this invoice
            invoiceLineId = (long) lineIndex;

            // Add line details as component to the single remit record
            Observation.ObservationComponentComponent remitLine = remit.addComponent();
            remitLine.setCode(new CodeableConcept().addCoding(
                new Coding("http://ciyex.com/fhir/CodeSystem/remit-line", "line", "Remit Line")
            ));
            remitLine.addExtension(new Extension(EXT_LINE_ID, new IntegerType(invoiceLineId.intValue())));
            remitLine.addExtension(new Extension(EXT_REMIT_SUBMITTED, new DecimalType(submitted)));
            remitLine.addExtension(new Extension(EXT_REMIT_ALLOWED, new DecimalType(allowed)));
            remitLine.addExtension(new Extension(EXT_REMIT_INS_WRITEOFF, new DecimalType(appliedWO)));
            remitLine.addExtension(new Extension(EXT_REMIT_INS_PAY, new DecimalType(insPay)));
            remitLine.addExtension(new Extension(EXT_REMIT_BALANCE, new DecimalType(ptResp)));
            remitLine.addExtension(new Extension(EXT_REMIT_DEDUCTIBLE, new DecimalType(nz(r.deductible()))));
            if (r.updateAllowed() != null) {
                remitLine.addExtension(new Extension(EXT_UPDATE_ALLOWED, new BooleanType(r.updateAllowed())));
            }
            if (r.updateFlatPortion() != null) {
                remitLine.addExtension(new Extension(EXT_UPDATE_FLAT_PORTION, new BooleanType(r.updateFlatPortion())));
            }
            if (r.applyWriteoff() != null) {
                remitLine.addExtension(new Extension(EXT_APPLY_WRITEOFF, new BooleanType(r.applyWriteoff())));
            }
            
            // Accumulate totals
            totalSubmitted = totalSubmitted.add(submitted);
            totalAllowed = totalAllowed.add(allowed);
            totalInsPay = totalInsPay.add(insPay);
            totalDeductible = totalDeductible.add(nz(r.deductible()));
            totalInsWriteOff = totalInsWriteOff.add(appliedWO);
            totalBalance = totalBalance.add(ptResp);
        }
        
        // Add totals to remit record
        remit.addExtension(new Extension(EXT_REMIT_SUBMITTED, new DecimalType(totalSubmitted)));
        remit.addExtension(new Extension(EXT_REMIT_ALLOWED, new DecimalType(totalAllowed)));
        remit.addExtension(new Extension(EXT_REMIT_INS_PAY, new DecimalType(totalInsPay)));
        remit.addExtension(new Extension(EXT_REMIT_DEDUCTIBLE, new DecimalType(totalDeductible)));
        remit.addExtension(new Extension(EXT_REMIT_INS_WRITEOFF, new DecimalType(totalInsWriteOff)));
        remit.addExtension(new Extension(EXT_REMIT_BALANCE, new DecimalType(totalBalance)));
        
        // Create single remit record - FHIR server will assign ID
        var outcome = fhirClientService.create(remit, getPracticeId());
        String remitIdStr = outcome.getId().getIdPart();
        Long insurancePaymentId;
        try {
            insurancePaymentId = Long.parseLong(remitIdStr);
        } catch (NumberFormatException e) {
            insurancePaymentId = Long.valueOf(Math.abs(remitIdStr.hashCode()));
        }

        // Recalculate and update invoice totals once
        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());

        // Re-read invoice to get latest version before updating claim
        invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        updateClaimStatusIfNeeded(patientId, invoiceId, invoice);

        PatientInvoiceDto invoiceDto = readInvoiceDto(invoiceId, patientId);
        return new InsurancePaymentResponseDto(insurancePaymentId, invoiceDto);
    }


    /**
     * Edit an existing insurance remit line.
     */
    public PatientInvoiceDto editInsuranceRemitLine(Long patientId, Long invoiceId, Long remitId, 
                                                     PatientInsuranceRemitLineDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        // Verify invoice exists
        Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(invoice, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Invoice ID: %d does not belong to Patient ID: %d", invoiceId, patientId)
            );
        }

        // Read and update remit record
        Observation remit = fhirClientService.read(Observation.class, String.valueOf(remitId), getPracticeId());

        if (dto.submitted() != null) {
            remit.getExtension().removeIf(e -> EXT_REMIT_SUBMITTED.equals(e.getUrl()));
            remit.addExtension(new Extension(EXT_REMIT_SUBMITTED, new DecimalType(dto.submitted())));
        }
        if (dto.balance() != null) {
            remit.getExtension().removeIf(e -> EXT_REMIT_BALANCE.equals(e.getUrl()));
            remit.addExtension(new Extension(EXT_REMIT_BALANCE, new DecimalType(dto.balance())));
        }
        if (dto.deductible() != null) {
            remit.getExtension().removeIf(e -> EXT_REMIT_DEDUCTIBLE.equals(e.getUrl()));
            remit.addExtension(new Extension(EXT_REMIT_DEDUCTIBLE, new DecimalType(dto.deductible())));
        }
        if (dto.allowed() != null) {
            remit.getExtension().removeIf(e -> EXT_REMIT_ALLOWED.equals(e.getUrl()));
            remit.addExtension(new Extension(EXT_REMIT_ALLOWED, new DecimalType(dto.allowed())));
        }
        if (dto.insWriteOff() != null) {
            remit.getExtension().removeIf(e -> EXT_REMIT_INS_WRITEOFF.equals(e.getUrl()));
            remit.addExtension(new Extension(EXT_REMIT_INS_WRITEOFF, new DecimalType(dto.insWriteOff())));
        }
        if (dto.insPay() != null) {
            remit.getExtension().removeIf(e -> EXT_REMIT_INS_PAY.equals(e.getUrl()));
            remit.addExtension(new Extension(EXT_REMIT_INS_PAY, new DecimalType(dto.insPay())));
        }
        if (dto.updateAllowed() != null) {
            remit.getExtension().removeIf(e -> EXT_UPDATE_ALLOWED.equals(e.getUrl()));
            remit.addExtension(new Extension(EXT_UPDATE_ALLOWED, new BooleanType(dto.updateAllowed())));
        }
        if (dto.updateFlatPortion() != null) {
            remit.getExtension().removeIf(e -> EXT_UPDATE_FLAT_PORTION.equals(e.getUrl()));
            remit.addExtension(new Extension(EXT_UPDATE_FLAT_PORTION, new BooleanType(dto.updateFlatPortion())));
        }
        if (dto.applyWriteoff() != null) {
            remit.getExtension().removeIf(e -> EXT_APPLY_WRITEOFF.equals(e.getUrl()));
            remit.addExtension(new Extension(EXT_APPLY_WRITEOFF, new BooleanType(dto.applyWriteoff())));
        }

        fhirClientService.update(remit, getPracticeId());

        // Update invoice line if specified
        if (dto.invoiceLineId() != null) {
            Observation.ObservationComponentComponent line = invoice.getComponent().stream()
                    .filter(c -> {
                        Long lineId = extractLineId(c);
                        return lineId != null && lineId.equals(dto.invoiceLineId());
                    })
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Invoice line not found with ID: %d", dto.invoiceLineId())
                    ));

            Extension submittedExt = remit.getExtensionByUrl(EXT_REMIT_SUBMITTED);
            BigDecimal submitted = BigDecimal.ZERO;
            if (submittedExt != null && submittedExt.getValue() instanceof DecimalType dt) {
                submitted = dt.getValue();
            }
            BigDecimal allowed = nz(getDecimalExt(remit, EXT_REMIT_ALLOWED));
            BigDecimal insPay = nz(getDecimalExt(remit, EXT_REMIT_INS_PAY));

            BigDecimal insWO = submitted.subtract(allowed).max(BigDecimal.ZERO);
            BigDecimal ptResp = allowed.subtract(insPay).max(BigDecimal.ZERO);

            line.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-charge".equals(e.getUrl()));
            line.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-charge", new DecimalType(submitted)));

            line.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-allowed".equals(e.getUrl()));
            line.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-allowed", new DecimalType(allowed)));

            line.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-ins-writeoff".equals(e.getUrl()));
            line.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-ins-writeoff", new DecimalType(insWO)));

            line.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-ins-portion".equals(e.getUrl()));
            line.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-ins-portion", new DecimalType(insPay)));

            line.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-pt-portion".equals(e.getUrl()));
            line.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-pt-portion", new DecimalType(ptResp)));
        }

        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());

        return readInvoiceDto(invoiceId, patientId);
    }

    /**
     * VOID = hard delete the remit record.
     */
    public PatientInvoiceDto voidInsurancePayment(Long patientId, Long invoiceId, Long remitId, VoidReason reason) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        validateRemitExists(remitId);
        // Read invoice first to get current version
        Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(invoice, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Invoice ID: %d does not belong to Patient ID: %d", invoiceId, patientId)
            );
        }

        // Verify remit exists before deleting
        if (!remitExists(remitId)) {
            throw new IllegalArgumentException(
                String.format("Insurance remit not found with ID: %d. Please provide a valid remit ID.", remitId)
            );
        }

        // Delete remit record
        fhirClientService.delete(Observation.class, String.valueOf(remitId), getPracticeId());

        // Re-read invoice after delete to get fresh version
        invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        
        // Recalculate invoice totals
        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());

        return readInvoiceDto(invoiceId, patientId);
    }

    private boolean remitExists(Long remitId) {
        try {
            fhirClientService.read(Observation.class, String.valueOf(remitId), getPracticeId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * REFUND insurance → increase insurance portion (reduce insurance paid).
     */
    public PatientInvoiceDto refundInsurancePayment(Long patientId, Long invoiceId, Long remitId, RefundRequest req) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        validateRemitExists(remitId);
        // Verify invoice exists
        Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(invoice, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Invoice ID: %d does not belong to Patient ID: %d", invoiceId, patientId)
            );
        }

        BigDecimal amount = Optional.ofNullable(req).map(RefundRequest::amount)
                .orElseThrow(() -> new IllegalArgumentException("Refund amount required"));
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Refund amount must be > 0");
        }

        // Pick first line and increase insurance portion
        if (invoice.getComponent() == null || invoice.getComponent().isEmpty()) {
            throw new IllegalStateException("No invoice lines to apply insurance refund");
        }

        Observation.ObservationComponentComponent line = invoice.getComponent().get(0);
        BigDecimal insPortion = getComponentDecimal(line, "http://ciyex.com/fhir/StructureDefinition/line-ins-portion");

        line.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-ins-portion".equals(e.getUrl()));
        line.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-ins-portion", 
                new DecimalType(nz(insPortion).add(amount))));

        // Record audit remit with negative insPay
        Observation refund = new Observation();
        refund.setStatus(Observation.ObservationStatus.FINAL);
        refund.setCode(new CodeableConcept().addCoding(
            new Coding("http://ciyex.com/fhir/CodeSystem/remit", "remit-refund", "Insurance Remit Refund")
        ));
        refund.addExtension(new Extension(EXT_PATIENT, new StringType(String.valueOf(patientId))));
        refund.addExtension(new Extension(EXT_INVOICE, new StringType(String.valueOf(invoiceId))));
        refund.addExtension(new Extension(EXT_REMIT_INS_PAY, new DecimalType(amount.negate())));

        fhirClientService.create(refund, getPracticeId());

        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());

        return readInvoiceDto(invoiceId, patientId);
    }

    /**
     * TRANSFER insurance balance → patient account credit.
     * Adjust for insurance overpayment by moving insurance portion to write-off.
     */
    public PatientInvoiceDto transferInsuranceCreditToPatient(Long patientId, Long invoiceId, Long remitId, 
                                                                TransferCreditRequest req) {
        // Verify invoice exists
        Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(invoice, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Invoice ID: %d does not belong to Patient ID: %d", invoiceId, patientId)
            );
        }

        BigDecimal amount = Optional.ofNullable(req).map(TransferCreditRequest::amount)
                .orElseThrow(() -> new IllegalArgumentException("Transfer amount required"));
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Transfer amount must be > 0");
        }

        if (invoice.getComponent() == null || invoice.getComponent().isEmpty()) {
            throw new IllegalStateException("No invoice lines to adjust");
        }

        // Calculate total insurance balance
        BigDecimal totalInsPortion = BigDecimal.ZERO;
        for (Observation.ObservationComponentComponent comp : invoice.getComponent()) {
            totalInsPortion = totalInsPortion.add(getComponentDecimal(comp, 
                    "http://ciyex.com/fhir/StructureDefinition/line-ins-portion"));
        }

        if (amount.compareTo(totalInsPortion) > 0) {
            throw new IllegalArgumentException(
                String.format("Adjustment exceeds insurance balance. Requested: %s, Available: %s", 
                    amount, totalInsPortion)
            );
        }

        // Distribute adjustment proportionally across lines
        BigDecimal remaining = amount;
        for (Observation.ObservationComponentComponent comp : invoice.getComponent()) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal lineInsPortion = getComponentDecimal(comp, 
                    "http://ciyex.com/fhir/StructureDefinition/line-ins-portion");
            BigDecimal toAdjust = lineInsPortion.min(remaining);

            if (toAdjust.compareTo(BigDecimal.ZERO) > 0) {
                comp.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-ins-portion".equals(e.getUrl()));
                comp.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-ins-portion", 
                        new DecimalType(lineInsPortion.subtract(toAdjust))));

                BigDecimal lineWO = getComponentDecimal(comp, 
                        "http://ciyex.com/fhir/StructureDefinition/line-ins-writeoff");
                comp.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-ins-writeoff".equals(e.getUrl()));
                comp.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-ins-writeoff", 
                        new DecimalType(nz(lineWO).add(toAdjust))));

                remaining = remaining.subtract(toAdjust);
            }
        }

        // Record transfer
        Observation transfer = new Observation();
        transfer.setStatus(Observation.ObservationStatus.FINAL);
        transfer.setCode(new CodeableConcept().addCoding(
            new Coding("http://ciyex.com/fhir/CodeSystem/remit", "remit-transfer", "Insurance Transfer")
        ));
        transfer.addExtension(new Extension(EXT_PATIENT, new StringType(String.valueOf(patientId))));
        transfer.addExtension(new Extension(EXT_INVOICE, new StringType(String.valueOf(invoiceId))));
        transfer.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/transfer-amount", 
                new DecimalType(amount)));

        fhirClientService.create(transfer, getPracticeId());

        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());

        return readInvoiceDto(invoiceId, patientId);
    }

    /**
     * Get detailed insurance payment information for a specific remit record.
     */
    public InsurancePaymentDetailDto getInsurancePaymentDetails(Long patientId, Long invoiceId, Long remitId) {
        // Verify patient exists
        try {
            fhirClientService.search(Patient.class, getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }

        // Read remit record
        Observation remitLine;
        try {
            remitLine = fhirClientService.read(Observation.class, String.valueOf(remitId), getPracticeId());
        } catch (Exception e) {
            throw new RuntimeException(
                String.format("Insurance payment not found with ID: %d. Please provide a valid remit ID.", remitId)
            );
        }

        // Read invoice
        Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(invoice, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Invoice ID: %d does not belong to Patient ID: %d", invoiceId, patientId)
            );
        }

        // Build line details
        List<InsurancePaymentDetailDto.InsurancePaymentLineDetailDto> lineDetails = invoice.getComponent().stream()
                .map(line -> {
                    BigDecimal lineTotal = getComponentDecimal(line, 
                            "http://ciyex.com/fhir/StructureDefinition/line-charge");
                    BigDecimal patientPortion = getComponentDecimal(line, 
                            "http://ciyex.com/fhir/StructureDefinition/line-pt-portion");
                    BigDecimal insurancePortion = getComponentDecimal(line, 
                            "http://ciyex.com/fhir/StructureDefinition/line-ins-portion");
                    BigDecimal previousBalance = lineTotal.subtract(patientPortion).subtract(insurancePortion);

                    String description = getComponentString(line, 
                            "http://ciyex.com/fhir/StructureDefinition/line-treatment");
                    if (description == null) {
                        description = getComponentString(line, 
                                "http://ciyex.com/fhir/StructureDefinition/line-code");
                    }

                    String providerName = getComponentString(line, 
                            "http://ciyex.com/fhir/StructureDefinition/line-provider");

                    return InsurancePaymentDetailDto.InsurancePaymentLineDetailDto.builder()
                            .lineId(extractLineId(line))
                            .description(description != null ? description : "")
                            .providerName(providerName != null ? providerName : "")
                            .amount(lineTotal)
                            .patient(patientPortion)
                            .insurance(insurancePortion)
                            .previousBalance(previousBalance)
                            .payment(nz(getDecimalExt(remitLine, EXT_REMIT_INS_PAY)))
                            .build();
                })
                .collect(Collectors.toList());

        // Calculate totals
        BigDecimal insWriteoff = nz(getDecimalExt(remitLine, EXT_REMIT_INS_WRITEOFF));
        BigDecimal insuranceAmount = nz(getDecimalExt(remitLine, EXT_REMIT_INS_PAY));
        BigDecimal patientAmount = getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/pt-balance");
        BigDecimal previousTotalBalance = getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/total-charge");
        BigDecimal paymentAmount = nz(getDecimalExt(remitLine, EXT_REMIT_INS_PAY));
        BigDecimal appliedWO = getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/ins-writeoff");
        BigDecimal ptPaid = getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/pt-balance");
        BigDecimal insPaid = getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/ins-balance");

        return InsurancePaymentDetailDto.builder()
                .remitId(remitId)
                .invoiceId(invoiceId)
                .invoiceNumber(invoiceId.toString())
                .paymentDate(LocalDate.now())
                .chequeNumber("")
                .bankBranchNumber("")
                .insWriteoff(insWriteoff)
                .patientAmount(patientAmount)
                .insuranceAmount(insuranceAmount)
                .previousTotalBalance(previousTotalBalance)
                .paymentAmount(paymentAmount)
                .appliedWO(appliedWO)
                .ptPaid(ptPaid)
                .insPaid(insPaid)
                .lineDetails(lineDetails)
                .build();
    }

    /* ===================== Attachment Management ===================== */

    /**
     * Upload claim attachment as FHIR Binary resource.
     */
    public void uploadClaimAttachment(Long patientId, Long claimId, MultipartFile file) throws Exception {
        validatePatientExists(patientId);
        validateClaimExists(claimId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        // Verify claim exists and belongs to patient
        Claim claim = fhirClientService.read(Claim.class, String.valueOf(claimId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(claim, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Claim ID: %d does not belong to Patient ID: %d", claimId, patientId)
            );
        }

        // Create Binary resource
        Binary binary = new Binary();
        binary.setContentType(file.getContentType());
        binary.setData(file.getBytes());

        var outcome = fhirClientService.create(binary, getPracticeId());
        String binaryId = outcome.getId().getIdPart();

        // Store reference in claim
        claim.getExtension().removeIf(e -> EXT_ATTACHMENT_BINARY.equals(e.getUrl()));
        claim.addExtension(new Extension(EXT_ATTACHMENT_BINARY, 
                new Reference("Binary/" + binaryId)));

        // Increment attachment count
        int attachmentCount = getIntExtension(claim, "http://ciyex.com/fhir/StructureDefinition/attachments-count");
        claim.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/attachments-count".equals(e.getUrl()));
        claim.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/attachments-count", 
                new IntegerType(attachmentCount + 1)));

        fhirClientService.update(claim, getPracticeId());
    }

    /**
     * Download claim attachment.
     */
    public byte[] getClaimAttachment(Long patientId, Long claimId) {
        validatePatientExists(patientId);
        validateClaimExists(claimId);
        // Verify claim exists and belongs to patient
        Claim claim = fhirClientService.read(Claim.class, String.valueOf(claimId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(claim, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Claim ID: %d does not belong to Patient ID: %d", claimId, patientId)
            );
        }

        Extension binaryExt = claim.getExtensionByUrl(EXT_ATTACHMENT_BINARY);
        if (binaryExt == null || !(binaryExt.getValue() instanceof Reference ref)) {
            throw new IllegalArgumentException("No attachment found for claim");
        }

        String binaryId = ref.getReference().replace("Binary/", "");
        Binary binary = fhirClientService.read(Binary.class, binaryId, getPracticeId());

        return binary.getData();
    }

    /**
     * Upload claim EOB document as FHIR Binary resource.
     */
    public void uploadClaimEob(Long patientId, Long claimId, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        // Verify claim exists and belongs to patient
        Claim claim = fhirClientService.read(Claim.class, String.valueOf(claimId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(claim, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Claim ID: %d does not belong to Patient ID: %d", claimId, patientId)
            );
        }

        // Create Binary resource
        Binary binary = new Binary();
        binary.setContentType(file.getContentType());
        binary.setData(file.getBytes());

        var outcome = fhirClientService.create(binary, getPracticeId());
        String binaryId = outcome.getId().getIdPart();

        // Store reference in claim
        claim.getExtension().removeIf(e -> EXT_EOB_BINARY.equals(e.getUrl()));
        claim.addExtension(new Extension(EXT_EOB_BINARY, 
                new Reference("Binary/" + binaryId)));

        // Mark EOB as attached
        claim.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/eob-attached".equals(e.getUrl()));
        claim.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/eob-attached", 
                new BooleanType(true)));

        fhirClientService.update(claim, getPracticeId());
    }

    /**
     * Download claim EOB document.
     */
    public byte[] getClaimEob(Long patientId, Long claimId) {
        // Verify claim exists and belongs to patient
        Claim claim = fhirClientService.read(Claim.class, String.valueOf(claimId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(claim, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Claim ID: %d does not belong to Patient ID: %d", claimId, patientId)
            );
        }

        Extension binaryExt = claim.getExtensionByUrl(EXT_EOB_BINARY);
        if (binaryExt == null || !(binaryExt.getValue() instanceof Reference ref)) {
            throw new IllegalArgumentException("No EOB found for claim");
        }

        String binaryId = ref.getReference().replace("Binary/", "");
        Binary binary = fhirClientService.read(Binary.class, binaryId, getPracticeId());

        return binary.getData();
    }

    /* ===================== Helpers ===================== */

    /**
     * Null-coalescing: return zero if value is null.
     */
    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * Extract line ID from observation component.
     */
    private Long extractLineId(Observation.ObservationComponentComponent comp) {
        Extension ext = comp.getExtensionByUrl(EXT_LINE_ID);
        if (ext != null && ext.getValue() instanceof IntegerType it) {
            return it.getValue().longValue();
        }
        return null;
    }

    /**
     * Recalculate invoice totals from all lines.
     */
    private void recalcInvoiceTotals(Observation invoice) {
        BigDecimal totalCharge = BigDecimal.ZERO;
        BigDecimal totalInsPortion = BigDecimal.ZERO;
        BigDecimal totalPtPortion = BigDecimal.ZERO;

        if (invoice.getComponent() != null) {
            for (Observation.ObservationComponentComponent comp : invoice.getComponent()) {
                totalCharge = totalCharge.add(getComponentDecimal(comp, 
                        "http://ciyex.com/fhir/StructureDefinition/line-charge"));
                totalInsPortion = totalInsPortion.add(getComponentDecimal(comp, 
                        "http://ciyex.com/fhir/StructureDefinition/line-ins-portion"));
                totalPtPortion = totalPtPortion.add(getComponentDecimal(comp, 
                        "http://ciyex.com/fhir/StructureDefinition/line-pt-portion"));
            }
        }

        invoice.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/total-charge".equals(e.getUrl()));
        invoice.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/total-charge", 
                new DecimalType(totalCharge)));

        invoice.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/ins-balance".equals(e.getUrl()));
        invoice.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/ins-balance", 
                new DecimalType(totalInsPortion)));

        invoice.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/pt-balance".equals(e.getUrl()));
        invoice.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/pt-balance", 
                new DecimalType(totalPtPortion)));
    }

    /**
     * Update associated claim status if patient balance reaches zero.
     */
    private void updateClaimStatusIfNeeded(Long patientId, Long invoiceId, Observation invoice) {
        try {
            Bundle claimBundle = fhirClientService.search(Claim.class, getPracticeId());
            Optional<Claim> claim = fhirClientService.extractResources(claimBundle, Claim.class).stream()
                    .filter(c -> String.valueOf(invoiceId).equals(optStringExt(c, EXT_INVOICE)))
                    .filter(c -> String.valueOf(patientId).equals(optStringExt(c, EXT_PATIENT)))
                    .findFirst();

            if (claim.isPresent()) {
                BigDecimal ptBalance = getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/pt-balance");
                String newStatus = ptBalance.compareTo(BigDecimal.ZERO) == 0 ? "accepted" : "in-process";

                Claim c = claim.get();
                c.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/claim-status".equals(e.getUrl()));
                c.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/claim-status", 
                        new StringType(newStatus)));

                fhirClientService.update(c, getPracticeId());
            }
        } catch (Exception e) {
            log.warn("Unable to update claim status: {}", e.getMessage());
        }
    }

    /**
     * Get string extension value (optional).
     */
    private String optStringExt(DomainResource resource, String url) {
        if (resource == null) return null;
        Extension ext = resource.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType st) {
            return st.getValue();
        }
        return null;
    }

    /**
     * Get string value from observation component extension.
     */
    private String getComponentString(Observation.ObservationComponentComponent comp, String url) {
        if (comp == null) return null;
        Extension ext = comp.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType st) {
            return st.getValue();
        }
        return null;
    }

    /**
     * Get decimal value from observation component extension.
     */
    private BigDecimal getComponentDecimal(Observation.ObservationComponentComponent comp, String url) {
        if (comp == null) return BigDecimal.ZERO;
        Extension ext = comp.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof DecimalType dt) {
            return dt.getValue();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get decimal value from observation extension.
     */
    private BigDecimal getDecimalExt(Observation obs, String url) {
        if (obs == null) return BigDecimal.ZERO;
        Extension ext = obs.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof DecimalType dt) {
            return dt.getValue();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get integer value from resource extension.
     */
    private int getIntExtension(DomainResource resource, String url) {
        if (resource == null) return 0;
        Extension ext = resource.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof IntegerType it) {
            return it.getValue();
        }
        return 0;
    }

    /**
     * Get decimal value from observation extension, with invoice-level totals.
     */
    private BigDecimal getInvoiceExtDecimal(Observation obs, String url) {
        if (obs == null) return BigDecimal.ZERO;
        Extension ext = obs.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof DecimalType dt) {
            return dt.getValue();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get code from observation.
     */
    private String getCodeFromObservation(Observation obs) {
        if (obs == null || obs.getCode() == null || !obs.getCode().hasCoding()) {
            return null;
        }
        return obs.getCode().getCodingFirstRep().getCode();
    }

    /**
     * Check if observation is a remit record.
     */
    private boolean isRemitObservation(Observation obs) {
        if (obs == null) return false;
        String code = getCodeFromObservation(obs);
        return code != null && (code.equals("remit") || code.startsWith("remit-"));
    }

    private Long getPatientIdFromObs(Observation obs) {
        Extension ext = obs.getExtensionByUrl(EXT_PATIENT);
        if (ext != null && ext.getValue() instanceof StringType st) {
            try {
                return Long.parseLong(st.getValue());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Long getInvoiceIdFromObs(Observation obs) {
        Extension ext = obs.getExtensionByUrl(EXT_INVOICE);
        if (ext != null && ext.getValue() instanceof StringType st) {
            try {
                return Long.parseLong(st.getValue());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /**
     * Convert FHIR Observation to PatientInsuranceRemitLineDto.
     * Now handles remit records with multiple line components.
     */
    private PatientInsuranceRemitLineDto toRemitDto(Observation obs) {
        try {
            String fhirId = obs.getIdElement().getIdPart();
            Long id;
            try {
                id = Long.parseLong(fhirId);
            } catch (NumberFormatException e) {
                id = Long.valueOf(Math.abs(fhirId.hashCode()));
            }

            Long patientId = getPatientIdFromObs(obs);
            Long invoiceId = getInvoiceIdFromObs(obs);
            Long insuranceId = null; // Can be added if stored in extensions

            Long invoiceLineId = null;
            if (obs.getComponent() != null && !obs.getComponent().isEmpty()) {
                Extension lineExt = obs.getComponentFirstRep().getExtensionByUrl(EXT_LINE_ID);
                if (lineExt != null && lineExt.getValue() instanceof IntegerType it) {
                    invoiceLineId = it.getValue().longValue();
                }
            }

            String chequeNumber = optStringExt(obs, "http://ciyex.com/fhir/StructureDefinition/cheque-number");
            String bankBranch = optStringExt(obs, "http://ciyex.com/fhir/StructureDefinition/bank-branch");

            return new PatientInsuranceRemitLineDto(
                    id,
                    patientId,
                    invoiceId,
                    insuranceId,
                    invoiceLineId,
                    getDecimalExt(obs, EXT_REMIT_SUBMITTED),
                    getDecimalExt(obs, EXT_REMIT_BALANCE),
                    getDecimalExt(obs, EXT_REMIT_DEDUCTIBLE),
                    getDecimalExt(obs, EXT_REMIT_ALLOWED),
                    getDecimalExt(obs, EXT_REMIT_INS_WRITEOFF),
                    getDecimalExt(obs, EXT_REMIT_INS_PAY),
                    getBooleanExtension(obs, EXT_UPDATE_ALLOWED),
                    getBooleanExtension(obs, EXT_UPDATE_FLAT_PORTION),
                    getBooleanExtension(obs, EXT_APPLY_WRITEOFF),
                    chequeNumber,
                    bankBranch
            );
        } catch (Exception e) {
            log.warn("Error converting observation to remit DTO: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convert FHIR Observation to PatientInsuranceRemitWithLinesDto with all line details.
     */
    private PatientInsuranceRemitWithLinesDto toRemitDtoWithLines(Observation obs) {
        try {
            String fhirId = obs.getIdElement().getIdPart();
            Long id;
            try {
                id = Long.parseLong(fhirId);
            } catch (NumberFormatException e) {
                id = Long.valueOf(Math.abs(fhirId.hashCode()));
            }

            Long patientId = getPatientIdFromObs(obs);
            Long invoiceId = getInvoiceIdFromObs(obs);
            Long insuranceId = null;

            String chequeNumber = optStringExt(obs, "http://ciyex.com/fhir/StructureDefinition/cheque-number");
            String bankBranch = optStringExt(obs, "http://ciyex.com/fhir/StructureDefinition/bank-branch");

            // Extract all line components
            List<PatientInsuranceRemitWithLinesDto.RemitLineDetail> lines = new ArrayList<>();
            if (obs.getComponent() != null) {
                for (Observation.ObservationComponentComponent comp : obs.getComponent()) {
                    Extension lineIdExt = comp.getExtensionByUrl(EXT_LINE_ID);
                    Long lineId = null;
                    if (lineIdExt != null && lineIdExt.getValue() instanceof IntegerType it) {
                        lineId = it.getValue().longValue();
                    }

                    BigDecimal submitted = getComponentDecimalFromExt(comp, EXT_REMIT_SUBMITTED);
                    BigDecimal allowed = getComponentDecimalFromExt(comp, EXT_REMIT_ALLOWED);
                    BigDecimal insWriteOff = getComponentDecimalFromExt(comp, EXT_REMIT_INS_WRITEOFF);
                    BigDecimal insPay = getComponentDecimalFromExt(comp, EXT_REMIT_INS_PAY);
                    BigDecimal balance = getComponentDecimalFromExt(comp, EXT_REMIT_BALANCE);
                    BigDecimal deductible = getComponentDecimalFromExt(comp, EXT_REMIT_DEDUCTIBLE);

                    Boolean updateAllowed = getComponentBooleanFromExt(comp, EXT_UPDATE_ALLOWED);
                    Boolean updateFlatPortion = getComponentBooleanFromExt(comp, EXT_UPDATE_FLAT_PORTION);
                    Boolean applyWriteoff = getComponentBooleanFromExt(comp, EXT_APPLY_WRITEOFF);

                    lines.add(new PatientInsuranceRemitWithLinesDto.RemitLineDetail(
                            lineId, submitted, allowed, insWriteOff, insPay, balance, deductible,
                            updateAllowed, updateFlatPortion, applyWriteoff
                    ));
                }
            }

            return new PatientInsuranceRemitWithLinesDto(
                    id,
                    patientId,
                    invoiceId,
                    insuranceId,
                    getDecimalExt(obs, EXT_REMIT_SUBMITTED),
                    getDecimalExt(obs, EXT_REMIT_BALANCE),
                    getDecimalExt(obs, EXT_REMIT_DEDUCTIBLE),
                    getDecimalExt(obs, EXT_REMIT_ALLOWED),
                    getDecimalExt(obs, EXT_REMIT_INS_WRITEOFF),
                    getDecimalExt(obs, EXT_REMIT_INS_PAY),
                    chequeNumber,
                    bankBranch,
                    lines
            );
        } catch (Exception e) {
            log.warn("Error converting observation to remit DTO with lines: {}", e.getMessage());
            return null;
        }
    }

    private BigDecimal getComponentDecimalFromExt(Observation.ObservationComponentComponent comp, String url) {
        if (comp == null) return BigDecimal.ZERO;
        Extension ext = comp.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof DecimalType dt) {
            return dt.getValue();
        }
        return BigDecimal.ZERO;
    }

    private Boolean getComponentBooleanFromExt(Observation.ObservationComponentComponent comp, String url) {
        if (comp == null) return null;
        Extension ext = comp.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof BooleanType bt) {
            return bt.getValue();
        }
        return null;
    }

    /**
     * Get boolean value from resource extension.
     */
    private Boolean getBooleanExtension(DomainResource resource, String url) {
        if (resource == null) return null;
        Extension ext = resource.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof BooleanType bt) {
            return bt.getValue();
        }
        return null;
    }

    /**
     * Read invoice DTO from FHIR Observation.
     */
    private PatientInvoiceDto readInvoiceDto(Long invoiceId, Long patientId) {
        try {
            Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
            return fromFhirObservation(invoice, patientId);
        } catch (Exception e) {
            log.error("Error reading invoice: {}", e.getMessage());
            throw new IllegalArgumentException(
                String.format("Unable to read invoice ID: %d", invoiceId)
            );
        }
    }

    /**
     * Convert FHIR Observation to PatientInvoiceDto.
     */
    private PatientInvoiceDto fromFhirObservation(Observation obs, Long patientId) {
        List<PatientInvoiceLineDto> lines = new ArrayList<>();
        if (obs.getComponent() != null) {
            int index = 0;
            for (Observation.ObservationComponentComponent comp : obs.getComponent()) {
                Long lineId = extractLineId(comp);
                // If no line ID found, use index as fallback
                if (lineId == null) {
                    lineId = (long) index;
                }
                String dosStr = getComponentString(comp, "http://ciyex.com/fhir/StructureDefinition/line-dos");
                LocalDate dos = dosStr != null ? LocalDate.parse(dosStr) : LocalDate.now();
                String code = getComponentString(comp, "http://ciyex.com/fhir/StructureDefinition/line-code");
                String treatment = getComponentString(comp, "http://ciyex.com/fhir/StructureDefinition/line-treatment");
                String provider = getComponentString(comp, "http://ciyex.com/fhir/StructureDefinition/line-provider");
                BigDecimal charge = getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-charge");
                BigDecimal allowed = getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-allowed");
                BigDecimal insWO = getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-ins-writeoff");
                BigDecimal insPortion = getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-ins-portion");
                BigDecimal ptPortion = getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-pt-portion");

                lines.add(new PatientInvoiceLineDto(lineId, dos, code, treatment, provider, charge, allowed, insWO, insPortion, ptPortion));
                index++;
            }
        }

        String statusStr = optStringExt(obs, "http://ciyex.com/fhir/StructureDefinition/invoice-status");
        PatientInvoiceStatus status = PatientInvoiceStatus.OPEN;
        if (statusStr != null) {
            try {
                status = PatientInvoiceStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                status = PatientInvoiceStatus.OPEN;
            }
        }
        BigDecimal insWO = getInvoiceExtDecimal(obs, "http://ciyex.com/fhir/StructureDefinition/ins-writeoff");
        BigDecimal appliedWO = BigDecimal.ZERO;
        BigDecimal ptBalance = getInvoiceExtDecimal(obs, "http://ciyex.com/fhir/StructureDefinition/pt-balance");
        BigDecimal insBalance = getInvoiceExtDecimal(obs, "http://ciyex.com/fhir/StructureDefinition/ins-balance");
        BigDecimal totalCharge = getInvoiceExtDecimal(obs, "http://ciyex.com/fhir/StructureDefinition/total-charge");

        // invoice id
        String fhirId = obs.getIdElement().getIdPart();
        Long invoiceId = null;
        try { invoiceId = Long.parseLong(fhirId); } catch (Exception ex) { invoiceId = Long.valueOf(Math.abs(fhirId.hashCode())); }

        // invoice date
        String invDateStr = optStringExt(obs, "http://ciyex.com/fhir/StructureDefinition/invoice-date");
        java.time.LocalDateTime invoiceDate = null;
        if (invDateStr != null && !invDateStr.isEmpty()) {
            try { invoiceDate = java.time.LocalDateTime.parse(invDateStr); }
            catch (Exception e) {
                try { invoiceDate = java.time.LocalDate.parse(invDateStr).atStartOfDay(); } catch (Exception ex) { invoiceDate = null; }
            }
        }

        return new PatientInvoiceDto(invoiceId, patientId, invoiceDate, status, insWO, appliedWO, ptBalance, insBalance, totalCharge, lines);
    }

    private void validatePatientExists(Long patientId) {
        if (patientId == null) throw new IllegalArgumentException("Patient ID cannot be null");
        try {
            fhirClientService.read(Patient.class, String.valueOf(patientId), getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId));
        }
    }

    private void validateInvoiceExists(Long invoiceId) {
        if (invoiceId == null) throw new IllegalArgumentException("Invoice ID cannot be null");
        try {
            fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Invoice not found with ID: %d. Please provide a valid Invoice ID.", invoiceId));
        }
    }

    private void validateRemitExists(Long remitId) {
        if (remitId == null) throw new IllegalArgumentException("Remit ID cannot be null");
        try {
            fhirClientService.read(Observation.class, String.valueOf(remitId), getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Insurance remit not found with ID: %d. Please provide a valid remit ID.", remitId));
        }
    }

    private void validateClaimExists(Long claimId) {
        if (claimId == null) throw new IllegalArgumentException("Claim ID cannot be null");
        try {
            fhirClientService.read(Claim.class, String.valueOf(claimId), getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Claim not found with ID: %d. Please provide a valid Claim ID.", claimId));
        }
    }
}
