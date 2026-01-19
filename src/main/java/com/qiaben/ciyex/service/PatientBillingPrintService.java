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
import java.util.*;
import java.util.stream.Collectors;

/**
 * FHIR-backed Patient Billing Print Service
 * Generates printable invoices and patient statements by aggregating data from FHIR-backed services
 * All business logic from original implementation preserved
 * No local database storage - all data sourced from FHIR via service layer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientBillingPrintService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final PatientInvoiceService invoiceService;
    private final PatientClaimService claimService;
    private final PatientInsurancePaymentService insurancePaymentService;
    private final PatientPaymentService paymentService;
    private final PatientDepositService depositService;
    private final PatientCreditService creditService;
    private final PatientBillingNoteService noteService;

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    /* ===================== Printable Invoice ===================== */

    /**
     * Generate a printable invoice for a specific invoice with complete transaction history.
     * Aggregates data from all billing services to build comprehensive statement.
     */
    public PatientInvoicePrintDto getPrintableInvoice(Long patientId, Long invoiceId) {
        log.debug("Generating printable invoice for patient {} invoice {}", patientId, invoiceId);

        // Validate patient and invoice exist
        Patient patient = fhirClientService.read(Patient.class, String.valueOf(patientId), getPracticeId());
        if (patient == null) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }

        PatientInvoiceDto invoice = invoiceService.getPatientInvoice(patientId, invoiceId);

        PatientInvoicePrintDto dto = new PatientInvoicePrintDto();

        // Practice Info - Get from organization context
        PatientInvoicePrintDto.PracticeInfo practice = new PatientInvoicePrintDto.PracticeInfo();
        practice.practiceName = "Practice Name"; // From PracticeContextService if available
        practice.address = "Practice Address";
        practice.phone = "Practice Phone";
        practice.email = "Practice Email";
        practice.website = "Practice Website";
        dto.practice = practice;

        // Patient Info
        dto.patientId = patientId;
        String firstName = patient.getNameFirstRep() != null ? patient.getNameFirstRep().getGivenAsSingleString() : "";
        String lastName = patient.getNameFirstRep() != null ? patient.getNameFirstRep().getFamily() : "";
        dto.patientName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
        
        if (patient.getTelecom() != null && !patient.getTelecom().isEmpty()) {
            for (ContactPoint cp : patient.getTelecom()) {
                if ("phone".equals(cp.getSystem().toCode())) {
                    dto.patientPhone = cp.getValue();
                } else if ("email".equals(cp.getSystem().toCode())) {
                    dto.patientEmail = cp.getValue();
                }
            }
        }

        if (patient.getAddress() != null && !patient.getAddress().isEmpty()) {
            Address addr = patient.getAddressFirstRep();
            StringBuilder addressBuilder = new StringBuilder();
            if (addr.getLine() != null && !addr.getLine().isEmpty()) {
                addressBuilder.append(addr.getLine().get(0).getValue());
            }
            if (addr.getCity() != null) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(addr.getCity());
            }
            if (addr.getState() != null) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(addr.getState());
            }
            if (addr.getPostalCode() != null) {
                if (addressBuilder.length() > 0) addressBuilder.append(" ");
                addressBuilder.append(addr.getPostalCode());
            }
            dto.patientAddress = addressBuilder.toString();
        }

        // Invoice Info
        dto.invoiceId = invoiceId;
        dto.invoiceDate = invoice.invoiceDate() != null ? invoice.invoiceDate() : LocalDateTime.now();
        dto.invoiceNumber = "Invoice #" + invoiceId;
        dto.status = invoice.status() != null ? invoice.status().toString() : "OPEN";

        // Build Transaction History (main statement table)
        List<PatientInvoicePrintDto.TransactionLine> transactions = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO;

        // 1. Add Invoice Header
        PatientInvoicePrintDto.TransactionLine invoiceHeader = new PatientInvoicePrintDto.TransactionLine();
        invoiceHeader.date = dto.invoiceDate.toLocalDate();
        invoiceHeader.description = "Invoice #" + invoiceId + ": $" + nz(invoice.totalCharge());
        invoiceHeader.transactionType = "INVOICE";
        runningBalance = runningBalance.add(nz(invoice.totalCharge()));
        invoiceHeader.balance = runningBalance;
        transactions.add(invoiceHeader);

        // 2. Add Invoice Lines (procedure details)
        if (invoice.lines() != null) {
            for (PatientInvoiceLineDto line : invoice.lines()) {
                PatientInvoicePrintDto.TransactionLine lineTransaction = new PatientInvoicePrintDto.TransactionLine();
                lineTransaction.date = invoiceHeader.date; // Use invoice date if DOS not available
                lineTransaction.code = line.code();
                lineTransaction.procedureDescription = line.treatment();
                lineTransaction.description = line.code() + " " + line.treatment();
                lineTransaction.provider = line.provider();
                lineTransaction.amount = line.charge();
                lineTransaction.transactionType = "INVOICE_LINE";
                lineTransaction.balance = runningBalance;
                transactions.add(lineTransaction);
            }
        }

        // 3. Add Claims
        List<PatientInvoicePrintDto.ClaimInfo> claimsInfo = new ArrayList<>();
        try {
            PatientClaimDto claim = claimService.getActiveClaimForInvoice(patientId, invoiceId);
            if (claim != null) {
                PatientInvoicePrintDto.TransactionLine claimTransaction = new PatientInvoicePrintDto.TransactionLine();
                claimTransaction.date = invoiceHeader.date;
                claimTransaction.description = "Claim #" + claim.id();
                claimTransaction.transactionType = "CLAIM";
                claimTransaction.balance = runningBalance;
                transactions.add(claimTransaction);

                // Add claim detail line
                PatientInvoicePrintDto.TransactionLine claimDetail = new PatientInvoicePrintDto.TransactionLine();
                claimDetail.date = claimTransaction.date;
                claimDetail.description = "Local " + claim.id() + " by " + nz(claim.payerName(), "INSURANCE");
                claimDetail.transactionType = "CLAIM";
                claimDetail.balance = runningBalance;
                transactions.add(claimDetail);

                // Store claim info
                PatientInvoicePrintDto.ClaimInfo claimInfo = new PatientInvoicePrintDto.ClaimInfo();
                claimInfo.claimId = claim.id();
                claimInfo.claimNumber = String.valueOf(claim.id());
                claimInfo.insuranceName = claim.payerName() != null ? claim.payerName() : "INSURANCE";
                claimInfo.localId = "Local " + claim.id();
                claimInfo.status = claim.status() != null ? claim.status().toString() : "";
                claimsInfo.add(claimInfo);
            }
        } catch (Exception e) {
            log.debug("No claim found for invoice {}: {}", invoiceId, e.getMessage());
        }
        dto.claims = claimsInfo;

        // 4. Add Insurance Payments with details
        List<PatientInvoicePrintDto.InsurancePaymentDetail> insurancePaymentDetails = new ArrayList<>();
        try {
            List<PatientInsuranceRemitLineDto> insurancePayments = insurancePaymentService.listInsurancePayments(patientId, invoiceId, null, null);
            if (insurancePayments != null) {
                for (PatientInsuranceRemitLineDto remit : insurancePayments) {
                    PatientInvoicePrintDto.TransactionLine insPaymentLine = new PatientInvoicePrintDto.TransactionLine();
                    insPaymentLine.date = invoiceHeader.date;
                    insPaymentLine.description = "Insurance Payment";
                    insPaymentLine.credit = remit.insPay();
                    insPaymentLine.transactionType = "INSURANCE_PAYMENT";
                    runningBalance = runningBalance.subtract(nz(remit.insPay()));
                    insPaymentLine.balance = runningBalance;
                    transactions.add(insPaymentLine);

                    // Store insurance payment detail
                    PatientInvoicePrintDto.InsurancePaymentDetail paymentDetail = new PatientInvoicePrintDto.InsurancePaymentDetail();
                    paymentDetail.paymentId = remit.id();
                    paymentDetail.paymentDate = insPaymentLine.date;
                    paymentDetail.description = "Insurance Payment";
                    paymentDetail.amount = remit.insPay();
                    paymentDetail.credit = remit.insPay();
                    insurancePaymentDetails.add(paymentDetail);
                }
            }
        } catch (Exception e) {
            log.debug("No insurance payments found for invoice {}: {}", invoiceId, e.getMessage());
        }
        dto.insurancePayments = insurancePaymentDetails;

        // 5. Add Patient Payments with details
        List<PatientInvoicePrintDto.PatientPaymentDetail> patientPaymentDetails = new ArrayList<>();
        Map<Long, PatientInvoicePrintDto.PatientPaymentDetail> paymentMap = new HashMap<>();
        try {
            List<com.qiaben.ciyex.dto.PatientPatientPaymentAllocationDto> payments = paymentService.getPatientPaymentsByInvoice(patientId, invoiceId);
            if (payments != null) {
                for (com.qiaben.ciyex.dto.PatientPatientPaymentAllocationDto alloc : payments) {
                    // Group allocations by payment ID
                    if (!paymentMap.containsKey(alloc.id())) {
                        PatientInvoicePrintDto.TransactionLine paymentLine = new PatientInvoicePrintDto.TransactionLine();
                        paymentLine.date = alloc.createdAt() != null ? alloc.createdAt().toLocalDate() : invoiceHeader.date;
                        paymentLine.description = "Patient Payment";
                        paymentLine.credit = alloc.amount();
                        paymentLine.transactionType = "PATIENT_PAYMENT";
                        runningBalance = runningBalance.subtract(nz(alloc.amount()));
                        paymentLine.balance = runningBalance;
                        transactions.add(paymentLine);

                        // Store patient payment detail
                        PatientInvoicePrintDto.PatientPaymentDetail paymentDetail = new PatientInvoicePrintDto.PatientPaymentDetail();
                        paymentDetail.paymentId = alloc.id();
                        paymentDetail.paymentDate = paymentLine.date;
                        paymentDetail.description = "Patient Payment";
                        paymentDetail.paymentMethod = alloc.paymentMethod() != null ? alloc.paymentMethod() : "UNKNOWN";
                        paymentDetail.amount = alloc.amount();
                        paymentDetail.credit = alloc.amount();
                        paymentMap.put(alloc.id(), paymentDetail);
                    } else {
                        // Add to existing payment total
                        PatientInvoicePrintDto.PatientPaymentDetail existing = paymentMap.get(alloc.id());
                        existing.amount = existing.amount.add(alloc.amount());
                        existing.credit = existing.credit.add(alloc.amount());
                    }
                }
                patientPaymentDetails.addAll(paymentMap.values());
            }
        } catch (Exception e) {
            log.debug("No patient payments found for invoice {}: {}", invoiceId, e.getMessage());
        }
        dto.patientPayments = patientPaymentDetails;

        // 6. Add Patient Deposits
        List<PatientInvoicePrintDto.PatientDepositDetail> depositDetails = new ArrayList<>();
        try {
            List<PatientDepositDto> deposits = depositService.getPatientDeposits(patientId);
            if (deposits != null) {
                for (PatientDepositDto deposit : deposits) {
                    PatientInvoicePrintDto.TransactionLine depositLine = new PatientInvoicePrintDto.TransactionLine();
                    depositLine.date = deposit.depositDate();
                    depositLine.description = "Patient Deposit" + (deposit.description() != null ? " - " + deposit.description() : "");
                    depositLine.credit = deposit.amount();
                    depositLine.transactionType = "PATIENT_DEPOSIT";
                    runningBalance = runningBalance.subtract(nz(deposit.amount()));
                    depositLine.balance = runningBalance;
                    transactions.add(depositLine);

                    // Store deposit detail
                    PatientInvoicePrintDto.PatientDepositDetail depositDetail = new PatientInvoicePrintDto.PatientDepositDetail();
                    depositDetail.depositId = deposit.id();
                    depositDetail.depositDate = deposit.depositDate();
                    depositDetail.description = deposit.description();
                    depositDetail.paymentMethod = deposit.paymentMethod();
                    depositDetail.amount = deposit.amount();
                    depositDetails.add(depositDetail);
                }
            }
        } catch (Exception e) {
            log.debug("No deposits found for patient {}: {}", patientId, e.getMessage());
        }
        dto.patientDeposits = depositDetails;

        // 7. Add Courtesy Credits
        List<PatientInvoicePrintDto.CourtesyCreditDetail> courtesyCreditDetails = new ArrayList<>();
        try {
            List<InvoiceCourtesyCreditDto> courtesyCredits = depositService.getInvoiceWithCourtesyCredit(patientId, invoiceId);
            if (courtesyCredits != null) {
                for (InvoiceCourtesyCreditDto credit : courtesyCredits) {
                    if (credit.isActive()) {
                        PatientInvoicePrintDto.TransactionLine creditLine = new PatientInvoicePrintDto.TransactionLine();
                        creditLine.date = credit.createdAt().toLocalDate();
                        creditLine.description = "Courtesy Credit" + (credit.adjustmentType() != null ? " - " + credit.adjustmentType() : "");
                        creditLine.credit = credit.amount();
                        creditLine.transactionType = "COURTESY_CREDIT";
                        runningBalance = runningBalance.subtract(nz(credit.amount()));
                        creditLine.balance = runningBalance;
                        transactions.add(creditLine);

                        // Store courtesy credit detail
                        PatientInvoicePrintDto.CourtesyCreditDetail creditDetail = new PatientInvoicePrintDto.CourtesyCreditDetail();
                        creditDetail.creditId = credit.id();
                        creditDetail.creditDate = creditLine.date;
                        creditDetail.adjustmentType = credit.adjustmentType();
                        creditDetail.description = credit.description();
                        creditDetail.amount = credit.amount();
                        courtesyCreditDetails.add(creditDetail);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("No courtesy credits found for invoice {}: {}", invoiceId, e.getMessage());
        }
        dto.courtesyCredits = courtesyCreditDetails;

        dto.transactions = transactions;

        // Financial Summary
        PatientInvoicePrintDto.FinancialSummary financialSummary = new PatientInvoicePrintDto.FinancialSummary();
        financialSummary.totalCharges = nz(invoice.totalCharge());

        // Sum all payments and deposits
        BigDecimal totalInsPaid = insurancePaymentDetails.stream()
                .map(p -> nz(p.amount))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPtPaid = patientPaymentDetails.stream()
                .map(p -> nz(p.amount))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDeposits = depositDetails.stream()
                .map(d -> nz(d.amount))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalPtPaid = totalPtPaid.add(totalDeposits);

        financialSummary.totalInsurancePayments = totalInsPaid;
        financialSummary.totalPatientPayments = totalPtPaid;

        // Sum adjustments (write-offs and courtesy credits)
        BigDecimal totalAdjustments = courtesyCreditDetails.stream()
                .map(c -> nz(c.amount))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        financialSummary.totalAdjustment = totalAdjustments.negate();

        financialSummary.outstandingBalance = nz(invoice.ptBalance());
        financialSummary.estimatedRemainingInsurance = BigDecimal.ZERO;
        financialSummary.estimatedRemainingInsuranceAdjustment = BigDecimal.ZERO;

        dto.financialSummary = financialSummary;

        // Aging Summary (Your Portion)
        PatientInvoicePrintDto.AgingSummary agingSummary = new PatientInvoicePrintDto.AgingSummary();
        agingSummary.balance0_30 = financialSummary.outstandingBalance;
        agingSummary.balance30_60 = BigDecimal.ZERO;
        agingSummary.balance60_90 = BigDecimal.ZERO;
        agingSummary.balance90plus = BigDecimal.ZERO;

        try {
            PatientAccountCreditDto accountCredit = creditService.getAccountCredit(patientId);
            agingSummary.accountCredit = nz(accountCredit.balance());
        } catch (Exception e) {
            log.debug("No account credit found for patient {}: {}", patientId, e.getMessage());
            agingSummary.accountCredit = BigDecimal.ZERO;
        }

        dto.agingSummary = agingSummary;

        // Appointment Information
        PatientInvoicePrintDto.AppointmentInfo appointments = new PatientInvoicePrintDto.AppointmentInfo();
        appointments.nextScheduledTreatment = "No Scheduled Appointment";
        appointments.nextScheduledHygiene = "No Scheduled Appointment";
        // Appointment fetching would be done via repository or service
        dto.appointments = appointments;

        // Notes - get billing notes for invoice
        try {
            List<PatientBillingNoteDto> notes = noteService.listInvoiceNotes(patientId, invoiceId);
            dto.notes = notes.stream()
                    .map(n -> n.text)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.debug("No notes found for invoice {}: {}", invoiceId, e.getMessage());
            dto.notes = new ArrayList<>();
        }

        log.info("Generated printable invoice for patient {} invoice {}", patientId, invoiceId);
        return dto;
    }

    /* ===================== Patient Statement ===================== */

    /**
     * Generate a printable patient statement with all invoices and transactions
     */
    public PatientStatementDto getPatientStatement(Long patientId) {
        log.debug("Generating patient statement for patient {}", patientId);

        // Validate patient exists
        Patient patient = fhirClientService.read(Patient.class, String.valueOf(patientId), getPracticeId());
        if (patient == null) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }

        PatientStatementDto dto = new PatientStatementDto();

        // Practice Info
        PatientStatementDto.PracticeInfo practice = new PatientStatementDto.PracticeInfo();
        practice.practiceName = "Practice Name";
        practice.address = "Practice Address";
        practice.phone = "Practice Phone";
        practice.email = "Practice Email";
        practice.website = "Practice Website";
        dto.practice = practice;

        // Patient Info
        dto.patientId = patientId;
        String firstName = patient.getNameFirstRep() != null ? patient.getNameFirstRep().getGivenAsSingleString() : "";
        String lastName = patient.getNameFirstRep() != null ? patient.getNameFirstRep().getFamily() : "";
        dto.patientName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
        
        if (patient.getTelecom() != null && !patient.getTelecom().isEmpty()) {
            for (ContactPoint cp : patient.getTelecom()) {
                if ("phone".equals(cp.getSystem().toCode())) {
                    dto.patientPhone = cp.getValue();
                } else if ("email".equals(cp.getSystem().toCode())) {
                    dto.patientEmail = cp.getValue();
                }
            }
        }

        if (patient.getAddress() != null && !patient.getAddress().isEmpty()) {
            Address addr = patient.getAddressFirstRep();
            StringBuilder addressBuilder = new StringBuilder();
            if (addr.getLine() != null && !addr.getLine().isEmpty()) {
                addressBuilder.append(addr.getLine().get(0).getValue());
            }
            if (addr.getCity() != null) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(addr.getCity());
            }
            if (addr.getState() != null) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(addr.getState());
            }
            if (addr.getPostalCode() != null) {
                if (addressBuilder.length() > 0) addressBuilder.append(" ");
                addressBuilder.append(addr.getPostalCode());
            }
            dto.patientAddress = addressBuilder.toString();
        }

        dto.statementDate = LocalDate.now().toString();

        // Statement lines - aggregate all invoices
        List<PatientStatementDto.StatementLine> lines = new ArrayList<>();
        try {
            List<PatientInvoiceDto> invoices = invoiceService.listInvoices(patientId);
            if (invoices != null) {
                for (PatientInvoiceDto inv : invoices) {
                    PatientStatementDto.StatementLine line = new PatientStatementDto.StatementLine();
                    line.date = inv.invoiceDate() != null ? inv.invoiceDate().toString() : "";
                    line.description = "Invoice #" + inv.id();
                    line.amount = inv.totalCharge();
                    line.balance = inv.ptBalance();
                    line.transactionType = "INVOICE";
                    lines.add(line);
                }
            }
        } catch (Exception e) {
            log.debug("Error retrieving invoices for patient {}: {}", patientId, e.getMessage());
        }
        dto.lines = lines;

        // Summary
        PatientStatementDto.Summary summary = new PatientStatementDto.Summary();
        summary.totalCharges = lines.stream()
                .map(l -> nz(l.amount))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.totalPatientPayments = BigDecimal.ZERO; // Would aggregate from payments
        summary.totalInsurancePayments = BigDecimal.ZERO; // Would aggregate from insurance payments
        summary.totalAdjustment = BigDecimal.ZERO; // Would aggregate from adjustments
        summary.outstandingBalance = lines.stream()
                .map(l -> nz(l.balance))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.summary = summary;

        // Your Portion (Aging)
        PatientStatementDto.YourPortion portion = new PatientStatementDto.YourPortion();
        portion.balance0_30 = summary.outstandingBalance;
        portion.balance30_60 = BigDecimal.ZERO;
        portion.balance60_90 = BigDecimal.ZERO;
        portion.balance90plus = BigDecimal.ZERO;
        try {
            PatientAccountCreditDto accountCredit = creditService.getAccountCredit(patientId);
            portion.accountCredit = nz(accountCredit.balance());
        } catch (Exception e) {
            portion.accountCredit = BigDecimal.ZERO;
        }
        dto.yourPortion = portion;

        // Insurance Summary
        PatientStatementDto.InsuranceSummary insuranceSummary = new PatientStatementDto.InsuranceSummary();
        insuranceSummary.totalInsurancePayments = BigDecimal.ZERO;
        insuranceSummary.totalInsuranceAdjustments = BigDecimal.ZERO;
        insuranceSummary.estimatedRemainingInsurance = BigDecimal.ZERO;
        dto.insurance = insuranceSummary;

        // Appointment Summary
        PatientStatementDto.AppointmentSummary apptSummary = new PatientStatementDto.AppointmentSummary();
        apptSummary.nextScheduledAppointment = "No Scheduled Appointment";
        apptSummary.nextScheduledHygieneAppointment = "No Scheduled Appointment";
        dto.appointments = apptSummary;

        // Notes
        try {
            List<PatientBillingNoteDto> allNotes = new ArrayList<>();
            // Would fetch all notes across all invoices
            dto.notes = allNotes.stream()
                    .map(n -> n.text)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.debug("Error retrieving notes for patient {}: {}", patientId, e.getMessage());
            dto.notes = new ArrayList<>();
        }

        log.info("Generated patient statement for patient {}", patientId);
        return dto;
    }

    /* ===================== Helpers ===================== */

    /**
     * Null-coalescing utility: returns value if not null, otherwise BigDecimal.ZERO
     */
    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * Null-coalescing utility for strings: returns value if not null, otherwise default
     */
    private String nz(String v, String defaultValue) {
        return v != null && !v.isEmpty() ? v : defaultValue;
    }
}
