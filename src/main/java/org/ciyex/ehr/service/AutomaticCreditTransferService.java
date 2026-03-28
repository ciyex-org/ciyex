package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.CreditTransferDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * FHIR-only Automatic Credit Transfer Service.
 * Uses FHIR Basic resource for patient account credits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomaticCreditTransferService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String CREDIT_TYPE_SYSTEM = "http://ciyex.com/fhir/resource-type";
    private static final String CREDIT_TYPE_CODE = "patient-account-credit";
    private static final String EXT_PATIENT_ID = "http://ciyex.com/fhir/StructureDefinition/patient-id";
    private static final String EXT_BALANCE = "http://ciyex.com/fhir/StructureDefinition/balance";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    /**
     * Automatically transfer overpayment to patient account credit
     * Called after insurance or patient payments are processed
     */
    public CreditTransferDto processAutomaticCreditTransfer(Long patientId, Long invoiceId, BigDecimal overpaymentAmount) {
        if (overpaymentAmount == null || overpaymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        log.info("Processing automatic credit transfer: patientId={}, invoiceId={}, amount={}", 
                patientId, invoiceId, overpaymentAmount);

        // Find or create patient account credit in FHIR
        BigDecimal currentBalance = getPatientCreditBalance(patientId);
        BigDecimal newBalance = currentBalance.add(overpaymentAmount);
        
        savePatientCreditBalance(patientId, newBalance);

        log.info("Automatic credit transfer completed: patientId={}, newCreditBalance={}", 
                patientId, newBalance);
        
        return CreditTransferDto.automatic(patientId, invoiceId, overpaymentAmount, newBalance);
    }

    /**
     * Check if invoice is fully paid and process any overpayment.
     * This method is called with pre-calculated overpayment amount.
     */
    public CreditTransferDto checkAndProcessOverpayment(Long patientId, Long invoiceId, BigDecimal overpaymentAmount) {
        if (overpaymentAmount == null || overpaymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        
        return processAutomaticCreditTransfer(patientId, invoiceId, overpaymentAmount);
    }

    // -------- FHIR Helper Methods --------

    private BigDecimal getPatientCreditBalance(Long patientId) {
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        List<Basic> credits = fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isPatientCredit)
                .filter(b -> patientId.toString().equals(getStringExt(b, EXT_PATIENT_ID)))
                .toList();

        if (credits.isEmpty()) {
            return BigDecimal.ZERO;
        }

        String balanceStr = getStringExt(credits.get(0), EXT_BALANCE);
        return balanceStr != null ? new BigDecimal(balanceStr) : BigDecimal.ZERO;
    }

    private void savePatientCreditBalance(Long patientId, BigDecimal balance) {
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        List<Basic> credits = fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isPatientCredit)
                .filter(b -> patientId.toString().equals(getStringExt(b, EXT_PATIENT_ID)))
                .toList();

        Basic basic;
        if (credits.isEmpty()) {
            // Create new
            basic = new Basic();
            CodeableConcept code = new CodeableConcept();
            code.addCoding().setSystem(CREDIT_TYPE_SYSTEM).setCode(CREDIT_TYPE_CODE).setDisplay("Patient Account Credit");
            basic.setCode(code);
            basic.addExtension(new Extension(EXT_PATIENT_ID, new StringType(patientId.toString())));
            basic.addExtension(new Extension(EXT_BALANCE, new StringType(balance.toString())));
            fhirClientService.create(basic, getPracticeId());
        } else {
            // Update existing
            basic = credits.get(0);
            basic.getExtension().removeIf(e -> EXT_BALANCE.equals(e.getUrl()));
            basic.addExtension(new Extension(EXT_BALANCE, new StringType(balance.toString())));
            fhirClientService.update(basic, getPracticeId());
        }
    }

    private boolean isPatientCredit(Basic basic) {
        if (!basic.hasCode()) return false;
        return basic.getCode().getCoding().stream()
                .anyMatch(c -> CREDIT_TYPE_SYSTEM.equals(c.getSystem()) && CREDIT_TYPE_CODE.equals(c.getCode()));
    }

    private String getStringExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }
}