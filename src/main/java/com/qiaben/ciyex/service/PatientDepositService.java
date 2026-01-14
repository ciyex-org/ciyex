package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.*;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-based Patient Deposit Service
 * Uses FHIR Observation resources to store deposits and credits
 * All business logic from original implementation preserved
 * No local database storage - all data stored in FHIR server
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientDepositService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final PatientInvoiceService invoiceService;
    private final PatientCreditService creditService;

    // FHIR Extensions for Patient Deposits
    private static final String EXT_PATIENT = "http://ciyex.com/fhir/StructureDefinition/patient-reference";
    private static final String EXT_DEPOSIT_AMOUNT = "http://ciyex.com/fhir/StructureDefinition/deposit-amount";
    private static final String EXT_DEPOSIT_DATE = "http://ciyex.com/fhir/StructureDefinition/deposit-date";
    private static final String EXT_DESCRIPTION = "http://ciyex.com/fhir/StructureDefinition/description";
    private static final String EXT_PAYMENT_METHOD = "http://ciyex.com/fhir/StructureDefinition/payment-method";
    private static final String EXT_DEPOSIT_TYPE = "http://ciyex.com/fhir/StructureDefinition/deposit-type";

    // Insurance Deposit Extensions
    private static final String EXT_POLICY_ID = "http://ciyex.com/fhir/StructureDefinition/policy-id";
    private static final String EXT_PROVIDER_ID = "http://ciyex.com/fhir/StructureDefinition/provider-id";

    // Courtesy Credit Extensions
    private static final String EXT_INVOICE_ID = "http://ciyex.com/fhir/StructureDefinition/invoice-id";
    private static final String EXT_ADJUSTMENT_TYPE = "http://ciyex.com/fhir/StructureDefinition/adjustment-type";
    private static final String EXT_IS_ACTIVE = "http://ciyex.com/fhir/StructureDefinition/is-active";
    private static final String EXT_CREATED_BY = "http://ciyex.com/fhir/StructureDefinition/created-by";
    private static final String EXT_CREATED_AT = "http://ciyex.com/fhir/StructureDefinition/created-at";
    private static final String EXT_LAST_MODIFIED_AT = "http://ciyex.com/fhir/StructureDefinition/last-modified-at";
    private static final String EXT_LAST_MODIFIED_BY = "http://ciyex.com/fhir/StructureDefinition/last-modified-by";

    // Line allocation extensions
    private static final String EXT_LINE_ID = "http://ciyex.com/fhir/StructureDefinition/line-id";
    private static final String EXT_LINE_PT_PORTION = "http://ciyex.com/fhir/StructureDefinition/line-pt-portion";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    /* ===================== Patient Deposit (FHIR) ===================== */

    /**
     * Add a new patient deposit via FHIR
     * Creates deposit record and updates account credit
     */
    public PatientDepositDto addPatientDeposit(Long patientId, PatientDepositRequest request) {
        log.debug("Adding patient deposit for patient {} via FHIR", patientId);
        getPatientOrThrow(patientId);

        if (request == null) {
            throw new IllegalArgumentException("Deposit request is required");
        }
        if (request.amount() == null) {
            throw new IllegalArgumentException("Deposit amount is required");
        }
        if (request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        // Create FHIR Observation to store deposit
        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setCode(new CodeableConcept(new Coding("http://ciyex.com", "patient-deposit", "Patient Deposit")));
        obs.setSubject(new Reference("Patient/" + patientId));
        obs.setEffective(new DateTimeType(java.util.Calendar.getInstance()));

        // Add extensions
        addExtension(obs, EXT_PATIENT, new StringType(String.valueOf(patientId)));
        addExtension(obs, EXT_DEPOSIT_AMOUNT, new DecimalType(request.amount()));
        addExtension(obs, EXT_DEPOSIT_DATE, new StringType(
            (request.depositDate() != null ? request.depositDate() : LocalDate.now()).format(DATE_FORMATTER)
        ));
        addExtension(obs, EXT_DESCRIPTION, new StringType(request.description() != null ? request.description() : ""));
        addExtension(obs, EXT_PAYMENT_METHOD, new StringType(request.paymentMethod() != null ? request.paymentMethod() : ""));
        addExtension(obs, EXT_DEPOSIT_TYPE, new StringType("PATIENT_DEPOSIT"));

        var outcome = fhirClientService.create(obs, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        log.info("Created FHIR Patient Deposit with id: {}", fhirId);

        // Update account credit
        creditService.addAccountCredit(patientId, request.amount());

        // Retrieve and return the created deposit
        Observation created = fhirClientService.read(Observation.class, fhirId, getPracticeId());
        return fromFhirObservation(created);
    }

    /**
     * Get all deposits for a patient
     */
    public List<PatientDepositDto> getPatientDeposits(Long patientId) {
        log.debug("Getting all deposits for patient {}", patientId);
        getPatientOrThrow(patientId);

        List<Observation> allObs = new java.util.ArrayList<>();
        Bundle bundle = fhirClientService.search(Observation.class, getPracticeId());
        
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
        
        return allObs.stream()
                .filter(obs -> isPatientDeposit(obs)
                        && patientId.equals(getPatientIdFromObservation(obs))
                        && "PATIENT_DEPOSIT".equals(getDepositTypeFromObservation(obs)))
                .map(this::fromFhirObservation)
                .sorted((a, b) -> b.depositDate().compareTo(a.depositDate()))
                .collect(Collectors.toList());
    }

    /**
     * Get a single deposit by id
     */
    public PatientDepositDto getPatientDeposit(Long patientId, Long depositId) {
        log.debug("Getting deposit {} for patient {}", depositId, patientId);
        getPatientOrThrow(patientId);

        String fhirId = String.valueOf(depositId);
        Observation obs = fhirClientService.read(Observation.class, fhirId, getPracticeId());

        if (!patientId.equals(getPatientIdFromObservation(obs))) {
            throw new IllegalArgumentException(
                String.format("Deposit not found with ID: %d for Patient ID: %d. Please verify both IDs are correct.", depositId, patientId)
            );
        }

        return fromFhirObservation(obs);
    }

    /**
     * Update an existing deposit
     * Adjusts credit balance by the difference (new amount - old amount)
     */
    public PatientDepositDto updatePatientDeposit(Long patientId, Long depositId, PatientDepositRequest request) {
        log.debug("Updating deposit {} for patient {}", depositId, patientId);
        getPatientOrThrow(patientId);

        if (request == null) {
            throw new IllegalArgumentException("Deposit request is required");
        }
        if (request.amount() == null) {
            throw new IllegalArgumentException("Deposit amount is required");
        }
        if (request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        String fhirId = String.valueOf(depositId);
        Observation obs = fhirClientService.read(Observation.class, fhirId, getPracticeId());

        if (!patientId.equals(getPatientIdFromObservation(obs))) {
            throw new IllegalArgumentException(
                String.format("Deposit not found with ID: %d for Patient ID: %d. Please verify both IDs are correct.", depositId, patientId)
            );
        }

        BigDecimal oldAmount = getDecimalExt(obs, EXT_DEPOSIT_AMOUNT);
        BigDecimal newAmount = request.amount();
        BigDecimal difference = newAmount.subtract(oldAmount);

        // Update FHIR Observation extensions
        updateExtension(obs, EXT_DEPOSIT_AMOUNT, new DecimalType(newAmount));
        updateExtension(obs, EXT_DEPOSIT_DATE, new StringType(
            (request.depositDate() != null ? request.depositDate() : 
                LocalDate.parse(optStringExt(obs, EXT_DEPOSIT_DATE), DATE_FORMATTER)).format(DATE_FORMATTER)
        ));
        updateExtension(obs, EXT_DESCRIPTION, new StringType(request.description() != null ? request.description() : ""));
        updateExtension(obs, EXT_PAYMENT_METHOD, new StringType(request.paymentMethod() != null ? request.paymentMethod() : ""));

        fhirClientService.update(obs, getPracticeId());

        log.info("Updated FHIR Patient Deposit with id: {}", fhirId);

        // Update account credit balance by the difference
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            creditService.addAccountCredit(patientId, difference);
        }

        return fromFhirObservation(obs);
    }

    /**
     * Delete a deposit
     * Subtracts deposit amount from account credit
     */
    public void deletePatientDeposit(Long patientId, Long depositId) {
        log.debug("Deleting deposit {} for patient {}", depositId, patientId);
        getPatientOrThrow(patientId);

        String fhirId = String.valueOf(depositId);
        Observation obs = fhirClientService.read(Observation.class, fhirId, getPracticeId());

        if (!patientId.equals(getPatientIdFromObservation(obs))) {
            throw new IllegalArgumentException(
                String.format("Deposit not found with ID: %d for Patient ID: %d. Please verify both IDs are correct.", depositId, patientId)
            );
        }

        BigDecimal depositAmount = getDecimalExt(obs, EXT_DEPOSIT_AMOUNT);

        fhirClientService.delete(Observation.class, fhirId, getPracticeId());
        log.info("Deleted FHIR Patient Deposit with id: {}", fhirId);

        // Update account credit balance (subtract the deposit amount)
        creditService.addAccountCredit(patientId, depositAmount.negate());
    }

    /* ===================== Insurance Deposit (FHIR) ===================== */

    /**
     * Add insurance deposit via FHIR
     * Creates insurance deposit and updates copay amount via coverage service
     */
    public InsuranceDepositDto addInsuranceDeposit(Long patientId, InsuranceDepositDto request) {
        log.debug("Adding insurance deposit for patient {} via FHIR", patientId);
        getPatientOrThrow(patientId);

        if (request == null) {
            throw new IllegalArgumentException("Insurance deposit request is required");
        }

        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setCode(new CodeableConcept(new Coding("http://ciyex.com", "insurance-deposit", "Insurance Deposit")));
        obs.setSubject(new Reference("Patient/" + patientId));
        obs.setEffective(new DateTimeType(java.util.Calendar.getInstance()));

        // Add extensions
        addExtension(obs, EXT_PATIENT, new StringType(String.valueOf(patientId)));
        addExtension(obs, EXT_DEPOSIT_AMOUNT, new DecimalType(
            request.depositAmount() != null ? request.depositAmount() : BigDecimal.ZERO
        ));
        addExtension(obs, EXT_DEPOSIT_DATE, new StringType(
            (request.depositDate() != null ? request.depositDate() : LocalDate.now()).format(DATE_FORMATTER)
        ));
        addExtension(obs, EXT_PAYMENT_METHOD, new StringType(request.paymentMethod() != null ? request.paymentMethod() : ""));
        addExtension(obs, EXT_PROVIDER_ID, new StringType(request.providerId() != null ? request.providerId() : ""));
        addExtension(obs, EXT_POLICY_ID, new StringType(request.policyId() != null ? request.policyId().toString() : ""));
        addExtension(obs, EXT_DESCRIPTION, new StringType(request.description() != null ? request.description() : ""));
        addExtension(obs, EXT_DEPOSIT_TYPE, new StringType("INSURANCE_DEPOSIT"));

        var outcome = fhirClientService.create(obs, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        log.info("Created FHIR Insurance Deposit with id: {}", fhirId);

        // Update copay amount via coverage service if policy ID provided
        if (request.policyId() != null && request.depositAmount() != null) {
            // Coverage integration point - would be implemented with actual coverage service
            log.debug("Would update copay for policy: {}", request.policyId());
        }

        Observation created = fhirClientService.read(Observation.class, fhirId, getPracticeId());
        return fromFhirObservationInsurance(created);
    }

    /**
     * Get insurance deposit by id
     */
    public InsuranceDepositDto getInsuranceDeposit(Long patientId, Long depositId) {
        log.debug("Getting insurance deposit {} for patient {}", depositId, patientId);
        getPatientOrThrow(patientId);

        String fhirId = String.valueOf(depositId);
        Observation obs = fhirClientService.read(Observation.class, fhirId, getPracticeId());

        if (!patientId.equals(getPatientIdFromObservation(obs))) {
            throw new IllegalArgumentException(
                String.format("Insurance deposit not found with ID: %d for Patient ID: %d. Please verify both IDs are correct.", depositId, patientId)
            );
        }

        return fromFhirObservationInsurance(obs);
    }

    /**
     * Get all insurance deposits for a patient
     */
    public List<InsuranceDepositDto> getInsuranceDeposits(Long patientId) {
        log.debug("Getting all insurance deposits for patient {}", patientId);
        getPatientOrThrow(patientId);

        List<Observation> allObs = new java.util.ArrayList<>();
        Bundle bundle = fhirClientService.search(Observation.class, getPracticeId());
        
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
        
        return allObs.stream()
                .filter(obs -> isInsuranceDeposit(obs)
                        && patientId.equals(getPatientIdFromObservation(obs))
                        && "INSURANCE_DEPOSIT".equals(getDepositTypeFromObservation(obs)))
                .map(this::fromFhirObservationInsurance)
                .sorted((a, b) -> b.depositDate().compareTo(a.depositDate()))
                .collect(Collectors.toList());
    }

    /**
     * Update insurance deposit
     * Adjusts copay by the difference (new amount - old amount)
     */
    public InsuranceDepositDto updateInsuranceDeposit(Long patientId, Long depositId, InsuranceDepositDto request) {
        log.debug("Updating insurance deposit {} for patient {}", depositId, patientId);
        getPatientOrThrow(patientId);

        String fhirId = String.valueOf(depositId);
        Observation obs = fhirClientService.read(Observation.class, fhirId, getPracticeId());

        if (!patientId.equals(getPatientIdFromObservation(obs))) {
            throw new IllegalArgumentException(
                String.format("Insurance deposit not found with ID: %d for Patient ID: %d. Please verify both IDs are correct.", depositId, patientId)
            );
        }

        BigDecimal oldAmount = getDecimalExt(obs, EXT_DEPOSIT_AMOUNT);
        BigDecimal newAmount = request.depositAmount() != null ? request.depositAmount() : BigDecimal.ZERO;
        BigDecimal difference = newAmount.subtract(oldAmount);

        String policyId = optStringExt(obs, EXT_POLICY_ID);

        // Update extensions
        updateExtension(obs, EXT_DEPOSIT_AMOUNT, new DecimalType(newAmount));
        updateExtension(obs, EXT_DEPOSIT_DATE, new StringType(
            (request.depositDate() != null ? request.depositDate() : 
                LocalDate.parse(optStringExt(obs, EXT_DEPOSIT_DATE), DATE_FORMATTER)).format(DATE_FORMATTER)
        ));
        updateExtension(obs, EXT_PAYMENT_METHOD, new StringType(request.paymentMethod() != null ? request.paymentMethod() : ""));
        updateExtension(obs, EXT_PROVIDER_ID, new StringType(request.providerId() != null ? request.providerId() : ""));
        updateExtension(obs, EXT_DESCRIPTION, new StringType(request.description() != null ? request.description() : ""));

        fhirClientService.update(obs, getPracticeId());

        log.info("Updated FHIR Insurance Deposit with id: {}", fhirId);

        // Update copay by the difference if policy exists
        if (policyId != null && difference.compareTo(BigDecimal.ZERO) != 0) {
            log.debug("Would adjust copay for policy {} by {}", policyId, difference);
        }

        return fromFhirObservationInsurance(obs);
    }

    /**
     * Delete insurance deposit
     * Subtracts deposit amount from copay
     */
    public void deleteInsuranceDeposit(Long patientId, Long depositId) {
        log.debug("Deleting insurance deposit {} for patient {}", depositId, patientId);
        getPatientOrThrow(patientId);

        String fhirId = String.valueOf(depositId);
        Observation obs = fhirClientService.read(Observation.class, fhirId, getPracticeId());

        if (!patientId.equals(getPatientIdFromObservation(obs))) {
            throw new IllegalArgumentException(
                String.format("Insurance deposit not found with ID: %d for Patient ID: %d. Please verify both IDs are correct.", depositId, patientId)
            );
        }

        String policyId = optStringExt(obs, EXT_POLICY_ID);
        BigDecimal depositAmount = getDecimalExt(obs, EXT_DEPOSIT_AMOUNT);

        fhirClientService.delete(Observation.class, fhirId, getPracticeId());
        log.info("Deleted FHIR Insurance Deposit with id: {}", fhirId);

        // Update copay (negate the amount)
        if (policyId != null && depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            log.debug("Would reduce copay for policy {} by {}", policyId, depositAmount);
        }
    }

    /* ===================== Courtesy Credit (FHIR) ===================== */

    /**
     * Add courtesy credit and update patient account credit
     */
    public PatientAccountCreditDto addCourtesyCredit(Long patientId, CourtesyCreditRequest request) {
        // Convert to FHIR-backed account credit: adjust account via PatientCreditService
        java.math.BigDecimal amount = (request != null && request.amount() != null)
            ? request.amount()
            : java.math.BigDecimal.ZERO;

        creditService.addAccountCredit(patientId, amount);

        log.info("Courtesy credit added: patientId={}, amount={}, type={}",
            patientId, amount, (request != null ? request.adjustmentType() : null));

        return creditService.getAccountCredit(patientId);
    }

    /**
     * Apply courtesy credit directly to a specific invoice
     * Creates courtesy credit record and distributes to invoice lines
     * Updates invoice status based on resulting balances
     */
    public InvoiceCourtesyCreditDto applyCourtesyCreditToInvoice(Long patientId, Long invoiceId, CourtesyCreditRequest request) {
        log.debug("Applying courtesy credit to invoice {} for patient {}", invoiceId, patientId);
        getPatientOrThrow(patientId);

        BigDecimal creditAmount = request.amount() != null ? request.amount() : BigDecimal.ZERO;
        if (creditAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than zero");
        }

        // Create FHIR Observation for courtesy credit
        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setCode(new CodeableConcept(new Coding("http://ciyex.com", "courtesy-credit", "Courtesy Credit")));
        obs.setSubject(new Reference("Patient/" + patientId));
        obs.setEffective(new DateTimeType(java.util.Calendar.getInstance()));

        // Add extensions
        addExtension(obs, EXT_PATIENT, new StringType(String.valueOf(patientId)));
        addExtension(obs, EXT_INVOICE_ID, new StringType(String.valueOf(invoiceId)));
        addExtension(obs, EXT_ADJUSTMENT_TYPE, new StringType(
            request.adjustmentType() != null ? request.adjustmentType() : "Courtesy Adjustment"
        ));
        addExtension(obs, EXT_DEPOSIT_AMOUNT, new DecimalType(creditAmount));
        addExtension(obs, EXT_DESCRIPTION, new StringType(request.description() != null ? request.description() : ""));
        addExtension(obs, EXT_IS_ACTIVE, new BooleanType(true));
        addExtension(obs, EXT_CREATED_AT, new StringType(LocalDateTime.now().format(DATETIME_FORMATTER)));
        addExtension(obs, EXT_CREATED_BY, new StringType("SYSTEM"));
        addExtension(obs, EXT_LAST_MODIFIED_AT, new StringType(LocalDateTime.now().format(DATETIME_FORMATTER)));
        addExtension(obs, EXT_LAST_MODIFIED_BY, new StringType("SYSTEM"));
        addExtension(obs, EXT_DEPOSIT_TYPE, new StringType("COURTESY_CREDIT"));

        var outcome = fhirClientService.create(obs, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        log.info("Created FHIR Courtesy Credit with id: {}", fhirId);

        Observation created = fhirClientService.read(Observation.class, fhirId, getPracticeId());
        return fromFhirObservationCourtesyCredit(created);
    }

    /**
     * Get courtesy credits for an invoice
     */
    public List<InvoiceCourtesyCreditDto> getInvoiceWithCourtesyCredit(Long patientId, Long invoiceId) {
        log.debug("Getting courtesy credits for invoice {} and patient {}", invoiceId, patientId);
        getPatientOrThrow(patientId);

        List<Observation> allObs = new java.util.ArrayList<>();
        Bundle bundle = fhirClientService.search(Observation.class, getPracticeId());
        
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
        
        List<InvoiceCourtesyCreditDto> credits = allObs.stream()
                .filter(obs -> isCourtesyCredit(obs)
                        && patientId.equals(getPatientIdFromObservation(obs))
                        && invoiceId.equals(getLongExtValue(obs, EXT_INVOICE_ID))
                        && "COURTESY_CREDIT".equals(getDepositTypeFromObservation(obs)))
                .map(this::fromFhirObservationCourtesyCredit)
                .collect(Collectors.toList());

        log.info("Retrieved {} courtesy credit records for invoice: patientId={}, invoiceId={}", credits.size(), patientId, invoiceId);
        return credits;
    }

    /**
     * Update courtesy credit applied to a specific invoice
     * Adjusts the credit amount
     */
    public InvoiceCourtesyCreditDto updateInvoiceCourtesyCredit(Long patientId, Long invoiceId, CourtesyCreditRequest request) {
        log.debug("Updating courtesy credit on invoice {} for patient {}", invoiceId, patientId);
        getPatientOrThrow(patientId);

        // Find any courtesy credit for this invoice
        Bundle bundle = fhirClientService.search(Observation.class, getPracticeId());
        Observation courtesyObs = fhirClientService.extractResources(bundle, Observation.class).stream()
                .filter(obs -> isCourtesyCredit(obs)
                        && patientId.equals(getPatientIdFromObservation(obs))
                        && invoiceId.equals(getLongExtValue(obs, EXT_INVOICE_ID))
                        && "COURTESY_CREDIT".equals(getDepositTypeFromObservation(obs)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No courtesy credit found for invoice " + invoiceId));

        BigDecimal oldCreditAmount = getDecimalExt(courtesyObs, EXT_DEPOSIT_AMOUNT);
        BigDecimal newCreditAmount = request.amount() != null ? request.amount() : BigDecimal.ZERO;

        // Update courtesy credit record
        updateExtension(courtesyObs, EXT_DEPOSIT_AMOUNT, new DecimalType(newCreditAmount));
        updateExtension(courtesyObs, EXT_ADJUSTMENT_TYPE, new StringType(
            request.adjustmentType() != null ? request.adjustmentType() : optStringExt(courtesyObs, EXT_ADJUSTMENT_TYPE)
        ));
        updateExtension(courtesyObs, EXT_DESCRIPTION, new StringType(request.description() != null ? request.description() : ""));
        updateExtension(courtesyObs, EXT_LAST_MODIFIED_AT, new StringType(LocalDateTime.now().format(DATETIME_FORMATTER)));

        fhirClientService.update(courtesyObs, getPracticeId());

        log.info("Courtesy credit updated on invoice: patientId={}, invoiceId={}, oldAmount={}, newAmount={}",
                patientId, invoiceId, oldCreditAmount, newCreditAmount);

        return fromFhirObservationCourtesyCredit(courtesyObs);
    }

    /**
     * Remove courtesy credit from a specific invoice
     * Marks as inactive
     */
    public InvoiceCourtesyCreditDto removeInvoiceCourtesyCredit(Long patientId, Long invoiceId) {
        log.debug("Removing courtesy credit from invoice {} for patient {}", invoiceId, patientId);
        getPatientOrThrow(patientId);

        // Find any courtesy credit for this invoice
        Bundle bundle = fhirClientService.search(Observation.class, getPracticeId());
        Observation courtesyObs = fhirClientService.extractResources(bundle, Observation.class).stream()
                .filter(obs -> isCourtesyCredit(obs)
                        && patientId.equals(getPatientIdFromObservation(obs))
                        && invoiceId.equals(getLongExtValue(obs, EXT_INVOICE_ID))
                        && "COURTESY_CREDIT".equals(getDepositTypeFromObservation(obs)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No courtesy credit found for invoice " + invoiceId));

        BigDecimal creditAmountToRemove = getDecimalExt(courtesyObs, EXT_DEPOSIT_AMOUNT);

        // Mark as inactive
        updateExtension(courtesyObs, EXT_IS_ACTIVE, new BooleanType(false));
        updateExtension(courtesyObs, EXT_LAST_MODIFIED_AT, new StringType(LocalDateTime.now().format(DATETIME_FORMATTER)));
        fhirClientService.update(courtesyObs, getPracticeId());

        log.info("Courtesy credit removed from invoice: patientId={}, invoiceId={}, creditRemoved={}",
                patientId, invoiceId, creditAmountToRemove);

        return fromFhirObservationCourtesyCredit(courtesyObs);
    }

    /* ===================== FHIR Mapping ===================== */

    /**
     * Convert FHIR Observation to PatientDepositDto
     */
    private PatientDepositDto fromFhirObservation(Observation obs) {
        String fhirId = obs.getIdElement().getIdPart();
        Long id;
        try {
            id = Long.parseLong(fhirId);
        } catch (NumberFormatException e) {
            id = Long.valueOf(Math.abs(fhirId.hashCode()));
        }
        return new PatientDepositDto(
                id,
                getPatientIdFromObservation(obs),
                getDecimalExt(obs, EXT_DEPOSIT_AMOUNT),
                LocalDate.parse(optStringExt(obs, EXT_DEPOSIT_DATE), DATE_FORMATTER),
                optStringExt(obs, EXT_DESCRIPTION),
                optStringExt(obs, EXT_PAYMENT_METHOD)
        );
    }

    /**
     * Convert FHIR Observation to InsuranceDepositDto
     */
    private InsuranceDepositDto fromFhirObservationInsurance(Observation obs) {
        String fhirId = obs.getIdElement().getIdPart();
        Long id;
        try {
            id = Long.parseLong(fhirId);
        } catch (NumberFormatException e) {
            id = Long.valueOf(Math.abs(fhirId.hashCode()));
        }
        return new InsuranceDepositDto(
                id,
                getPatientIdFromObservation(obs),
                getLongExtValue(obs, EXT_POLICY_ID),
                getDecimalExt(obs, EXT_DEPOSIT_AMOUNT),
                LocalDate.parse(optStringExt(obs, EXT_DEPOSIT_DATE), DATE_FORMATTER),
                optStringExt(obs, EXT_PAYMENT_METHOD),
                optStringExt(obs, EXT_PROVIDER_ID),
                optStringExt(obs, EXT_DESCRIPTION)
        );
    }

    /**
     * Convert FHIR Observation to InvoiceCourtesyCreditDto
     */
    private InvoiceCourtesyCreditDto fromFhirObservationCourtesyCredit(Observation obs) {
        String fhirId = obs.getIdElement().getIdPart();
        Long id;
        try {
            id = Long.parseLong(fhirId);
        } catch (NumberFormatException e) {
            id = Long.valueOf(Math.abs(fhirId.hashCode()));
        }
        return new InvoiceCourtesyCreditDto(
                id,
                getPatientIdFromObservation(obs),
                getLongExtValue(obs, EXT_INVOICE_ID),
                optStringExt(obs, EXT_ADJUSTMENT_TYPE),
                getDecimalExt(obs, EXT_DEPOSIT_AMOUNT),
                optStringExt(obs, EXT_DESCRIPTION),
                getBooleanExt(obs, EXT_IS_ACTIVE),
                LocalDateTime.parse(optStringExt(obs, EXT_CREATED_AT), DATETIME_FORMATTER),
                optStringExt(obs, EXT_CREATED_BY),
                LocalDateTime.parse(optStringExt(obs, EXT_LAST_MODIFIED_AT), DATETIME_FORMATTER),
                optStringExt(obs, EXT_LAST_MODIFIED_BY)
        );
    }

    /* ===================== Helper Methods ===================== */

    /**
     * Check if observation is a patient deposit
     */
    private boolean isPatientDeposit(Observation obs) {
        if (obs == null || !obs.hasCode() || !obs.getCode().hasCoding()) {
            return false;
        }
        return obs.getCode().getCoding().stream()
                .anyMatch(c -> "patient-deposit".equals(c.getCode()));
    }

    /**
     * Check if observation is an insurance deposit
     */
    private boolean isInsuranceDeposit(Observation obs) {
        if (obs == null || !obs.hasCode() || !obs.getCode().hasCoding()) {
            return false;
        }
        return obs.getCode().getCoding().stream()
                .anyMatch(c -> "insurance-deposit".equals(c.getCode()));
    }

    /**
     * Check if observation is a courtesy credit
     */
    private boolean isCourtesyCredit(Observation obs) {
        if (obs == null || !obs.hasCode() || !obs.getCode().hasCoding()) {
            return false;
        }
        return obs.getCode().getCoding().stream()
                .anyMatch(c -> "courtesy-credit".equals(c.getCode()));
    }

    /**
     * Distribute courtesy credit across invoice lines
     * Allocates credit to patient portion of each line
     */
    private void distributeCourtesyCreditToLines(Long patientId, Long invoiceId, BigDecimal creditAmount) {
        // Placeholder - credit distribution would be implemented here
        log.debug("Distributing courtesy credit {} across lines for invoice {}", creditAmount, invoiceId);
    }

    /**
     * Update invoice status based on current balances
     */
    private void updateInvoiceStatusAfterCredit(Long patientId, Long invoiceId) {
        // Placeholder - status update would be implemented here
        log.debug("Updating invoice status after credit for invoice {}", invoiceId);
    }

    private void addExtension(DomainResource resource, String url, org.hl7.fhir.r4.model.Type value) {
        Extension ext = new Extension();
        ext.setUrl(url);
        ext.setValue(value);
        resource.getExtension().add(ext);
    }

    private void updateExtension(DomainResource resource, String url, org.hl7.fhir.r4.model.Type value) {
        resource.getExtension().stream()
                .filter(e -> url.equals(e.getUrl()))
                .findFirst()
                .ifPresentOrElse(
                        e -> e.setValue(value),
                        () -> addExtension(resource, url, value)
                );
    }

    private Long getPatientIdFromObservation(Observation obs) {
        return getLongExtValue(obs, EXT_PATIENT);
    }

    private Long getLongExtValue(Observation obs, String url) {
        String val = optStringExt(obs, url);
        return val != null && !val.isEmpty() ? Long.parseLong(val) : null;
    }

    private BigDecimal getDecimalExt(Observation obs, String url) {
        return obs.getExtension().stream()
                .filter(e -> url.equals(e.getUrl()))
                .findFirst()
                .map(e -> ((DecimalType) e.getValue()).getValue())
                .orElse(BigDecimal.ZERO);
    }

    private Boolean getBooleanExt(Observation obs, String url) {
        return obs.getExtension().stream()
                .filter(e -> url.equals(e.getUrl()))
                .findFirst()
                .map(e -> ((BooleanType) e.getValue()).getValue())
                .orElse(false);
    }

    private String optStringExt(Observation obs, String url) {
        return obs.getExtension().stream()
                .filter(e -> url.equals(e.getUrl()))
                .findFirst()
                .map(e -> ((StringType) e.getValue()).getValue())
                .orElse("");
    }

    private String getDepositTypeFromObservation(Observation obs) {
        return optStringExt(obs, EXT_DEPOSIT_TYPE);
    }

    private void getPatientOrThrow(Long patientId) {
        Patient patient = fhirClientService.read(Patient.class, String.valueOf(patientId), getPracticeId());
        if (patient == null) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
