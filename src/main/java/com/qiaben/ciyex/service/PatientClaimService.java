package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.*;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientClaimService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final PatientInvoiceService invoiceService;

    // FHIR Extension URLs
    private static final String EXT_PATIENT = "http://ciyex.com/fhir/StructureDefinition/patient-reference";
    private static final String EXT_INVOICE = "http://ciyex.com/fhir/StructureDefinition/invoice-reference";
    private static final String EXT_PAYER = "http://ciyex.com/fhir/StructureDefinition/payer-name";
    private static final String EXT_TREATING_PROVIDER = "http://ciyex.com/fhir/StructureDefinition/treating-provider";
    private static final String EXT_BILLING_ENTITY = "http://ciyex.com/fhir/StructureDefinition/billing-entity";
    private static final String EXT_CLAIM_TYPE = "http://ciyex.com/fhir/StructureDefinition/claim-type";
    private static final String EXT_NOTES = "http://ciyex.com/fhir/StructureDefinition/claim-notes";
    private static final String EXT_STATUS = "http://ciyex.com/fhir/StructureDefinition/claim-status";
    private static final String EXT_ATTACHMENTS = "http://ciyex.com/fhir/StructureDefinition/attachments-count";
    private static final String EXT_EOB_ATTACHED = "http://ciyex.com/fhir/StructureDefinition/eob-attached";
    private static final String EXT_CREATED_ON = "http://ciyex.com/fhir/StructureDefinition/created-on";
    private static final String EXT_PLAN_NAME = "http://ciyex.com/fhir/StructureDefinition/plan-name";
    private static final String EXT_PROVIDER = "http://ciyex.com/fhir/StructureDefinition/provider";
    private static final String EXT_POLICY_NUMBER = "http://ciyex.com/fhir/StructureDefinition/policy-number";
    private static final String EXT_PATIENT_NAME = "http://ciyex.com/fhir/StructureDefinition/patient-name";
    private static final String EXT_ATTACHMENT_BINARY = "http://ciyex.com/fhir/StructureDefinition/attachment-binary";
    private static final String EXT_EOB_BINARY = "http://ciyex.com/fhir/StructureDefinition/eob-binary";
    private static final String EXT_LOCKED = "http://ciyex.com/fhir/StructureDefinition/claim-locked";
    private static final String EXT_REMIT_DATE = "http://ciyex.com/fhir/StructureDefinition/remit-date";
    private static final String EXT_PAYMENT_AMOUNT = "http://ciyex.com/fhir/StructureDefinition/insurance-payment-amount";
    private static final String EXT_ATTACHMENT_INDICATOR = "http://ciyex.com/fhir/StructureDefinition/attachment-indicator";
    private static final String EXT_ATTACHMENT_TYPE = "http://ciyex.com/fhir/StructureDefinition/attachment-type";
    private static final String EXT_ATTACHMENT_TRANSMISSION_CODE = "http://ciyex.com/fhir/StructureDefinition/attachment-transmission-code";
    private static final String EXT_CLAIM_SUBMISSION_REASON_CODE = "http://ciyex.com/fhir/StructureDefinition/claim-submission-reason-code";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // ===================== Request Records =====================
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

    // ===================== Claims =====================

    /**
     * Fetch all claims for all patients in the org (for All Claims view)
     */
    public List<PatientClaimDto> listAllClaims() {
        Bundle bundle = fhirClientService.search(Claim.class, getPracticeId());
        return fhirClientService.extractResources(bundle, Claim.class).stream()
                .map(this::fromFhirClaim)
                .sorted((a, b) -> Long.compare(b.id(), a.id()))
                .collect(Collectors.toList());
    }

    /**
     * Fetch all claims for a specific patient
     */
    public List<PatientClaimDto> listAllClaimsForPatient(Long patientId) {
        validatePatientExists(patientId);
        Bundle bundle = fhirClientService.search(Claim.class, getPracticeId());
        return fhirClientService.extractResources(bundle, Claim.class).stream()
                .filter(c -> patientId.equals(getPatientFromClaim(c)))
                .map(this::fromFhirClaim)
                .sorted((a, b) -> Long.compare(b.id(), a.id()))
                .collect(Collectors.toList());
    }

    /**
     * Get the active claim for a specific invoice
     */
    public PatientClaimDto getActiveClaimForInvoice(Long patientId, Long invoiceId) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        return fromFhirClaim(getClaimOrThrow(patientId, invoiceId));
    }

    /**
     * List all claims (historical and active) for a specific invoice
     */
    public List<PatientClaimDto> listClaimsForInvoice(Long patientId, Long invoiceId) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        Bundle bundle = fhirClientService.search(Claim.class, getPracticeId());
        return fhirClientService.extractResources(bundle, Claim.class).stream()
                .filter(c -> invoiceId.equals(getInvoiceFromClaim(c)) && patientId.equals(getPatientFromClaim(c)))
                .map(this::fromFhirClaim)
                .sorted((a, b) -> Long.compare(b.id(), a.id()))
                .collect(Collectors.toList());
    }

    /**
     * Promote claim from DRAFT to IN_PROCESS status
     * Fetches and populates coverage data from patient's most recent coverage
     * Auto-creates claim if it doesn't exist
     */
    public PatientClaimDto promoteClaim(Long patientId, Long invoiceId) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        Claim claim;
        try {
            claim = getClaimOrThrow(patientId, invoiceId);
        } catch (IllegalArgumentException e) {
            log.warn("Claim not found for invoice {}, creating new draft claim", invoiceId);
            claim = createDraftClaim(patientId, invoiceId);
        }
        
        String currentStatus = optStringExt(claim, EXT_STATUS);
        
        if ("DRAFT".equals(currentStatus)) {
            claim.getExtension().removeIf(e -> EXT_STATUS.equals(e.getUrl()));
            claim.addExtension(new Extension(EXT_STATUS, new StringType("IN_PROCESS")));
            log.info("Promoting claim for invoice {} to IN_PROCESS status", invoiceId);
            fhirClientService.update(claim, getPracticeId());
        }
        return fromFhirClaim(claim);
    }

    /**
     * Create a draft claim for an invoice
     */
    private Claim createDraftClaim(Long patientId, Long invoiceId) {
        Claim claim = new Claim();
        claim.setStatus(Claim.ClaimStatus.ACTIVE);
        claim.addExtension(new Extension(EXT_PATIENT, new StringType(patientId.toString())));
        claim.addExtension(new Extension(EXT_INVOICE, new StringType(invoiceId.toString())));
        claim.addExtension(new Extension(EXT_STATUS, new StringType("DRAFT")));
        claim.addExtension(new Extension(EXT_CREATED_ON, new StringType(LocalDate.now().toString())));
        claim.addExtension(new Extension(EXT_CLAIM_TYPE, new StringType("Electronic")));
        
        var outcome = fhirClientService.create(claim, getPracticeId());
        log.info("Created draft claim for invoice {} and patient {}", invoiceId, patientId);
        return fhirClientService.read(Claim.class, outcome.getId().getIdPart(), getPracticeId());
    }

    /**
     * Send claim to batch - change status to READY_FOR_SUBMISSION
     */
    public PatientClaimDto sendClaimToBatch(Long patientId, Long invoiceId) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        Claim claim = getClaimOrThrow(patientId, invoiceId);
        claim.getExtension().removeIf(e -> EXT_STATUS.equals(e.getUrl()));
        claim.addExtension(new Extension(EXT_STATUS, new StringType("READY_FOR_SUBMISSION")));
        fhirClientService.update(claim, getPracticeId());
        return fromFhirClaim(claim);
    }

    /**
     * Submit claim - change status to SUBMITTED
     */
    public PatientClaimDto submitClaim(Long patientId, Long invoiceId) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        Claim claim = getClaimOrThrow(patientId, invoiceId);
        claim.getExtension().removeIf(e -> EXT_STATUS.equals(e.getUrl()));
        claim.addExtension(new Extension(EXT_STATUS, new StringType("SUBMITTED")));
        fhirClientService.update(claim, getPracticeId());
        return fromFhirClaim(claim);
    }

    /**
     * Close claim - change status to CLOSED
     */
    public PatientClaimDto closeClaim(Long patientId, Long invoiceId) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        Claim claim = getClaimOrThrow(patientId, invoiceId);
        claim.getExtension().removeIf(e -> EXT_STATUS.equals(e.getUrl()));
        claim.addExtension(new Extension(EXT_STATUS, new StringType("CLOSED")));
        fhirClientService.update(claim, getPracticeId());
        return fromFhirClaim(claim);
    }

    /**
     * Void and recreate claim - mark existing as VOID and create new DRAFT claim
     */
    public PatientClaimDto voidAndRecreateClaim(Long patientId, Long invoiceId) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        Claim existing = getClaimOrThrow(patientId, invoiceId);
        
        // Mark existing claim as VOID
        existing.getExtension().removeIf(e -> EXT_STATUS.equals(e.getUrl()));
        existing.addExtension(new Extension(EXT_STATUS, new StringType("VOID")));
        fhirClientService.update(existing, getPracticeId());

        // Create fresh DRAFT claim
        Claim fresh = new Claim();
        fresh.addExtension(new Extension(EXT_PATIENT, new StringType(patientId.toString())));
        fresh.addExtension(new Extension(EXT_INVOICE, new StringType(invoiceId.toString())));
        fresh.addExtension(new Extension(EXT_STATUS, new StringType("DRAFT")));
        fresh.addExtension(new Extension(EXT_CREATED_ON, new StringType(LocalDate.now().toString())));
        
        var outcome = fhirClientService.create(fresh, getPracticeId());
        Claim created = fhirClientService.read(Claim.class, outcome.getId().getIdPart(), getPracticeId());
        
        return fromFhirClaim(created);
    }

    /**
     * Void and recreate claim by claim ID (for All Claims view)
     * The existing claim is DELETED from the database (void = delete)
     * A new claim is created with DRAFT status for the same invoice
     */
    public PatientClaimDto voidAndRecreateClaimById(Long claimId) {
        validateClaimExists(claimId);
        Claim existing = fhirClientService.read(Claim.class, claimId.toString(), getPracticeId());

        // Store patient and invoice IDs before deleting
        Long patientId = getPatientFromClaim(existing);
        Long invoiceId = getInvoiceFromClaim(existing);

        // Delete the existing claim from FHIR (void = delete)
        fhirClientService.delete(Claim.class, existing.getIdElement().getIdPart(), getPracticeId());

        // Create new claim with same patient and invoice
        Claim fresh = new Claim();
        fresh.addExtension(new Extension(EXT_PATIENT, new StringType(patientId.toString())));
        fresh.addExtension(new Extension(EXT_INVOICE, new StringType(invoiceId.toString())));
        fresh.addExtension(new Extension(EXT_STATUS, new StringType("DRAFT")));
        fresh.addExtension(new Extension(EXT_CREATED_ON, new StringType(LocalDate.now().toString())));
        
        var outcome = fhirClientService.create(fresh, getPracticeId());
        Claim created = fhirClientService.read(Claim.class, outcome.getId().getIdPart(), getPracticeId());
        
        return fromFhirClaim(created);
    }

    /**
     * Update claim fields
     */
    public PatientClaimDto updateClaim(Long patientId, Long invoiceId, PatientClaimCoreUpdate p) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        if (p == null) {
            throw new IllegalArgumentException("Claim update request is required");
        }

        Claim claim = getClaimOrThrow(patientId, invoiceId);

        // Update treating provider
        claim.getExtension().removeIf(e -> EXT_TREATING_PROVIDER.equals(e.getUrl()));
        if (p.treatingProviderId() != null) {
            claim.addExtension(new Extension(EXT_TREATING_PROVIDER, new StringType(p.treatingProviderId())));
        }

        // Update billing entity
        claim.getExtension().removeIf(e -> EXT_BILLING_ENTITY.equals(e.getUrl()));
        if (p.billingEntity() != null) {
            claim.addExtension(new Extension(EXT_BILLING_ENTITY, new StringType(p.billingEntity())));
        }

        // Update claim type
        claim.getExtension().removeIf(e -> EXT_CLAIM_TYPE.equals(e.getUrl()));
        if (p.type() != null) {
            claim.addExtension(new Extension(EXT_CLAIM_TYPE, new StringType(p.type())));
        }

        // Update notes
        claim.getExtension().removeIf(e -> EXT_NOTES.equals(e.getUrl()));
        if (p.notes() != null) {
            claim.addExtension(new Extension(EXT_NOTES, new StringType(p.notes())));
        }

        // Update attachment indicator
        claim.getExtension().removeIf(e -> EXT_ATTACHMENT_INDICATOR.equals(e.getUrl()));
        if (p.attachmentIndicator() != null) {
            claim.addExtension(new Extension(EXT_ATTACHMENT_INDICATOR, new StringType(p.attachmentIndicator())));
        }

        // Update attachment type
        claim.getExtension().removeIf(e -> EXT_ATTACHMENT_TYPE.equals(e.getUrl()));
        if (p.attachmentType() != null) {
            claim.addExtension(new Extension(EXT_ATTACHMENT_TYPE, new StringType(p.attachmentType())));
        }

        // Update attachment transmission code
        claim.getExtension().removeIf(e -> EXT_ATTACHMENT_TRANSMISSION_CODE.equals(e.getUrl()));
        if (p.attachmentTransmissionCode() != null) {
            claim.addExtension(new Extension(EXT_ATTACHMENT_TRANSMISSION_CODE, new StringType(p.attachmentTransmissionCode())));
        }

        // Update claim submission reason code
        claim.getExtension().removeIf(e -> EXT_CLAIM_SUBMISSION_REASON_CODE.equals(e.getUrl()));
        if (p.claimSubmissionReasonCode() != null) {
            claim.addExtension(new Extension(EXT_CLAIM_SUBMISSION_REASON_CODE, new StringType(p.claimSubmissionReasonCode())));
        }

        fhirClientService.update(claim, getPracticeId());
        return fromFhirClaim(claim);
    }

    /**
     * Convert claim type (manual/electronic)
     */
    public PatientClaimDto convertClaimType(Long claimId, String targetType) {
        validateClaimExists(claimId);
        Claim claim = fhirClientService.read(Claim.class, claimId.toString(), getPracticeId());
        
        claim.getExtension().removeIf(e -> EXT_CLAIM_TYPE.equals(e.getUrl()));
        claim.addExtension(new Extension(EXT_CLAIM_TYPE, new StringType(targetType)));
        
        fhirClientService.update(claim, getPracticeId());
        return getClaimDtoById(claimId);
    }

    /**
     * Get claim line details (DOS, code, description, provider, total submitted amount)
     * Fetches invoice lines associated with the claim
     */
    public List<ClaimLineDetailDto> getClaimLineDetails(Long claimId) {
        validateClaimExists(claimId);
        Claim claim = fhirClientService.read(Claim.class, claimId.toString(), getPracticeId());
        Long invoiceId = getInvoiceFromClaim(claim);
        
        if (invoiceId == null) {
            return List.of();
        }

        // Fetch invoice lines using the invoice service
        try {
            List<PatientInvoiceLineDto> lines = invoiceService.getInvoiceLines(getPatientFromClaim(claim), invoiceId);
            
            // Convert to ClaimLineDetailDto
            return lines.stream()
                    .map(line -> new ClaimLineDetailDto(
                            line.id(),
                            line.dos(),
                            line.code(),
                            line.treatment(),  // This is the description
                            line.provider(),
                            line.charge()      // This is the total submitted amount
                    ))
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Failed to fetch invoice lines for claim {}", claimId, ex);
            return List.of();
        }
    }

    /**
     * Lock claim (after lock, claim cannot be edited)
     */
    public void lockClaim(Long patientId, Long claimId) {
        validatePatientExists(patientId);
        validateClaimExists(claimId);
        Claim claim = fhirClientService.read(Claim.class, claimId.toString(), getPracticeId());

        if (!patientId.equals(getPatientFromClaim(claim))) {
            throw new IllegalArgumentException(String.format(
                "Claim not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Claim ID are correct and that the claim belongs to this patient.",
                claimId, patientId));
        }

        claim.getExtension().removeIf(e -> EXT_LOCKED.equals(e.getUrl()));
        claim.addExtension(new Extension(EXT_LOCKED, new BooleanType(true)));
        fhirClientService.update(claim, getPracticeId());
    }

    /**
     * Change claim status and related fields
     */
    public void changeClaimStatus(Long patientId, Long claimId, ClaimStatusUpdateDto dto) {
        if (patientId != null) validatePatientExists(patientId);
        validateClaimExists(claimId);
        Claim claim = fhirClientService.read(Claim.class, claimId.toString(), getPracticeId());

        if (patientId != null && !patientId.equals(getPatientFromClaim(claim))) {
            throw new IllegalArgumentException(String.format(
                "Claim not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Claim ID are correct and that the claim belongs to this patient.",
                claimId, patientId));
        }

        // Update status
        if (dto.getStatus() != null) {
            claim.getExtension().removeIf(e -> EXT_STATUS.equals(e.getUrl()));
            claim.addExtension(new Extension(EXT_STATUS, new StringType(dto.getStatus())));
        }

        // Update remit date
        if (dto.getRemitDate() != null && !dto.getRemitDate().isEmpty()) {
            claim.getExtension().removeIf(e -> EXT_REMIT_DATE.equals(e.getUrl()));
            claim.addExtension(new Extension(EXT_REMIT_DATE, new StringType(dto.getRemitDate())));
        }

        // Update payment amount
        if (dto.getPaymentAmount() != null) {
            claim.getExtension().removeIf(e -> EXT_PAYMENT_AMOUNT.equals(e.getUrl()));
            claim.addExtension(new Extension(EXT_PAYMENT_AMOUNT, new StringType(dto.getPaymentAmount().toPlainString())));
        }

        fhirClientService.update(claim, getPracticeId());
    }

    /**
     * Get claim by ID and convert to DTO
     */
    public PatientClaimDto getClaimDtoById(Long claimId) {
        validateClaimExists(claimId);
        Claim claim = fhirClientService.read(Claim.class, claimId.toString(), getPracticeId());
        return fromFhirClaim(claim);
    }



    // ===================== Attachments & EOB =====================

    /**
     * Submit claim attachment
     */
    public void submitClaimAttachment(Long patientId, Long claimId, MultipartFile file) throws Exception {
        validatePatientExists(patientId);
        validateClaimExists(claimId);
        Claim claim = fhirClientService.read(Claim.class, claimId.toString(), getPracticeId());

        if (!patientId.equals(getPatientFromClaim(claim))) {
            throw new IllegalArgumentException(String.format(
                "Claim not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Claim ID are correct and that the claim belongs to this patient.",
                claimId, patientId));
        }

        if (file != null && !file.isEmpty()) {
            // Create a Binary resource to store the file
            Binary binary = new Binary();
            binary.setContentType("application/octet-stream");
            binary.setData(file.getBytes());
            
            var outcome = fhirClientService.create(binary, getPracticeId());
            String binaryId = outcome.getId().getIdPart();
            
            // Store reference to binary in claim
            claim.getExtension().removeIf(e -> EXT_ATTACHMENT_BINARY.equals(e.getUrl()));
            claim.addExtension(new Extension(EXT_ATTACHMENT_BINARY, new StringType(binaryId)));
            
            // Increment attachment count
            int count = getIntExtension(claim, EXT_ATTACHMENTS);
            claim.getExtension().removeIf(e -> EXT_ATTACHMENTS.equals(e.getUrl()));
            claim.addExtension(new Extension(EXT_ATTACHMENTS, new IntegerType(count + 1)));
            
            fhirClientService.update(claim, getPracticeId());
        }
    }

    /**
     * Get claim attachment
     */
    public byte[] getClaimAttachment(Long patientId, Long claimId) {
        validatePatientExists(patientId);
        validateClaimExists(claimId);
        Claim claim = fhirClientService.read(Claim.class, claimId.toString(), getPracticeId());
        
        if (!patientId.equals(getPatientFromClaim(claim))) {
            throw new IllegalArgumentException(String.format(
                "Claim not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Claim ID are correct and that the claim belongs to this patient.",
                claimId, patientId));
        }
        
        Extension e = claim.getExtensionByUrl(EXT_ATTACHMENT_BINARY);
        if (e == null || !(e.getValue() instanceof StringType)) {
            return new byte[0];
        }
        
        String binId = ((StringType) e.getValue()).getValue();
        Binary b = fhirClientService.read(Binary.class, binId, getPracticeId());
        return b.getData();
    }

    /**
     * Upload claim EOB
     */
    public void uploadClaimEob(Long patientId, Long claimId, MultipartFile file) throws Exception {
        validatePatientExists(patientId);
        validateClaimExists(claimId);
        Claim claim = fhirClientService.read(Claim.class, claimId.toString(), getPracticeId());
        
        if (!patientId.equals(getPatientFromClaim(claim))) {
            throw new IllegalArgumentException(String.format(
                "Claim not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Claim ID are correct and that the claim belongs to this patient.",
                claimId, patientId));
        }
        
        if (file != null && !file.isEmpty()) {
            // Create a Binary resource to store the EOB file
            Binary binary = new Binary();
            binary.setContentType("application/pdf");
            binary.setData(file.getBytes());
            
            var outcome = fhirClientService.create(binary, getPracticeId());
            String binaryId = outcome.getId().getIdPart();
            
            // Store reference to binary in claim
            claim.getExtension().removeIf(e -> EXT_EOB_BINARY.equals(e.getUrl()));
            claim.addExtension(new Extension(EXT_EOB_BINARY, new StringType(binaryId)));
            
            // Mark EOB as attached
            claim.getExtension().removeIf(e -> EXT_EOB_ATTACHED.equals(e.getUrl()));
            claim.addExtension(new Extension(EXT_EOB_ATTACHED, new BooleanType(true)));
            
            fhirClientService.update(claim, getPracticeId());
        }
    }

    /**
     * Get claim EOB
     */
    public byte[] getClaimEob(Long patientId, Long claimId) {
        validatePatientExists(patientId);
        validateClaimExists(claimId);
        Claim claim = fhirClientService.read(Claim.class, claimId.toString(), getPracticeId());
        
        if (!patientId.equals(getPatientFromClaim(claim))) {
            throw new IllegalArgumentException(String.format(
                "Claim not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Claim ID are correct and that the claim belongs to this patient.",
                claimId, patientId));
        }
        
        Extension e = claim.getExtensionByUrl(EXT_EOB_BINARY);
        if (e == null || !(e.getValue() instanceof StringType)) {
            return new byte[0];
        }
        
        String binId = ((StringType) e.getValue()).getValue();
        Binary b = fhirClientService.read(Binary.class, binId, getPracticeId());
        return b.getData();
    }

    /**
     * Get EHR claim form data
     */
    public EhrClaimFormDataDto getEhrClaimFormData(Long patientId, Long claimId) {
        validatePatientExists(patientId);
        validateClaimExists(claimId);
        Claim claim = fhirClientService.read(Claim.class, claimId.toString(), getPracticeId());
        
        if (!patientId.equals(getPatientFromClaim(claim))) {
            throw new IllegalArgumentException(String.format(
                "Claim not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Claim ID are correct and that the claim belongs to this patient.",
                claimId, patientId));
        }
        
        return EhrClaimFormDataDto.builder()
                .claimId(claimId)
                .claimNumber(claimId.toString())
                .claimStatus(optStringExt(claim, EXT_STATUS))
                .claimDate(getCreatedDateFromClaim(claim))
                .insuranceInfo(null)
                .policyholderInfo(null)
                .patientInfo(null)
                .providerInfo(null)
                .serviceRecords(List.of())
                .financialSummary(null)
                .build();
    }

    // ===================== Helpers =====================

    /**
     * Get claim or throw exception if not found
     */
    private Claim getClaimOrThrow(Long patientId, Long invoiceId) {
        Bundle bundle = fhirClientService.search(Claim.class, getPracticeId());
        List<Claim> allClaims = fhirClientService.extractResources(bundle, Claim.class);
        
        log.info("Searching for claim: patientId={}, invoiceId={}, total claims={}", 
                patientId, invoiceId, allClaims.size());
        
        // Debug: log each claim's patient and invoice IDs
        for (Claim c : allClaims) {
            Long claimPatientId = getPatientFromClaim(c);
            Long claimInvoiceId = getInvoiceFromClaim(c);
            String claimId = c.getIdElement().getIdPart();
            log.info("Claim {}: patientId={}, invoiceId={}", claimId, claimPatientId, claimInvoiceId);
        }
        
        return allClaims.stream()
                .filter(c -> {
                    Long claimPatientId = getPatientFromClaim(c);
                    Long claimInvoiceId = getInvoiceFromClaim(c);
                    boolean matches = patientId.equals(claimPatientId) && invoiceId.equals(claimInvoiceId);
                    if (matches) {
                        log.info("Found matching claim: patientId={}, invoiceId={}", claimPatientId, claimInvoiceId);
                    }
                    return matches;
                })
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Claim not found for Invoice ID: {} and Patient ID: {}", invoiceId, patientId);
                    return new IllegalArgumentException(String.format(
                        "Claim not found for Invoice ID: %d and Patient ID: %d", invoiceId, patientId));
                });
    }

    /**
     * Extract patient ID from claim
     */
    private Long getPatientFromClaim(Claim c) {
        Extension ext = c.getExtensionByUrl(EXT_PATIENT);
        if (ext != null && ext.getValue() instanceof StringType) {
            try {
                return Long.parseLong(((StringType) ext.getValue()).getValue());
            } catch (NumberFormatException e) {
                log.warn("Invalid patient ID format in claim", e);
                return null;
            }
        }
        return null;
    }

    /**
     * Extract invoice ID from claim
     */
    private Long getInvoiceFromClaim(Claim c) {
        Extension ext = c.getExtensionByUrl(EXT_INVOICE);
        if (ext != null && ext.getValue() instanceof StringType) {
            try {
                return Long.parseLong(((StringType) ext.getValue()).getValue());
            } catch (NumberFormatException e) {
                log.warn("Invalid invoice ID format in claim", e);
                return null;
            }
        }
        return null;
    }

    /**
     * Convert FHIR Claim to DTO
     */
    private PatientClaimDto fromFhirClaim(Claim c) {
        String fhirId = c.getIdElement().getIdPart();
        Long patientId = getPatientFromClaim(c);
        Long invoiceId = getInvoiceFromClaim(c);
        String payer = optStringExt(c, EXT_PAYER);
        String treatingProvider = optStringExt(c, EXT_TREATING_PROVIDER);
        String billingEntity = optStringExt(c, EXT_BILLING_ENTITY);
        String type = optStringExt(c, EXT_CLAIM_TYPE);
        String notes = optStringExt(c, EXT_NOTES);
        String status = optStringExt(c, EXT_STATUS);
        int attachments = getIntExtension(c, EXT_ATTACHMENTS);
        boolean eob = getBooleanExtension(c, EXT_EOB_ATTACHED);
        LocalDate createdOn = getCreatedDateFromClaim(c);
        String patientName = getPatientNameById(patientId);
        String planName = optStringExt(c, EXT_PLAN_NAME);
        String provider = optStringExt(c, EXT_PROVIDER);
        String policyNumber = optStringExt(c, EXT_POLICY_NUMBER);
        boolean hasAttachment = hasAttachmentBinary(c);

        return new PatientClaimDto(
                Long.parseLong(fhirId),
                invoiceId,
                patientId,
                payer,
                treatingProvider,
                billingEntity,
                type,
                notes,
                status != null ? normalizeStatus(status) : PatientClaimStatus.DRAFT,
                attachments,
                eob,
                createdOn,
                hasAttachment,
                eob,
                patientName,
                planName,
                provider,
                policyNumber
        );
    }

    /**
     * Get created date from claim
     */
    private LocalDate getCreatedDateFromClaim(Claim c) {
        Extension createdExt = c.getExtensionByUrl(EXT_CREATED_ON);
        if (createdExt != null && createdExt.getValue() instanceof StringType) {
            try {
                String dateStr = ((StringType) createdExt.getValue()).getValue();
                return LocalDate.parse(dateStr);
            } catch (Exception e) {
                log.warn("Failed to parse created date from claim", e);
            }
        }
        return LocalDate.now();
    }

    /**
     * Check if claim has attachment binary
     */
    private boolean hasAttachmentBinary(Claim c) {
        Extension e = c.getExtensionByUrl(EXT_ATTACHMENT_BINARY);
        return e != null && e.getValue() instanceof StringType;
    }

    /**
     * Get optional string extension
     */
    private String optStringExt(Claim c, String url) {
        Extension ext = c.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    /**
     * Get integer extension
     */
    private int getIntExtension(Claim c, String url) {
        Extension ext = c.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof IntegerType) {
            return ((IntegerType) ext.getValue()).getValue();
        }
        return 0;
    }

    /**
     * Get boolean extension
     */
    private boolean getBooleanExtension(Claim c, String url) {
        Extension ext = c.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof BooleanType) {
            return ((BooleanType) ext.getValue()).booleanValue();
        }
        return false;
    }

    /**
     * Normalize status string to match enum format
     */
    private PatientClaimStatus normalizeStatus(String status) {
        if (status == null) return PatientClaimStatus.DRAFT;
        String normalized = status.replace("-", "_").toUpperCase();
        try {
            return PatientClaimStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown claim status: {}, defaulting to DRAFT", status);
            return PatientClaimStatus.DRAFT;
        }
    }

    /**
     * Fetch insurance email for a claim (FHIR-based)
     */
    public String getInsuranceEmailForClaim(Long claimId) {
        validateClaimExists(claimId);
        Claim claim = fhirClientService.read(Claim.class, claimId.toString(), getPracticeId());
        Long patientId = getPatientFromClaim(claim);
        if (patientId == null) return null;

        // Search for Coverage resources for this patient
        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Coverage.class)
                .where(new ca.uhn.fhir.rest.gclient.ReferenceClientParam("beneficiary").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Coverage) {
                    Coverage coverage = (Coverage) entry.getResource();
                    // Extract insurance company email from extension
                    String email = optStringExt(coverage, "http://ciyex.com/fhir/StructureDefinition/insurance-email");
                    if (email != null) return email;
                }
            }
        }
        return null;
    }

    /**
     * Send claim details to insurance email
     */
    public boolean sendClaimDetailsToInsuranceEmail(PatientClaimDto claim, String insuranceEmail) {
        log.info("Sending claim {} to insurance email {}", claim.id(), insuranceEmail);
        // TODO: Implement actual email sending logic
        return true;
    }

    /**
     * Get optional string extension from Coverage
     */
    private String optStringExt(Coverage c, String url) {
        Extension ext = c.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    /**
     * Get patient name by ID
     */
    private String getPatientNameById(Long patientId) {
        if (patientId == null) return null;
        try {
            Patient patient = fhirClientService.read(Patient.class, patientId.toString(), getPracticeId());
            if (patient.hasName()) {
                HumanName name = patient.getNameFirstRep();
                StringBuilder fullName = new StringBuilder();
                if (!name.getGiven().isEmpty()) {
                    fullName.append(name.getGiven().get(0).getValue());
                }
                if (name.hasFamily()) {
                    if (fullName.length() > 0) fullName.append(" ");
                    fullName.append(name.getFamily());
                }
                return fullName.toString();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch patient name for ID: {}", patientId, e);
        }
        return null;
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

    private void validateClaimExists(Long claimId) {
        if (claimId == null) throw new IllegalArgumentException("Claim ID cannot be null");
        try {
            fhirClientService.read(Claim.class, String.valueOf(claimId), getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Claim not found with ID: %d. Please provide a valid Claim ID.", claimId));
        }
    }
}
