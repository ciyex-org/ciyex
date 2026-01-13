package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.*;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PatientCreditService: FHIR-backed service for managing patient account credits and adjustments.
 * 
 * Stores patient account credits as extensions on Patient resources.
 * Provides credit adjustment details and transfer of credit information.
 * 
 * Extension URLs:
 * - account-credit-balance: Patient account credit balance (on Patient resource)
 * - invoice-reference: Invoice ID (on Observation resources)
 * - remit-allowed: Allowed amount from insurance EOB
 * - remit-ins-pay: Insurance payment from EOB
 * - remit-ins-writeoff: Insurance write-off from EOB
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientCreditService {
    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // FHIR Extension URLs
    private static final String EXT_PATIENT = "http://ciyex.com/fhir/StructureDefinition/patient-reference";
    private static final String EXT_INVOICE = "http://ciyex.com/fhir/StructureDefinition/invoice-reference";
    private static final String EXT_ACCOUNT_CREDIT_BALANCE = "http://ciyex.com/fhir/StructureDefinition/account-credit-balance";
    private static final String EXT_REMIT_ALLOWED = "http://ciyex.com/fhir/StructureDefinition/remit-allowed";
    private static final String EXT_REMIT_INS_PAY = "http://ciyex.com/fhir/StructureDefinition/remit-ins-pay";
    private static final String EXT_REMIT_INS_WRITEOFF = "http://ciyex.com/fhir/StructureDefinition/remit-ins-writeoff";
    private static final String EXT_LINE_ID = "http://ciyex.com/fhir/StructureDefinition/line-id";

    /* ====== Request DTOs ====== */
    public record ApplyCreditRequest(BigDecimal amount) {}
    public record CourtesyCreditRequest(
            BigDecimal amount,
            String adjustmentType,
            String description,
            Boolean closeInvoice
    ) {}
    public record AccountAdjustmentRequest(
            String adjustmentType,
            BigDecimal flatRate,
            BigDecimal specificAmount,
            String description,
            Boolean includeCourtesyCredit
    ) {}

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    /* ===================== Account Credit (FHIR-backed) ===================== */

    /**
     * Get or create patient account credit balance.
     */
    public PatientAccountCreditDto getAccountCredit(Long patientId) {
        try {
            Patient patient = fhirClientService.read(Patient.class, String.valueOf(patientId), getPracticeId());
            Extension ext = patient.getExtensionByUrl(EXT_ACCOUNT_CREDIT_BALANCE);
            BigDecimal balance = BigDecimal.ZERO;
            if (ext != null && ext.getValue() instanceof DecimalType dt) {
                balance = dt.getValue();
            }
            return new PatientAccountCreditDto(patientId, balance);
        } catch (Exception e) {
            // Patient doesn't exist or no credit - create with zero balance
            return new PatientAccountCreditDto(patientId, BigDecimal.ZERO);
        }
    }

    /**
     * Add (or subtract) amount to patient account credit. Positive to add, negative to subtract.
     */
    public void addAccountCredit(Long patientId, BigDecimal amount) {
        if (amount == null) return;
        PatientAccountCreditDto current = getAccountCredit(patientId);
        BigDecimal newBalance = nz(current.balance()).add(amount);
        setAccountCredit(patientId, newBalance);
    }

    /**
     * Apply (use) account credit - reduces patient's credit balance.
     * Requires patient to have existing credit with sufficient balance.
     */
    public PatientAccountCreditDto applyAccountCredit(Long patientId, ApplyCreditRequest req) {
        BigDecimal amount = (req == null) ? BigDecimal.ZERO : nz(req.amount());

        // Get current credit balance
        PatientAccountCreditDto current = getAccountCredit(patientId);
        BigDecimal currentBalance = current.balance();

        // If amount is zero or negative, return current balance unchanged
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return new PatientAccountCreditDto(patientId, currentBalance);
        }

        // Verify sufficient credit exists
        if (currentBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                String.format("Insufficient credit. Available: %s, Requested: %s", currentBalance, amount)
            );
        }

        // Update patient credit
        BigDecimal newBalance = currentBalance.subtract(amount);
        setAccountCredit(patientId, newBalance);

        return new PatientAccountCreditDto(patientId, newBalance);
    }

    /* ===================== Credit Adjustment & Transfer of Credit (FHIR-backed) ===================== */

    /**
     * Get credit adjustment details for an invoice.
     * Includes totals, remit line details, and line-by-line breakdown.
     */
    public CreditAdjustmentDetailDto getCreditAdjustment(Long patientId, Long invoiceId) {
        // Verify invoice exists and belongs to patient
        Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(invoice, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Invoice ID: %d does not belong to Patient ID: %d", invoiceId, patientId)
            );
        }

        // Fetch all remit records for this invoice
        Bundle bundle = fhirClientService.search(Observation.class, getPracticeId());
        List<Observation> remitLines = fhirClientService.extractResources(bundle, Observation.class).stream()
                .filter(o -> "remit".equals(getCodeFromObservation(o)))
                .filter(o -> String.valueOf(invoiceId).equals(optStringExt(o, EXT_INVOICE)))
                .collect(Collectors.toList());

        // Calculate totals
        BigDecimal totalInvoiceAmount = nz(getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/total-charge"));
        BigDecimal totalInsuranceAllowed = remitLines.stream()
                .map(o -> nz(getDecimalExt(o, EXT_REMIT_ALLOWED)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal creditAdjustmentWriteOff = totalInvoiceAmount.subtract(totalInsuranceAllowed);

        BigDecimal insurancePayment = remitLines.stream()
                .map(o -> nz(getDecimalExt(o, EXT_REMIT_INS_PAY)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ptBalance = nz(getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/pt-balance"));
        BigDecimal insBalance = nz(getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/ins-balance"));
        BigDecimal creditAdjustmentAmount = ptBalance.add(insBalance);

        // Extract line details from invoice components
        List<CreditAdjustmentDetailDto.LineDetail> lines = invoice.getComponent().stream()
                .map(comp -> {
                    String code = getComponentString(comp, "http://ciyex.com/fhir/StructureDefinition/line-code");
                    String treatment = getComponentString(comp, "http://ciyex.com/fhir/StructureDefinition/line-treatment");
                    String provider = getComponentString(comp, "http://ciyex.com/fhir/StructureDefinition/line-provider");
                    BigDecimal insWriteOff = nz(getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-ins-writeoff"));
                    BigDecimal ptPortion = nz(getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-pt-portion"));
                    BigDecimal insPortion = nz(getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-ins-portion"));
                    BigDecimal charge = nz(getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-charge"));

                    return new CreditAdjustmentDetailDto.LineDetail(
                            code,
                            treatment,
                            provider,
                            insWriteOff,
                            ptPortion,
                            insPortion,
                            charge,
                            insWriteOff
                    );
                })
                .collect(Collectors.toList());

        return new CreditAdjustmentDetailDto(
                invoiceId,
                LocalDate.now(),
                "Write Off $" + creditAdjustmentWriteOff,
                insurancePayment,
                creditAdjustmentWriteOff,
                ptBalance,
                insBalance,
                totalInvoiceAmount,
                creditAdjustmentAmount,
                lines
        );
    }

    /**
     * Get transfer of credit details for an invoice.
     * Shows invoice totals and line-by-line breakdown for credit allocation.
     */
    public TransferOfCreditDetailDto getTransferOfCredit(Long patientId, Long invoiceId) {
        // Verify invoice exists and belongs to patient
        Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(invoice, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Invoice ID: %d does not belong to Patient ID: %d", invoiceId, patientId)
            );
        }

        BigDecimal totalCredit = nz(getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/total-charge"));

        // Extract line details from invoice components
        List<TransferOfCreditDetailDto.LineDetail> lines = invoice.getComponent().stream()
                .map(comp -> {
                    String code = getComponentString(comp, "http://ciyex.com/fhir/StructureDefinition/line-code");
                    String treatment = getComponentString(comp, "http://ciyex.com/fhir/StructureDefinition/line-treatment");
                    BigDecimal insWriteOff = nz(getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-ins-writeoff"));
                    BigDecimal ptPortion = nz(getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-pt-portion"));
                    BigDecimal insPortion = nz(getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-ins-portion"));
                    BigDecimal charge = nz(getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-charge"));

                    return new TransferOfCreditDetailDto.LineDetail(
                            code,
                            treatment,
                            insWriteOff,
                            ptPortion,
                            insPortion,
                            charge
                    );
                })
                .collect(Collectors.toList());

        return new TransferOfCreditDetailDto(
                invoiceId,
                LocalDate.now(),
                "Transfer of credits",
                totalCredit,
                lines
        );
    }

    /* ===================== Helpers ===================== */

    /**
     * Null-coalescing: return zero if value is null.
     */
    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * Set account credit balance for patient.
     */
    private void setAccountCredit(Long patientId, BigDecimal amount) {
        try {
            Patient patient = fhirClientService.read(Patient.class, String.valueOf(patientId), getPracticeId());
            patient.getExtension().removeIf(e -> EXT_ACCOUNT_CREDIT_BALANCE.equals(e.getUrl()));
            patient.addExtension(new Extension(EXT_ACCOUNT_CREDIT_BALANCE, new DecimalType(nz(amount))));
            fhirClientService.update(patient, getPracticeId());
        } catch (Exception e) {
            log.warn("Unable to set account credit for patient {}: {}", patientId, e.getMessage());
        }
    }

    /**
     * Get decimal value from observation extension.
     */
    private BigDecimal getDecimalExt(Observation obs, String url) {
        if (obs == null) return null;
        Extension ext = obs.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof DecimalType dt) {
            return dt.getValue();
        }
        return null;
    }

    /**
     * Get decimal value from observation-level extension.
     */
    private BigDecimal getInvoiceExtDecimal(Observation inv, String url) {
        if (inv == null) return BigDecimal.ZERO;
        Extension ext = inv.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof DecimalType dt) {
            return dt.getValue();
        }
        return BigDecimal.ZERO;
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
     * Get code from observation.
     */
    private String getCodeFromObservation(Observation obs) {
        if (obs == null || obs.getCode() == null || !obs.getCode().hasCoding()) {
            return null;
        }
        return obs.getCode().getCodingFirstRep().getCode();
    }
}
