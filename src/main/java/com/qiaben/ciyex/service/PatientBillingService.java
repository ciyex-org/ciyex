package com.qiaben.ciyex.service;
import com.qiaben.ciyex.dto.*;
import com.qiaben.ciyex.entity.*;
import com.qiaben.ciyex.entity.PatientBillingNote;
import com.qiaben.ciyex.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PatientBillingService {

    /**
     * Generate a printable patient statement for the print/statement API.
     */
    public PatientStatementDto getPatientStatement(Long patientId) {
        PatientStatementDto dto = new PatientStatementDto();
        // Patient info (replace with actual lookup if PatientRepository is available)
        dto.patientId = patientId;
        dto.patientName = "[Patient Name]";
        dto.statementDate = java.time.LocalDate.now().toString();


        // Statement lines (invoices, claims, payments, adjustments)
        List<PatientStatementDto.StatementLine> lines = new ArrayList<>();
        List<PatientInvoice> invoices = invoiceRepo.findByPatientIdOrderByIdDesc(patientId);
        for (PatientInvoice inv : invoices) {
            PatientStatementDto.StatementLine line = new PatientStatementDto.StatementLine();
            line.date = inv.getCreatedAt() != null ? inv.getCreatedAt().toString() : "";
            line.description = "Invoice #" + inv.getId() + (inv.getDescription() != null ? (": " + inv.getDescription()) : "");
            line.provider = inv.getProviderName();
            line.amount = inv.getTotalCharge();
            line.credit = null;
            line.balance = inv.getPtBalance();
            lines.add(line);
            // TODO: Add claims, payments, adjustments, insurance lines as needed
        }
        dto.lines = lines;

        // Summary
        PatientStatementDto.Summary summary = new PatientStatementDto.Summary();
        summary.totalCharges = invoices.stream().map(PatientInvoice::getTotalCharge).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.totalPatientPayments = BigDecimal.ZERO; // TODO: sum patient payments
        summary.totalInsurancePayments = BigDecimal.ZERO; // TODO: sum insurance payments
        summary.totalAdjustment = BigDecimal.ZERO; // TODO: sum adjustments
        summary.outstandingBalance = invoices.stream().map(PatientInvoice::getPtBalance).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.summary = summary;

        // Your Portion
        PatientStatementDto.YourPortion portion = new PatientStatementDto.YourPortion();
        portion.balance0_30 = summary.outstandingBalance; // TODO: split by aging
        portion.balance30_60 = BigDecimal.ZERO;
        portion.balance60_90 = BigDecimal.ZERO;
        portion.balance90plus = BigDecimal.ZERO;
        portion.accountCredit = BigDecimal.ZERO; // TODO: get from account credit repo
        dto.yourPortion = portion;

        // Appointments
        PatientStatementDto.AppointmentSummary appt = new PatientStatementDto.AppointmentSummary();

        dto.appointments = appt;

        // Notes
        List<PatientBillingNote> notes = noteRepo.findByPatientId(patientId);
        dto.notes = notes.stream().map(PatientBillingNote::getText).toList();

        return dto;
    }

    /** Transfer INS balance to PT balance */
    public PatientInvoiceDto transferOutstandingToPatient(Long patientId, Long invoiceId, Double amount) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        if (invoice == null || amount == null || amount <= 0) return toInvoiceDto(invoice);
        BigDecimal amt = BigDecimal.valueOf(amount);
        BigDecimal insBal = invoice.getInsBalance() != null ? invoice.getInsBalance() : BigDecimal.ZERO;
        BigDecimal ptBal = invoice.getPtBalance() != null ? invoice.getPtBalance() : BigDecimal.ZERO;
        if (insBal.compareTo(amt) < 0) amt = insBal;
        invoice.setInsBalance(insBal.subtract(amt));
        invoice.setPtBalance(ptBal.add(amt));
        invoiceRepo.save(invoice);
        return toInvoiceDto(invoice);
    }

    /** Transfer PT balance to INS balance */
    public PatientInvoiceDto transferOutstandingToInsurance(Long patientId, Long invoiceId, Double amount) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        if (invoice == null || amount == null || amount <= 0) return toInvoiceDto(invoice);
        BigDecimal amt = BigDecimal.valueOf(amount);
        BigDecimal ptBal = invoice.getPtBalance() != null ? invoice.getPtBalance() : BigDecimal.ZERO;
        BigDecimal insBal = invoice.getInsBalance() != null ? invoice.getInsBalance() : BigDecimal.ZERO;
        if (ptBal.compareTo(amt) < 0) amt = ptBal;
        invoice.setPtBalance(ptBal.subtract(amt));
        invoice.setInsBalance(insBal.add(amt));
        invoiceRepo.save(invoice);
        return toInvoiceDto(invoice);
    }

    private final PatientInvoiceRepository invoiceRepo;
    private final PatientInvoiceLineRepository lineRepo;
    private final PatientClaimRepository claimRepo;
    private final PatientInsuranceRemitLineRepository remitRepo;
    private final PatientAccountCreditRepository creditRepo;
    private final PatientDepositRepository depositRepo;

    private final PatientPaymentAllocationRepository allocationRepo;
    private final PatientPaymentRepository paymentRepo;
    private final PatientInvoiceLineRepository invoiceLineRepo;
    private final PatientBillingNoteRepository noteRepo;
    private final PatientRepository patientRepo;
    private final CoverageRepository coverageRepo;
    private final InvoiceCourtesyCreditRepository invoiceCourtesyCreditRepo;
    private final AppointmentRepository appointmentRepo;
    private final FacilityRepository facilityRepo;



    /* ====== Request DTOs ====== */
    public record CreateInvoiceRequest(String code, String description, String provider, String dos, BigDecimal rate) {}
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
    public record AccountAdjustmentRequest(String adjustmentType, BigDecimal flatRate, BigDecimal specificAmount, String description, Boolean includeCourtesyCredit) {
        public BigDecimal flatRate() { return flatRate; }
        public BigDecimal specificAmount() { return specificAmount; }
        public Boolean includeCourtesyCredit() { return includeCourtesyCredit; }
    }

    /* ===================== Invoices ===================== */

    /** Backdate invoice date */
    public PatientInvoiceDto backdateInvoice(Long patientId, Long invoiceId, BackdateRequest req) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        if (req != null && req.date() != null) {
            invoice.setBackdate(LocalDate.parse(req.date()));
            invoiceRepo.save(invoice);
        }
        return toInvoiceDto(invoice);
    }

    public PatientAccountCreditDto accountAdjustment(Long patientId, AccountAdjustmentRequest req) {
        if (req == null || req.adjustmentType() == null) {
            throw new IllegalArgumentException("Adjustment type is required");
        }

        PatientAccountCredit credit = creditRepo.findByPatientId(patientId)
                .orElseGet(() -> {
                    PatientAccountCredit c = new PatientAccountCredit();
                    c.setPatientId(patientId);
                    c.setBalance(BigDecimal.ZERO);
                    return c;
                });

        BigDecimal adjustmentAmount = BigDecimal.ZERO;

        switch (req.adjustmentType()) {
            case "Flat-rate" -> adjustmentAmount = nz(req.flatRate());
            case "Total Outstanding" -> {
                // Calculate total outstanding from all patient invoices
                List<PatientInvoice> invoices = invoiceRepo.findByPatientIdOrderByIdDesc(patientId);
                BigDecimal totalOutstanding = invoices.stream()
                        .map(inv -> nz(inv.getPtBalance()).add(nz(inv.getInsBalance())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                adjustmentAmount = totalOutstanding;
            }
            case "Patient Outstanding" -> {
                // Calculate only patient portion outstanding
                List<PatientInvoice> invoices = invoiceRepo.findByPatientIdOrderByIdDesc(patientId);
                BigDecimal patientOutstanding = invoices.stream()
                        .map(inv -> nz(inv.getPtBalance()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                adjustmentAmount = patientOutstanding;
            }
            case "Specific" -> adjustmentAmount = nz(req.specificAmount());
            default -> throw new IllegalArgumentException("Invalid adjustment type: " + req.adjustmentType());
        }

        // Apply courtesy credit if checkbox is selected
        if (Boolean.TRUE.equals(req.includeCourtesyCredit())) {
            log.info("Courtesy credit included in adjustment");
        }

        // Update credit balance
        credit.setBalance(nz(credit.getBalance()).add(adjustmentAmount));
        creditRepo.save(credit);

        log.info("Account adjustment applied: type={}, amount={}, patientId={}",
                req.adjustmentType(), adjustmentAmount, patientId);

        return new PatientAccountCreditDto(credit.getPatientId(), credit.getBalance());
    }



    public PatientInvoiceDto adjustInvoice(Long patientId, Long invoiceId, InvoiceAdjustmentRequest req) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);

        if (req == null || req.adjustmentType() == null) {
            throw new IllegalArgumentException("Adjustment type is required");
        }

        // Apply percentage discount if provided
        if (req.percentageDiscount() != null && req.percentageDiscount() > 0) {
            int percent = req.percentageDiscount();
            BigDecimal discountFactor = BigDecimal.valueOf(percent)
                    .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

            for (PatientInvoiceLine line : invoice.getLines()) {
                BigDecimal originalCharge = line.getCharge();
                BigDecimal discount = originalCharge.multiply(discountFactor);
                BigDecimal newCharge = originalCharge.subtract(discount).max(BigDecimal.ZERO);

                line.setCharge(newCharge);
                line.setAllowed(newCharge);

                // Recalculate portions proportionally
                BigDecimal totalBefore = nz(line.getInsPortion()).add(nz(line.getPatientPortion()));
                if (totalBefore.signum() > 0) {
                    BigDecimal factor = newCharge.divide(totalBefore, 8, RoundingMode.HALF_UP);
                    line.setInsPortion(nz(line.getInsPortion()).multiply(factor));
                    line.setPatientPortion(nz(line.getPatientPortion()).multiply(factor));
                } else {
                    // Default: all to insurance
                    line.setInsPortion(newCharge);
                    line.setPatientPortion(BigDecimal.ZERO);
                }

                line.setInsWriteOff(BigDecimal.ZERO);
                lineRepo.save(line);
            }
        }

        // Apply adjustment amount if provided
        if (req.adjustmentAmount() != null && req.adjustmentAmount().signum() != 0) {
            // Create credit adjustment for the patient
            addCredit(patientId, req.adjustmentAmount());
        }

        // Recalculate invoice totals
        invoice.recalcTotals();
        invoiceRepo.save(invoice);

        log.info("Invoice adjusted: invoiceId={}, type={}, discount={}%, amount={}",
                invoiceId, req.adjustmentType(), req.percentageDiscount(), req.adjustmentAmount());

        return toInvoiceDto(invoice);
    }

    public List<PatientInvoiceDto> listInvoices(Long patientId) {
        return invoiceRepo.findByPatientIdOrderByIdDesc(patientId)
                .stream().map(this::toInvoiceDto).toList();
    }

    public List<PatientInvoiceLineDto> getInvoiceLines(Long patientId, Long invoiceId) {
        // Verify the invoice belongs to the patient
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);

        // Return the invoice lines
        return invoice.getLines().stream()
                .map(this::toInvoiceLineDto)
                .toList();
    }

    public PatientInvoiceDto createInvoiceFromProcedure(Long patientId, CreateInvoiceRequest b) {
        if (b == null) throw new IllegalArgumentException("Body required");

        PatientInvoice invoice = new PatientInvoice();
        invoice.setPatientId(patientId);
        invoice.setStatus(PatientInvoice.Status.OPEN);



        PatientInvoiceLine line = new PatientInvoiceLine();
        line.setInvoice(invoice);
        line.setCode(b.code());
        line.setTreatment(b.description());
        line.setProvider(b.provider());
        line.setDos(LocalDate.parse(b.dos()));
        line.setCharge(nz(b.rate()));
        line.setAllowed(nz(b.rate()));
        // seed: all initially to insurance; patient = 0
        line.setInsPortion(nz(b.rate()));
        line.setPatientPortion(BigDecimal.ZERO);

        invoice.getLines().add(line);
        invoice.recalcTotals();
        invoiceRepo.saveAndFlush(invoice);

        // seed a draft claim
        PatientClaim claim = new PatientClaim();
        claim.setPatientId(patientId);
        claim.setInvoiceId(invoice.getId());
        claim.setStatus(PatientClaim.Status.DRAFT);
        claim.setCreatedOn(LocalDate.parse(b.dos()));
        claim.setType("Electronic"); // Always set type to Electronic
        // Set patient name
        patientRepo.findById(patientId).ifPresent(patient -> {
            String fullName = patient.getFirstName() + (patient.getMiddleName() != null ? " " + patient.getMiddleName() : "") + " " + patient.getLastName();
            claim.setPatientName(fullName.trim());
        });
        claimRepo.save(claim);

        return toInvoiceDto(invoice);
    }

    public void deleteInvoice(Long patientId, Long invoiceId) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);

        // Delete associated claim if exists
        PatientClaim claim = claimRepo.findByInvoiceId(invoiceId);
        if (claim != null) {
            claimRepo.delete(claim);
        }

        // Delete invoice (cascade will handle invoice lines)
        invoiceRepo.delete(invoice);
    }

    public PatientInvoiceDto updateInvoiceFromProcedure(Long patientId, Long invoiceId, UpdateInvoiceRequest b) {
        if (b == null) throw new IllegalArgumentException("Body required");

        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);

        // Update the first line (assuming single line invoice from procedure)
        if (invoice.getLines().isEmpty()) {
            throw new IllegalArgumentException("Invoice has no lines to update");
        }

        PatientInvoiceLine line = invoice.getLines().get(0);

        // Update line fields
        if (b.code() != null) line.setCode(b.code());
        if (b.description() != null) line.setTreatment(b.description());
        if (b.provider() != null) line.setProvider(b.provider());
        if (b.dos() != null) line.setDos(LocalDate.parse(b.dos()));

        if (b.rate() != null) {
            BigDecimal rate = nz(b.rate());
            line.setCharge(rate);
            line.setAllowed(rate);
            line.setInsPortion(rate);
            line.setPatientPortion(BigDecimal.ZERO);
        }

        invoice.recalcTotals();
        invoiceRepo.saveAndFlush(invoice);

        // Update associated claim if exists
        PatientClaim claim = claimRepo.findByInvoiceId(invoiceId);
        if (claim != null && b.dos() != null) {
            claim.setCreatedOn(LocalDate.parse(b.dos()));
            claimRepo.save(claim);
        }

        return toInvoiceDto(invoice);
    }

    public PatientInvoiceDto updateInvoiceLineAmount(Long patientId, Long invoiceId, Long lineId, UpdateLineAmountRequest b) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        PatientInvoiceLine line = lineRepo.findById(lineId).orElseThrow();
        if (!line.getInvoice().getId().equals(invoiceId)) throw new IllegalArgumentException("Line not in invoice");

        BigDecimal amt = nz(b == null ? null : b.newCharge());
        line.setCharge(amt);
        line.setAllowed(amt);
        line.setInsWriteOff(BigDecimal.ZERO);
        line.setInsPortion(amt);
        line.setPatientPortion(BigDecimal.ZERO);

        invoice.recalcTotals();
        return toInvoiceDto(invoice);
    }

    public PatientInvoiceDto applyInvoicePercentageAdjustment(Long patientId, Long invoiceId, PercentageAdjustmentRequest b) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        int percent = (b == null) ? 0 : b.percent();
        BigDecimal p = BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

        for (PatientInvoiceLine l : invoice.getLines()) {
            BigDecimal delta = l.getCharge().multiply(p);
            BigDecimal newCharge = l.getCharge().subtract(delta).max(BigDecimal.ZERO);
            l.setCharge(newCharge);
            l.setAllowed(newCharge);
            // keep proportions simple
            BigDecimal totalBefore = nz(l.getInsPortion()).add(nz(l.getPatientPortion()));
            if (totalBefore.signum() == 0) {
                l.setInsPortion(BigDecimal.ZERO);
                l.setPatientPortion(BigDecimal.ZERO);
            } else {
                // scale down proportionally
                BigDecimal factor = newCharge.divide(nz(totalBefore), 8, RoundingMode.HALF_UP);
                l.setInsPortion(nz(l.getInsPortion()).multiply(factor));
                l.setPatientPortion(nz(l.getPatientPortion()).multiply(factor));
            }
            l.setInsWriteOff(BigDecimal.ZERO);
        }
        invoice.recalcTotals();
        return toInvoiceDto(invoice);
    }

    /* ===================== Claims ===================== */

    /** Fetch all claims for all patients in the org (for All Claims view) */
    public List<PatientClaimDto> listAllClaims() {
        List<PatientClaim> claims;
        try {
            claims = claimRepo.findAllByOrderByIdDesc();
        } catch (NoSuchMethodError | RuntimeException e) {
            claims = claimRepo.findAll();
        }
        return claims.stream().map(this::toClaimDto).toList();
    }

    public List<PatientClaimDto> listAllClaimsForPatient(Long patientId) {
        List<PatientClaim> claims;
        try {
            claims = claimRepo.findAllByPatientIdOrderByIdDesc(patientId);
        } catch (NoSuchMethodError | RuntimeException e) {
            claims = claimRepo.findAllByPatientId(patientId);
        }
        return claims.stream().map(this::toClaimDto).toList();
    }

    public PatientClaimDto getActiveClaimForInvoice(Long patientId, Long invoiceId) {
        return toClaimDto(getClaimOrThrow(patientId, invoiceId));
    }

    public List<PatientClaimDto> listClaimsForInvoice(Long patientId, Long invoiceId) {
        List<PatientClaim> claims;
        try {
            claims = claimRepo.findAllByInvoiceIdAndPatientIdOrderByIdDesc(invoiceId, patientId);
        } catch (NoSuchMethodError | RuntimeException e) {
            claims = claimRepo.findAllByInvoiceIdAndPatientIdOrderByIdDesc(invoiceId, patientId);
        }
        return claims.stream().map(this::toClaimDto).toList();
    }

    public PatientClaimDto promoteClaim(Long patientId, Long invoiceId) {
        PatientClaim c = getClaimOrThrow(patientId, invoiceId);
        if (c.getStatus() == PatientClaim.Status.DRAFT) {
            c.setStatus(PatientClaim.Status.IN_PROCESS);

            // Fetch coverage data for the patient and populate claim fields
            List<Coverage> coverages = coverageRepo.findByPatientIdOrderByEffectiveDateDesc(patientId);
            if (!coverages.isEmpty()) {
                Coverage coverage = coverages.get(0); // Get the most recent coverage
                c.setPlanName(coverage.getPlanName());
                c.setProvider(coverage.getProvider());
                c.setPolicyNumber(coverage.getPolicyNumber());
            }

            claimRepo.save(c);
        }
        return toClaimDto(c);
    }

    public PatientClaimDto sendClaimToBatch(Long patientId, Long invoiceId) {
        PatientClaim c = getClaimOrThrow(patientId, invoiceId);
        c.setStatus(PatientClaim.Status.READY_FOR_SUBMISSION);
        return toClaimDto(c);
    }

    public PatientClaimDto submitClaim(Long patientId, Long invoiceId) {
        PatientClaim c = getClaimOrThrow(patientId, invoiceId);
        c.setStatus(PatientClaim.Status.SUBMITTED);
        return toClaimDto(c);
    }

    public PatientClaimDto closeClaim(Long patientId, Long invoiceId) {
        PatientClaim c = getClaimOrThrow(patientId, invoiceId);
        c.setStatus(PatientClaim.Status.CLOSED);
        return toClaimDto(c);
    }

    public PatientClaimDto voidAndRecreateClaim(Long patientId, Long invoiceId) {
        PatientClaim existing = getClaimOrThrow(patientId, invoiceId);
        existing.setStatus(PatientClaim.Status.VOID);

        PatientClaim fresh = new PatientClaim();
        fresh.setPatientId(patientId);
        fresh.setInvoiceId(invoiceId);
        fresh.setStatus(PatientClaim.Status.DRAFT);
        fresh.setCreatedOn(LocalDate.now());
        claimRepo.save(fresh);

        return toClaimDto(fresh);
    }

    /**
     * Void and recreate claim by claim ID (for All Claims view)
     * The existing claim is DELETED from the database (void = delete)
     * A new claim is created with DRAFT status for the same invoice
     */
    public PatientClaimDto voidAndRecreateClaimById(Long claimId) {
        PatientClaim existing = claimRepo.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + claimId));

        // Store patient and invoice IDs before deleting
        Long patientId = existing.getPatientId();
        Long invoiceId = existing.getInvoiceId();

        // Delete the existing claim from database (void = delete)
        claimRepo.delete(existing);

        // Create new claim with same patient and invoice
        PatientClaim fresh = new PatientClaim();
        fresh.setPatientId(patientId);
        fresh.setInvoiceId(invoiceId);
        fresh.setStatus(PatientClaim.Status.DRAFT);
        fresh.setCreatedOn(LocalDate.now());
        claimRepo.save(fresh);

        return toClaimDto(fresh);
    }



    public PatientClaimDto updateClaim(Long patientId, Long invoiceId, PatientClaimCoreUpdate p) {
        PatientClaim c = getClaimOrThrow(patientId, invoiceId);
        if (p != null) {
            c.setTreatingProviderId(p.treatingProviderId());
            c.setBillingEntity(p.billingEntity());
            c.setType(p.type());
            c.setNotes(p.notes());
            c.setAttachmentIndicator(p.attachmentIndicator());
            c.setAttachmentType(p.attachmentType());
            c.setAttachmentTransmissionCode(p.attachmentTransmissionCode());
            c.setClaimSubmissionReasonCode(p.claimSubmissionReasonCode());
        }
        return toClaimDto(c);
    }

    /**
     * Convert claim type (manual/electronic)
     */
    public PatientClaimDto convertClaimType(Long claimId, String targetType) {
        // Fetch claim entity
        PatientClaim claim = claimRepo.findById(claimId).orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
        // Update claim type
        claim.setType(targetType); // Assumes claim has a setType(String) method
        claimRepo.save(claim);
        // Return updated DTO
        return getClaimDtoById(claimId);
    }

    /**
     * Get claim line details (DOS, code, description, provider, total submitted amount)
     * Fetches invoice lines associated with the claim
     */
    public List<ClaimLineDetailDto> getClaimLineDetails(Long claimId) {
        // Get the claim
        PatientClaim claim = claimRepo.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));

        // Get the invoice associated with the claim
        Long invoiceId = claim.getInvoiceId();
        if (invoiceId == null) {
            return List.of();
        }

        // Fetch all invoice lines for this invoice
        List<PatientInvoiceLine> lines = lineRepo.findByInvoiceId(invoiceId);

        // Convert to ClaimLineDetailDto
        return lines.stream()
                .map(line -> new ClaimLineDetailDto(
                        line.getId(),
                        line.getDos(),
                        line.getCode(),
                        line.getTreatment(), // This is the description
                        line.getProvider(),
                        line.getCharge() // This is the total submitted amount
                ))
                .toList();
    }


    /* ================ Insurance Payment ================ */

    public PatientInvoiceDto applyInsurancePayment(Long patientId, Long invoiceId, PatientInsurancePaymentRequestDto req) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);

        if (req != null && req.lines() != null) {
            for (PatientInsuranceRemitLineDto r : req.lines()) {
                // 1) persist remit row
                PatientInsuranceRemitLine e = new PatientInsuranceRemitLine();
                e.setPatientId(patientId);
                e.setInvoiceId(invoiceId);
                e.setInvoiceLineId(r.invoiceLineId());
                e.setSubmitted(nz(r.submitted()));
                e.setBalance(nz(r.balance()));
                e.setDeductible(nz(r.deductible()));
                e.setAllowed(nz(r.allowed()));
                e.setInsWriteOff(nz(r.insWriteOff()));
                e.setInsPay(nz(r.insPay()));
                e.setUpdateAllowed(r.updateAllowed());
                e.setUpdateFlatPortion(r.updateFlatPortion());
                e.setApplyWriteoff(r.applyWriteoff());
                remitRepo.save(e);

                // 2) recompute affected invoice line
                PatientInvoiceLine line = lineRepo.findById(r.invoiceLineId()).orElseThrow();
                if (!line.getInvoice().getId().equals(invoiceId)) throw new IllegalArgumentException("Line not in invoice");

                BigDecimal submitted = nz(r.submitted());
                BigDecimal allowed   = nz(r.allowed());
                BigDecimal insPay    = nz(r.insPay());

                BigDecimal insWO  = submitted.subtract(allowed); if (insWO.signum() < 0) insWO = BigDecimal.ZERO;
                BigDecimal ptResp = allowed.subtract(insPay);    if (ptResp.signum() < 0) ptResp = BigDecimal.ZERO;

                line.setCharge(submitted);
                line.setAllowed(allowed);
                line.setInsWriteOff(insWO);
                // insurer still owes ptResp? no, ptResp is patient, so insPortion becomes 0
                line.setInsPortion(BigDecimal.ZERO);
                line.setPatientPortion(ptResp);
                lineRepo.save(line);
            }
        }

        invoice.recalcTotals();
        // advance claim state
        claimRepo.findByInvoiceIdAndPatientId(invoiceId, patientId)
                .ifPresent(c -> c.setStatus(
                        invoice.getPtBalance().compareTo(BigDecimal.ZERO) == 0
                                ? PatientClaim.Status.ACCEPTED
                                : PatientClaim.Status.IN_PROCESS));

        return toInvoiceDto(invoice);
    }

    public List<PatientInsuranceRemitLineDto> listInsurancePayments(
            Long patientId, Long invoiceId, Long claimId, Long insuranceId) {

        if (invoiceId == null && claimId != null) {
            try {
                PatientClaim c = claimRepo.findById(claimId).orElse(null);
                if (c != null) invoiceId = c.getInvoiceId();
            } catch (RuntimeException ex) {
                log.warn("Unable to resolve claimId {} to invoiceId: {}", claimId, ex.getMessage());
            }
        }

        List<PatientInsuranceRemitLine> rows;
        try {
            rows = (invoiceId != null)
                    ? remitRepo.findAllByPatientIdAndInvoiceIdOrderByIdDesc(patientId, invoiceId)
                    : remitRepo.findAllByPatientIdOrderByIdDesc(patientId);
        } catch (RuntimeException e) {
            rows = remitRepo.findAllByPatientId(patientId);
        }

        return rows.stream().map(this::toRemitDto).toList();
    }

    /** EDIT */
    public PatientInvoiceDto editInsuranceRemitLine(Long patientId, Long invoiceId, Long remitId, PatientInsuranceRemitLineDto dto) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        PatientInsuranceRemitLine remit = remitRepo.findById(remitId)
                .orElseThrow(() -> new IllegalArgumentException("Insurance remit not found"));

        if (dto.submitted() != null)    remit.setSubmitted(dto.submitted());
        if (dto.balance() != null)      remit.setBalance(dto.balance());
        if (dto.deductible() != null)   remit.setDeductible(dto.deductible());
        if (dto.allowed() != null)      remit.setAllowed(dto.allowed());
        if (dto.insWriteOff() != null)  remit.setInsWriteOff(dto.insWriteOff());
        if (dto.insPay() != null)       remit.setInsPay(dto.insPay());
        if (dto.updateAllowed() != null)        remit.setUpdateAllowed(dto.updateAllowed());
        if (dto.updateFlatPortion() != null)    remit.setUpdateFlatPortion(dto.updateFlatPortion());
        if (dto.applyWriteoff() != null)        remit.setApplyWriteoff(dto.applyWriteoff());
        remitRepo.save(remit);

        if (dto.invoiceLineId() != null) {
            PatientInvoiceLine line = lineRepo.findById(dto.invoiceLineId())
                    .orElseThrow(() -> new IllegalArgumentException("Invoice line not found"));
            if (!line.getInvoice().getId().equals(invoiceId)) throw new IllegalArgumentException("Line not in invoice");

            BigDecimal submitted = nz(dto.submitted());
            BigDecimal allowed   = nz(dto.allowed());
            BigDecimal insPay    = nz(dto.insPay());

            BigDecimal insWO  = submitted.subtract(allowed); if (insWO.signum() < 0) insWO = BigDecimal.ZERO;
            BigDecimal ptResp = allowed.subtract(insPay);    if (ptResp.signum() < 0) ptResp = BigDecimal.ZERO;

            line.setCharge(submitted);
            line.setAllowed(allowed);
            line.setInsWriteOff(insWO);
            line.setInsPortion(BigDecimal.ZERO);
            line.setPatientPortion(ptResp);
            lineRepo.save(line);
        }

        invoice.recalcTotals();
        return toInvoiceDto(invoice);
    }

    /** VOID = hard delete the remit row */
    public PatientInvoiceDto voidInsurancePayment(Long patientId, Long invoiceId, Long remitId, VoidReason reason) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        remitRepo.deleteById(remitId);
        invoice.recalcTotals(); // recompute totals from remaining lines + their portions
        return toInvoiceDto(invoice);
    }

    /** REFUND insurance → increase insurance balance (reduce paid) */
    public PatientInvoiceDto refundInsurancePayment(Long patientId, Long invoiceId, Long remitId, RefundRequest req) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        BigDecimal amount = Optional.ofNullable(req).map(RefundRequest::amount)
                .orElseThrow(() -> new IllegalArgumentException("Refund amount required"));
        if (amount.signum() <= 0) throw new IllegalArgumentException("Refund amount must be > 0");

        // pick a line (prefer first) and push refund to insurance portion
        if (invoice.getLines() == null || invoice.getLines().isEmpty()) {
            throw new IllegalStateException("No invoice lines to apply insurance refund");
        }
        PatientInvoiceLine line = invoice.getLines().get(0);

        // Move amount to insurance portion (insurer still owes more)
        line.setInsPortion(nz(line.getInsPortion()).add(amount));
        lineRepo.save(line);

        // Optional: record an audit remit with negative insPay
        PatientInsuranceRemitLine refundRow = new PatientInsuranceRemitLine();
        refundRow.setPatientId(patientId);
        refundRow.setInvoiceId(invoiceId);
        refundRow.setInvoiceLineId(line.getId());
        refundRow.setInsPay(amount.negate());
        remitRepo.save(refundRow);

        invoice.recalcTotals();
        return toInvoiceDto(invoice);
    }

    /** TRANSFER insurance balance → patient account credit */
    public PatientInvoiceDto transferInsuranceCreditToPatient(Long patientId, Long invoiceId, Long remitId, TransferCreditRequest req) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        BigDecimal amount = Optional.ofNullable(req).map(TransferCreditRequest::amount)
                .orElseThrow(() -> new IllegalArgumentException("Transfer amount required"));
        if (amount.signum() <= 0) throw new IllegalArgumentException("Transfer amount must be > 0");

        if (invoice.getLines() == null || invoice.getLines().isEmpty()) {
            throw new IllegalStateException("No invoice lines to transfer from");
        }

        // Calculate total insurance balance across all lines
        BigDecimal totalInsPortion = invoice.getLines().stream()
                .map(line -> nz(line.getInsPortion()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Validate that transfer amount doesn't exceed total insurance balance
        if (amount.compareTo(totalInsPortion) > 0) {
            throw new IllegalArgumentException("Transfer exceeds insurance balance. Requested: " + amount + ", Available: " + totalInsPortion);
        }

        // Distribute the transfer across lines proportionally
        BigDecimal remaining = amount;
        PatientInvoiceLine firstLineWithBalance = null;

        for (PatientInvoiceLine line : invoice.getLines()) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal lineInsPortion = nz(line.getInsPortion());
            if (lineInsPortion.compareTo(BigDecimal.ZERO) > 0 && firstLineWithBalance == null) {
                firstLineWithBalance = line;
            }

            BigDecimal toTransfer = lineInsPortion.min(remaining);
            if (toTransfer.compareTo(BigDecimal.ZERO) > 0) {
                line.setInsPortion(lineInsPortion.subtract(toTransfer));
                remaining = remaining.subtract(toTransfer);
                lineRepo.save(line);
            }
        }

        // credit patient account
        addCredit(patientId, amount);

        // audit: create remit row with negative insPay to mirror the transfer out of invoice
        // Use the first line that had a balance for reference
        PatientInsuranceRemitLine adj = new PatientInsuranceRemitLine();
        adj.setPatientId(patientId);
        adj.setInvoiceId(invoiceId);
        adj.setInvoiceLineId(firstLineWithBalance != null ? firstLineWithBalance.getId() : invoice.getLines().get(0).getId());
        adj.setInsPay(amount.negate());
        remitRepo.save(adj);

        invoice.recalcTotals();
        return toInvoiceDto(invoice);
    }

    /** Get detailed insurance payment information */
    public InsurancePaymentDetailDto getInsurancePaymentDetails(Long patientId, Long invoiceId, Long remitId) {
        // Fetch the remit line
        PatientInsuranceRemitLine remitLine = remitRepo.findById(remitId)
                .orElseThrow(() -> new RuntimeException("Insurance payment not found: " + remitId));
        
        // Fetch the invoice
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        
        // Build line details
        List<InsurancePaymentDetailDto.InsurancePaymentLineDetailDto> lineDetails = invoice.getLines().stream()
                .map(line -> {
                    // Calculate amounts for each line
                    BigDecimal lineTotal = nz(line.getCharge());
                    BigDecimal patientPortion = nz(line.getPatientPortion());
                    BigDecimal insurancePortion = nz(line.getInsPortion());
                    BigDecimal previousBalance = lineTotal.subtract(patientPortion).subtract(insurancePortion);
                    
                    return InsurancePaymentDetailDto.InsurancePaymentLineDetailDto.builder()
                            .lineId(line.getId())
                            .description(line.getTreatment() != null ? line.getTreatment() : (line.getCode() != null ? line.getCode() : ""))
                            .providerName(line.getProvider() != null ? line.getProvider() : "")
                            .amount(lineTotal)
                            .patient(patientPortion)
                            .insurance(insurancePortion)
                            .previousBalance(previousBalance)
                            .payment(nz(remitLine.getInsPay()))
                            .build();
                })
                .collect(Collectors.toList());
        
        // Calculate totals
        BigDecimal insWriteoff = nz(remitLine.getInsWriteOff());
        BigDecimal insuranceAmount = nz(remitLine.getInsPay());
        BigDecimal patientAmount = nz(invoice.getPtBalance());
        BigDecimal previousTotalBalance = nz(invoice.getTotalCharge());
        BigDecimal paymentAmount = nz(remitLine.getInsPay());
        
        return InsurancePaymentDetailDto.builder()
                .remitId(remitLine.getId())
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getId().toString())
                .paymentDate(LocalDate.now()) // TODO: Add payment date to entity
                .chequeNumber("") // TODO: Add cheque number to entity
                .bankBranchNumber("") // TODO: Add bank branch to entity
                .insWriteoff(insWriteoff)
                .patientAmount(patientAmount)
                .insuranceAmount(insuranceAmount)
                .previousTotalBalance(previousTotalBalance)
                .paymentAmount(paymentAmount)
                .appliedWO(nz(invoice.getInsWO()))
                .ptPaid(nz(invoice.getPtBalance()))
                .insPaid(nz(invoice.getInsBalance()))
                .lineDetails(lineDetails)
                .build();
    }

    /** Get detailed patient payment information */
    public PatientPaymentDetailDto getPatientPaymentDetails(Long patientId, Long invoiceId, Long paymentId) {
        // Fetch the payment
        PatientPayment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Patient payment not found: " + paymentId));
        
        // Fetch the invoice
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        
        // Build line details
        List<PatientPaymentDetailDto.PatientPaymentLineDetailDto> lineDetails = invoice.getLines().stream()
                .map(line -> {
                    // Calculate amounts for each line
                    BigDecimal lineTotal = nz(line.getCharge());
                    BigDecimal patientPortion = nz(line.getPatientPortion());
                    BigDecimal insurancePortion = nz(line.getInsPortion());
                    BigDecimal previousBalance = lineTotal.subtract(patientPortion).subtract(insurancePortion);
                    
                    // Find payment allocation for this line
                    BigDecimal linePayment = payment.getAllocations().stream()
                            .filter(alloc -> alloc.getInvoiceLine() != null && 
                                           alloc.getInvoiceLine().getId().equals(line.getId()))
                            .map(alloc -> nz(alloc.getAmount()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return PatientPaymentDetailDto.PatientPaymentLineDetailDto.builder()
                            .lineId(line.getId())
                            .description(line.getTreatment() != null ? line.getTreatment() : (line.getCode() != null ? line.getCode() : ""))
                            .providerName(line.getProvider() != null ? line.getProvider() : "")
                            .amount(lineTotal)
                            .patient(patientPortion)
                            .insurance(insurancePortion)
                            .previousBalance(previousBalance)
                            .payment(linePayment)
                            .build();
                })
                .collect(Collectors.toList());
        
        // Calculate totals
        BigDecimal patientAmount = nz(invoice.getPtBalance());
        BigDecimal insuranceAmount = nz(invoice.getInsBalance());
        BigDecimal previousTotalBalance = nz(invoice.getTotalCharge());
        BigDecimal paymentAmount = nz(payment.getAmount());
        
        return PatientPaymentDetailDto.builder()
                .paymentId(payment.getId())
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getId().toString())
                .paymentDate(payment.getCreatedAt())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "")
                .chequeNumber("") // TODO: Add cheque number to entity if needed
                .bankBranchNumber("") // TODO: Add bank branch to entity if needed
                .patientAmount(patientAmount)
                .insuranceAmount(insuranceAmount)
                .previousTotalBalance(previousTotalBalance)
                .paymentAmount(paymentAmount)
                .ptPaid(nz(invoice.getPtBalance()))
                .insPaid(nz(invoice.getInsBalance()))
                .lineDetails(lineDetails)
                .build();
    }

    /* ================ Patient Payment & Credit ================ */

    public PatientInvoiceDto applyPatientPayment(Long patientId, Long invoiceId, PatientPatientPaymentRequestDto req) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);

        BigDecimal outstanding = nz(invoice.getInsBalance()).add(nz(invoice.getPtBalance()));
        BigDecimal entered = (req == null || req.allocations() == null)
                ? BigDecimal.ZERO
                : req.allocations().stream()
                .map(a -> nz(a.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // If nothing is outstanding, entire payment becomes account credit
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            addCredit(patientId, entered);
            invoice.setPtBalance(BigDecimal.ZERO);
            invoice.setInsBalance(BigDecimal.ZERO);
            invoice.setStatus(PatientInvoice.Status.PAID);
            invoiceRepo.save(invoice);
            return toInvoiceDto(invoice);
        }

        BigDecimal cover = entered.min(outstanding);
        BigDecimal remaining = outstanding.subtract(cover);

        // Use account credit if available
        PatientAccountCredit credit = creditRepo.findByPatientId(patientId).orElse(null);
        if (credit != null && credit.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal usedCredit = credit.getBalance().min(cover);
            credit.setBalance(credit.getBalance().subtract(usedCredit));
            cover = cover.subtract(usedCredit);
            creditRepo.save(credit);
        }

        // If fully paid, set balances to zero and status to PAID
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setPtBalance(BigDecimal.ZERO);
            invoice.setInsBalance(BigDecimal.ZERO);
            invoice.setStatus(PatientInvoice.Status.PAID);
        } else {
            invoice.setPtBalance(remaining);
            invoice.setStatus(PatientInvoice.Status.PARTIALLY_PAID);
        }

        BigDecimal over = entered.subtract(outstanding.max(BigDecimal.ZERO));
        if (over.compareTo(BigDecimal.ZERO) > 0) addCredit(patientId, over);

        // Persist payment
        PatientPayment savedPayment = paymentRepo.saveAndFlush(
                new PatientPayment(patientId, PaymentMethod.valueOf(req.paymentMethod()), entered)
        );

        // Persist allocations
        if (req.allocations() != null) {
            for (var allocReq : req.allocations()) {
                PatientInvoiceLine line = invoiceLineRepo.findById(allocReq.invoiceLineId())
                        .orElseThrow(() -> new RuntimeException("Invoice line not found: " + allocReq.invoiceLineId()));

                PatientPaymentAllocation alloc = new PatientPaymentAllocation(savedPayment, line, allocReq.amount());
                allocationRepo.save(alloc);
            }
        }

        invoiceRepo.save(invoice);
        return toInvoiceDto(invoice);
    }

    public List<PatientPatientPaymentAllocationDto> getAllPatientPayments(Long patientId) {
        var allocations = allocationRepo.findByPatientId(patientId);
        return allocations.stream()
                .map(a -> new PatientPatientPaymentAllocationDto(
                        a.getId(),
                        a.getInvoiceLine() != null ? a.getInvoiceLine().getId() : null,
                        a.getAmount(),
                        a.getPayment() != null ? a.getPayment().getPaymentMethod().name() : null,
                        a.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public List<PatientPatientPaymentAllocationDto> getPatientPaymentsByInvoice(Long patientId, Long invoiceId) {
        getInvoiceOrThrow(patientId, invoiceId);
        var allocations = allocationRepo.findByInvoiceId(invoiceId);
        return allocations.stream()
                .map(a -> new PatientPatientPaymentAllocationDto(
                        a.getId(),
                        a.getInvoiceLine() != null ? a.getInvoiceLine().getId() : null,
                        a.getAmount(),
                        a.getPayment() != null ? a.getPayment().getPaymentMethod().name() : null,
                        a.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public PatientInvoiceDto editPatientPayment(Long patientId, Long invoiceId, Long paymentId, PatientPaymentDto dto) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        PatientPayment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Patient payment not found"));

        if (dto.amount() != null) payment.setAmount(dto.amount());
        if (dto.paymentMethod() != null) {
            String method = dto.paymentMethod().replace(" ", "_").replace("-", "_").toUpperCase();
            payment.setPaymentMethod(PaymentMethod.valueOf(method));
        }
        paymentRepo.save(payment);

        invoice.recalcTotals();
        return toInvoiceDto(invoice);
    }

    /** VOID = delete allocations then delete payment */
    public PatientInvoiceDto voidPatientPayment(Long patientId, Long invoiceId, Long paymentId, VoidReason reason) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        List<PatientPaymentAllocation> allocs = allocationRepo.findByPaymentId(paymentId);
        allocs.forEach(allocationRepo::delete);
        paymentRepo.deleteById(paymentId);
        invoice.recalcTotals();
        return toInvoiceDto(invoice);
    }

    /** REFUND patient → add to patient account credit */
    public PatientInvoiceDto refundPatientPayment(Long patientId, Long invoiceId, Long paymentId, RefundRequest req) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        BigDecimal amount = Optional.ofNullable(req).map(RefundRequest::amount)
                .orElseThrow(() -> new IllegalArgumentException("Refund amount required"));
        if (amount.signum() <= 0) throw new IllegalArgumentException("Refund amount must be > 0");

        PatientPayment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Patient payment not found"));

        if (payment.getAmount().compareTo(amount) == 0) {
            allocationRepo.findByPaymentId(paymentId).forEach(allocationRepo::delete);
            paymentRepo.deleteById(paymentId);
        } else if (payment.getAmount().compareTo(amount) > 0) {
            payment.setAmount(payment.getAmount().subtract(amount));
            paymentRepo.save(payment);
        } else {
            throw new IllegalArgumentException("Refund amount exceeds payment amount");
        }

        addCredit(patientId, amount);
        invoice.recalcTotals();
        return toInvoiceDto(invoice);
    }

    public PatientAccountCreditDto[] transferPatientCreditToPatient(Long fromPatientId, Long toPatientId, BigDecimal amount) {
        if (fromPatientId.equals(toPatientId)) throw new IllegalArgumentException("Source and destination patients must differ");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive");

        PatientAccountCredit fromCredit = creditRepo.findByPatientId(fromPatientId)
                .orElseThrow(() -> new IllegalArgumentException("Source patient has no credit account"));
        if (fromCredit.getBalance().compareTo(amount) < 0) throw new IllegalArgumentException("Insufficient credit in source account");

        PatientAccountCredit toCredit = creditRepo.findByPatientId(toPatientId)
                .orElseGet(() -> {
                    PatientAccountCredit ac = new PatientAccountCredit();
                    ac.setPatientId(toPatientId);
                    ac.setBalance(BigDecimal.ZERO);
                    return creditRepo.save(ac);
                });

        fromCredit.setBalance(fromCredit.getBalance().subtract(amount));
        toCredit.setBalance(nz(toCredit.getBalance()).add(amount));
        creditRepo.save(fromCredit);
        creditRepo.save(toCredit);

        return new PatientAccountCreditDto[] {
                new PatientAccountCreditDto(fromPatientId, fromCredit.getBalance()),
                new PatientAccountCreditDto(toPatientId, toCredit.getBalance())
        };
    }

    /* ===================== Account Credit ===================== */

    public PatientAccountCreditDto getAccountCredit(Long patientId) {
        PatientAccountCredit c = creditRepo.findByPatientId(patientId)
                .orElseGet(() -> {
                    PatientAccountCredit ac = new PatientAccountCredit();
                    ac.setPatientId(patientId);
                    ac.setBalance(BigDecimal.ZERO);
                    return creditRepo.save(ac);
                });
        return new PatientAccountCreditDto(patientId, c.getBalance());
    }

    public PatientAccountCreditDto applyAccountCredit(Long patientId, ApplyCreditRequest b) {
        BigDecimal amount = (b == null) ? BigDecimal.ZERO : nz(b.amount());
        PatientAccountCredit c = creditRepo.findByPatientId(patientId).orElseThrow();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return new PatientAccountCreditDto(patientId, c.getBalance());
        if (c.getBalance().compareTo(amount) < 0) throw new IllegalArgumentException("Insufficient credit");
        c.setBalance(c.getBalance().subtract(amount));
        return new PatientAccountCreditDto(patientId, c.getBalance());
    }

    public PatientDepositDto addPatientDeposit(Long patientId, PatientDepositRequest request) {
        if (request == null || request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        // Create deposit record
        PatientDeposit deposit = new PatientDeposit();
        deposit.setPatientId(patientId);
        deposit.setAmount(request.amount());
        deposit.setDepositDate(request.depositDate() != null ? request.depositDate() : LocalDate.now());
        deposit.setDescription(request.description());
        deposit.setPaymentMethod(request.paymentMethod());
        depositRepo.save(deposit);

        // Update account credit
        var creditOpt = creditRepo.findByPatientId(patientId);
        var credit = creditOpt.orElseGet(() -> {
            var c = new PatientAccountCredit();
            c.setPatientId(patientId);
            c.setBalance(java.math.BigDecimal.ZERO);
            return c;
        });
        credit.setBalance(credit.getBalance().add(request.amount()));
        creditRepo.save(credit);

        return toDepositDto(deposit);
    }

    /**
     * Get all deposits for a patient
     */
    public List<PatientDepositDto> getPatientDeposits(Long patientId) {
        List<PatientDeposit> deposits = depositRepo.findByPatientIdOrderByDepositDateDesc(patientId);
        return deposits.stream()
                .map(this::toDepositDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a single deposit by id
     */
    public PatientDepositDto getPatientDeposit(Long patientId, Long depositId) {
        PatientDeposit deposit = depositRepo.findByIdAndPatientId(depositId, patientId)
                .orElseThrow(() -> new IllegalArgumentException("Deposit not found for patient"));
        return toDepositDto(deposit);
    }

    /**
     * Update an existing deposit
     */
    public PatientDepositDto updatePatientDeposit(Long patientId, Long depositId, PatientDepositRequest request) {
        if (request == null || request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        PatientDeposit deposit = depositRepo.findByIdAndPatientId(depositId, patientId)
                .orElseThrow(() -> new IllegalArgumentException("Deposit not found for patient"));

        BigDecimal oldAmount = deposit.getAmount();
        BigDecimal newAmount = request.amount();
        BigDecimal difference = newAmount.subtract(oldAmount);

        // Update deposit record
        deposit.setAmount(newAmount);
        deposit.setDepositDate(request.depositDate() != null ? request.depositDate() : deposit.getDepositDate());
        deposit.setDescription(request.description());
        deposit.setPaymentMethod(request.paymentMethod());
        depositRepo.save(deposit);

        // Update account credit balance
        var creditOpt = creditRepo.findByPatientId(patientId);
        if (creditOpt.isPresent()) {
            var credit = creditOpt.get();
            credit.setBalance(credit.getBalance().add(difference));
            creditRepo.save(credit);
        }

        return toDepositDto(deposit);
    }

    /**
     * Delete a deposit
     */
    public void deletePatientDeposit(Long patientId, Long depositId) {
        PatientDeposit deposit = depositRepo.findByIdAndPatientId(depositId, patientId)
                .orElseThrow(() -> new IllegalArgumentException("Deposit not found for patient"));

        // Update account credit balance (subtract the deposit amount)
        var creditOpt = creditRepo.findByPatientId(patientId);
        if (creditOpt.isPresent()) {
            var credit = creditOpt.get();
            credit.setBalance(credit.getBalance().subtract(deposit.getAmount()));
            creditRepo.save(credit);
        }

        // Delete the deposit
        depositRepo.delete(deposit);
    }



    /* ===================== Notes ===================== */

    /**
     * List notes for a specific invoice (targetType=INVOICE, targetId=invoiceId)
     */
    public List<PatientBillingNoteDto> listInvoiceNotes(Long patientId, Long invoiceId) {
        // Verify invoice exists and belongs to patient
        getInvoiceOrThrow(patientId, invoiceId);

        return noteRepo.findByPatientIdAndTargetTypeAndTargetIdOrderByCreatedDateAsc(patientId, NoteTargetType.INVOICE, invoiceId)
                .stream()
                .map(PatientBillingNoteDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Create a note for a specific invoice
     */
    public PatientBillingNoteDto createInvoiceNote(Long patientId, Long invoiceId, PatientBillingNoteDto dto) {
        // Verify invoice exists and belongs to patient
        getInvoiceOrThrow(patientId, invoiceId);

        if (dto == null || dto.text == null) {
            throw new IllegalArgumentException("Required fields missing for note creation: text");
        }

        // If provided in body, validate against path (optional: can remove if not needed)
        if (dto.patientId != null && !dto.patientId.equals(patientId)) {
            throw new IllegalArgumentException("Patient ID mismatch");
        }
        if (dto.invoiceId != null && !dto.invoiceId.equals(invoiceId)) {
            throw new IllegalArgumentException("Invoice ID mismatch");
        }

        PatientBillingNote note = new PatientBillingNote();
        note.setPatientId(patientId); // Always from path
        note.setInvoiceId(invoiceId); // Always from path
        note.setTargetType(NoteTargetType.INVOICE);
        note.setTargetId(invoiceId);
        note.setText(dto.text);
        note.setCreatedBy(dto.createdBy); // Optional

        noteRepo.save(note);
        return PatientBillingNoteDto.from(note);
    }

    /**
     * Update a note for an invoice
     */
    public PatientBillingNoteDto updateInvoiceNote(Long patientId, Long invoiceId, Long noteId, PatientBillingNoteDto dto) {
        PatientBillingNote note = noteRepo.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        if (!note.getPatientId().equals(patientId)) {
            throw new IllegalArgumentException("Note does not belong to this patient");
        }
        if (note.getTargetType() != NoteTargetType.INVOICE || !note.getTargetId().equals(invoiceId)) {
            throw new IllegalArgumentException("Note does not belong to this invoice");
        }

        if (dto.text != null) {
            note.setText(dto.text);
        }
        if (dto.createdBy != null) {
            note.setCreatedBy(dto.createdBy);
        }
        // TargetType and TargetId are immutable

        noteRepo.save(note);
        return PatientBillingNoteDto.from(note);
    }

    /**
     * Delete a note for an invoice
     */
    public void deleteInvoiceNote(Long patientId, Long invoiceId, Long noteId) {
        PatientBillingNote note = noteRepo.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        if (!note.getPatientId().equals(patientId)) {
            throw new IllegalArgumentException("Note does not belong to this patient");
        }
        if (note.getTargetType() != NoteTargetType.INVOICE || !note.getTargetId().equals(invoiceId)) {
            throw new IllegalArgumentException("Note does not belong to this invoice");
        }

        noteRepo.delete(note);
    }

    /**
     * Add insurance deposit and update patient account credit
     */
    public PatientAccountCreditDto addInsuranceDeposit(Long patientId, InsuranceDepositRequest request) {
        // Find or create PatientAccountCredit
        var credit = creditRepo.findByPatientId(patientId)
                .orElseGet(() -> {
                    var c = new PatientAccountCredit();
                    c.setPatientId(patientId);
                    c.setBalance(java.math.BigDecimal.ZERO);
                    return c;
                });
        java.math.BigDecimal amount = request.amount() != null ? request.amount() : java.math.BigDecimal.ZERO;
        credit.setBalance(credit.getBalance().add(amount));
        creditRepo.save(credit);
        // TODO: Optionally, persist insurance deposit as a separate entity for audit/history
        return new PatientAccountCreditDto(patientId, credit.getBalance());
    }

    /**
     * Add courtesy credit and update patient account credit
     */
    public PatientAccountCreditDto addCourtesyCredit(Long patientId, CourtesyCreditRequest request) {
        // Find or create PatientAccountCredit
        var credit = creditRepo.findByPatientId(patientId)
                .orElseGet(() -> {
                    var c = new PatientAccountCredit();
                    c.setPatientId(patientId);
                    c.setBalance(java.math.BigDecimal.ZERO);
                    return c;
                });
        java.math.BigDecimal amount = request.amount() != null ? request.amount() : java.math.BigDecimal.ZERO;
        credit.setBalance(credit.getBalance().add(amount));
        creditRepo.save(credit);

        log.info("Courtesy credit added: patientId={}, amount={}, type={}",
                patientId, amount, request.adjustmentType());

        return new PatientAccountCreditDto(patientId, credit.getBalance());
    }





    /**
     * Apply courtesy credit directly to a specific invoice
     * This creates a record in invoice_courtesy_credit table and reduces the patient balance
     */
    public InvoiceCourtesyCreditDto applyCourtesyCreditToInvoice(Long patientId, Long invoiceId, CourtesyCreditRequest request) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);

        BigDecimal creditAmount = request.amount() != null ? request.amount() : BigDecimal.ZERO;
        BigDecimal currentPtBalance = nz(invoice.getPtBalance());

        // Ensure we don't credit more than the patient balance
        if (creditAmount.compareTo(currentPtBalance) > 0) {
            creditAmount = currentPtBalance;
        }

        // Create courtesy credit record in database
        InvoiceCourtesyCredit courtesyCredit = new InvoiceCourtesyCredit();
        courtesyCredit.setPatientId(patientId);
        courtesyCredit.setInvoiceId(invoiceId);
        courtesyCredit.setAdjustmentType(request.adjustmentType() != null ? request.adjustmentType() : "Courtesy Adjustment");
        courtesyCredit.setAmount(creditAmount);
        courtesyCredit.setDescription(request.description());
        courtesyCredit.setIsActive(true);
        courtesyCredit.setIsDeleted(false);

        invoiceCourtesyCreditRepo.save(courtesyCredit);

        // Reduce patient balance
        invoice.setPtBalance(currentPtBalance.subtract(creditAmount));

        // Update invoice status if balance is zero and closeInvoice flag is true
        if (invoice.getPtBalance().compareTo(BigDecimal.ZERO) == 0
                && nz(invoice.getInsBalance()).compareTo(BigDecimal.ZERO) == 0
                && Boolean.TRUE.equals(request.closeInvoice())) {
            invoice.setStatus(PatientInvoice.Status.PAID);
        } else if (invoice.getPtBalance().compareTo(BigDecimal.ZERO) == 0
                && nz(invoice.getInsBalance()).compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(PatientInvoice.Status.PARTIALLY_PAID);
        }

        invoiceRepo.save(invoice);

        log.info("Courtesy credit applied to invoice: patientId={}, invoiceId={}, creditId={}, amount={}, type={}, newPtBalance={}",
                patientId, invoiceId, courtesyCredit.getId(), creditAmount, request.adjustmentType(), invoice.getPtBalance());

        return InvoiceCourtesyCreditDto.from(courtesyCredit);
    }

    /**
     * Get invoice with courtesy credit details from database
     * Returns the list of courtesy credits applied to this invoice
     */
    public List<InvoiceCourtesyCreditDto> getInvoiceWithCourtesyCredit(Long patientId, Long invoiceId) {
        // Verify invoice exists and belongs to patient
        getInvoiceOrThrow(patientId, invoiceId);

        // Get all courtesy credits for this invoice
        List<InvoiceCourtesyCredit> credits = invoiceCourtesyCreditRepo
            .findByInvoiceIdAndIsDeletedOrderByCreatedDateDesc(invoiceId, false);

        log.info("Retrieved {} courtesy credit records for invoice: patientId={}, invoiceId={}",
                credits.size(), patientId, invoiceId);

        return credits.stream()
                .map(InvoiceCourtesyCreditDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Update courtesy credit applied to a specific invoice
     * This updates the database record and adjusts the patient balance accordingly
     */
    public InvoiceCourtesyCreditDto updateInvoiceCourtesyCredit(Long patientId, Long invoiceId, CourtesyCreditRequest request) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);

        // Find the active courtesy credit for this invoice
        InvoiceCourtesyCredit courtesyCredit = invoiceCourtesyCreditRepo
            .findByInvoiceIdAndIsActiveAndIsDeleted(invoiceId, true, false)
            .orElseThrow(() -> new IllegalArgumentException("No active courtesy credit found for invoice " + invoiceId));

        BigDecimal oldCreditAmount = courtesyCredit.getAmount();
        BigDecimal newCreditAmount = request.amount() != null ? request.amount() : BigDecimal.ZERO;
        BigDecimal currentPtBalance = nz(invoice.getPtBalance());
        BigDecimal totalCharge = nz(invoice.getTotalCharge());

        // Calculate the difference in credit amount
        BigDecimal creditDifference = newCreditAmount.subtract(oldCreditAmount);

        // Update the courtesy credit record
        courtesyCredit.setAmount(newCreditAmount);
        courtesyCredit.setAdjustmentType(request.adjustmentType() != null ? request.adjustmentType() : courtesyCredit.getAdjustmentType());
        courtesyCredit.setDescription(request.description());
        invoiceCourtesyCreditRepo.save(courtesyCredit);

        // Adjust patient balance based on credit difference
        // If credit increased, reduce patient balance; if credit decreased, increase patient balance
        BigDecimal newPtBalance = currentPtBalance.subtract(creditDifference);

        // Ensure patient balance doesn't go negative or exceed total charge
        if (newPtBalance.compareTo(BigDecimal.ZERO) < 0) {
            newPtBalance = BigDecimal.ZERO;
        }
        if (newPtBalance.compareTo(totalCharge) > 0) {
            newPtBalance = totalCharge;
        }

        invoice.setPtBalance(newPtBalance);

        // Update invoice status
        if (invoice.getPtBalance().compareTo(BigDecimal.ZERO) == 0
                && nz(invoice.getInsBalance()).compareTo(BigDecimal.ZERO) == 0
                && Boolean.TRUE.equals(request.closeInvoice())) {
            invoice.setStatus(PatientInvoice.Status.PAID);
        } else if (invoice.getPtBalance().compareTo(BigDecimal.ZERO) == 0
                && nz(invoice.getInsBalance()).compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(PatientInvoice.Status.PARTIALLY_PAID);
        } else if (invoice.getPtBalance().compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(PatientInvoice.Status.OPEN);
        }

        invoiceRepo.save(invoice);

        log.info("Courtesy credit updated on invoice: patientId={}, invoiceId={}, creditId={}, oldAmount={}, newAmount={}, oldPtBalance={}, newPtBalance={}",
                patientId, invoiceId, courtesyCredit.getId(), oldCreditAmount, newCreditAmount, currentPtBalance, invoice.getPtBalance());

        return InvoiceCourtesyCreditDto.from(courtesyCredit);
    }

    /**
     * Remove courtesy credit from a specific invoice
     * This marks the record as inactive and restores the patient balance
     */
    public InvoiceCourtesyCreditDto removeInvoiceCourtesyCredit(Long patientId, Long invoiceId) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);

        // Find the active courtesy credit for this invoice
        InvoiceCourtesyCredit courtesyCredit = invoiceCourtesyCreditRepo
            .findByInvoiceIdAndIsActiveAndIsDeleted(invoiceId, true, false)
            .orElseThrow(() -> new IllegalArgumentException("No active courtesy credit found for invoice " + invoiceId));

        BigDecimal creditAmountToRemove = courtesyCredit.getAmount();
        BigDecimal currentPtBalance = nz(invoice.getPtBalance());
        BigDecimal totalCharge = nz(invoice.getTotalCharge());

        // Mark courtesy credit as inactive (soft delete)
        courtesyCredit.setIsActive(false);
        invoiceCourtesyCreditRepo.save(courtesyCredit);

        // Add back the credit amount to patient balance (reverse the credit)
        BigDecimal newPtBalance = currentPtBalance.add(creditAmountToRemove);

        // Ensure patient balance doesn't exceed total charge
        if (newPtBalance.compareTo(totalCharge) > 0) {
            newPtBalance = totalCharge;
        }

        invoice.setPtBalance(newPtBalance);

        // Update invoice status
        if (invoice.getPtBalance().compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(PatientInvoice.Status.OPEN);
        }

        invoiceRepo.save(invoice);

        log.info("Courtesy credit removed from invoice: patientId={}, invoiceId={}, creditId={}, creditRemoved={}, oldPtBalance={}, newPtBalance={}",
                patientId, invoiceId, courtesyCredit.getId(), creditAmountToRemove, currentPtBalance, invoice.getPtBalance());

        return InvoiceCourtesyCreditDto.from(courtesyCredit);
    }




    /** Lock claim (after lock, claim cannot be edited) */
    public void lockClaim(Long patientId, Long claimId) {
        PatientClaim claim = claimRepo.findById(claimId).orElseThrow();
        claim.setLocked(true);
        claimRepo.save(claim);
    }


    public void changeClaimStatus(Long patientId, Long claimId, ClaimStatusUpdateDto dto) {
        PatientClaim claim = claimRepo.findById(claimId).orElseThrow();
        if (dto.getStatus() != null) {
            claim.setStatus(PatientClaim.Status.valueOf(dto.getStatus()));
        }
        if (dto.getRemitDate() != null && !dto.getRemitDate().isEmpty()) {
            claim.setRemittanceDate(dto.getRemitDate()); // Assuming remittanceDate is a String in PatientClaim
        }
        if (dto.getPaymentAmount() != null) {
            claim.setInsurancePaymentAmount(dto.getPaymentAmount().toPlainString()); // Assuming insurancePaymentAmount is a String
        }
        claimRepo.save(claim);
    }

    /**
     * Get claim by ID and convert to DTO
     */
    public PatientClaimDto getClaimDtoById(Long claimId) {
        PatientClaim claim = claimRepo.findById(claimId).orElseThrow();
        return toClaimDto(claim);
    }



    /** Submit claim attachment */
    public void submitClaimAttachment(Long patientId, Long claimId, MultipartFile file) throws Exception {
        // Save file to claim entity and increment attachment count
        PatientClaim claim = claimRepo.findById(claimId).orElseThrow();
        if (file != null && !file.isEmpty()) {
            claim.setAttachmentFile(file.getBytes());
            claim.setAttachments(claim.getAttachments() + 1);
        }
        claimRepo.save(claim);
    }

    /**
     * Generate a printable invoice for a specific invoice with complete transaction history.
     */
    public PatientInvoicePrintDto getPrintableInvoice(Long patientId, Long invoiceId) {
        PatientInvoice invoice = getInvoiceOrThrow(patientId, invoiceId);
        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        PatientInvoicePrintDto dto = new PatientInvoicePrintDto();

        // Practice Info - Get from primary facility
        PatientInvoicePrintDto.PracticeInfo practice = new PatientInvoicePrintDto.PracticeInfo();
        Facility facility = getPrimaryFacility();
        if (facility != null) {
            practice.practiceName = facility.getName();
            // Construct full address
            StringBuilder addressBuilder = new StringBuilder();
            if (facility.getPhysicalAddress() != null) {
                addressBuilder.append(facility.getPhysicalAddress());
            }
            if (facility.getPhysicalCity() != null) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(facility.getPhysicalCity());
            }
            if (facility.getPhysicalState() != null) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(facility.getPhysicalState());
            }
            if (facility.getPhysicalZipCode() != null) {
                if (addressBuilder.length() > 0) addressBuilder.append(" ");
                addressBuilder.append(facility.getPhysicalZipCode());
            }
            practice.address = addressBuilder.toString();
            practice.phone = facility.getPhone();
            practice.email = facility.getEmail();
            practice.website = facility.getWebsite();
        } else {
            // Fallback to default values if no facility found
            log.warn("No active facility found, using default practice info");
            practice.practiceName = "Practice Name Not Set";
            practice.address = "";
            practice.phone = "";
            practice.email = "";
            practice.website = "";
        }
        dto.practice = practice;

        // Patient Info
        dto.patientId = patientId;
        dto.patientName = (patient.getFirstName() != null ? patient.getFirstName() : "") + " " +
                (patient.getLastName() != null ? patient.getLastName() : "");
        dto.patientPhone = patient.getPhoneNumber();
        dto.patientEmail = patient.getEmail();
        dto.patientAddress = patient.getAddress();

        // Invoice Info
        dto.invoiceId = invoice.getId();
        dto.invoiceDate = invoice.getCreatedAt();
        dto.invoiceNumber = "Invoice #" + invoice.getId();
        dto.status = invoice.getStatus() != null ? invoice.getStatus().name() : "OPEN";

        // Build Transaction History (main statement table)
        List<PatientInvoicePrintDto.TransactionLine> transactions = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO;

        // 1. Add Invoice Header
        PatientInvoicePrintDto.TransactionLine invoiceHeader = new PatientInvoicePrintDto.TransactionLine();
        invoiceHeader.date = invoice.getCreatedAt() != null ? invoice.getCreatedAt().toLocalDate() : LocalDate.now();
        invoiceHeader.description = "Invoice #" + invoice.getId() + ": $" +
                (invoice.getTotalCharge() != null ? invoice.getTotalCharge() : BigDecimal.ZERO);
        invoiceHeader.transactionType = "INVOICE";
        runningBalance = runningBalance.add(invoice.getTotalCharge() != null ? invoice.getTotalCharge() : BigDecimal.ZERO);
        invoiceHeader.balance = runningBalance;
        transactions.add(invoiceHeader);

        // 2. Add Invoice Lines (procedure details)
        for (PatientInvoiceLine line : invoice.getLines()) {
            PatientInvoicePrintDto.TransactionLine lineTransaction = new PatientInvoicePrintDto.TransactionLine();
            lineTransaction.date = line.getDos() != null ? line.getDos() : invoiceHeader.date;
            lineTransaction.code = line.getCode();
            lineTransaction.procedureDescription = line.getTreatment();
            lineTransaction.description = line.getCode() + " " + line.getTreatment();
            lineTransaction.provider = line.getProvider();
            lineTransaction.amount = line.getCharge();
            lineTransaction.transactionType = "INVOICE_LINE";
            lineTransaction.balance = runningBalance;
            transactions.add(lineTransaction);
        }

        // 3. Add Claims
        List<PatientInvoicePrintDto.ClaimInfo> claimsInfo = new ArrayList<>();
        Optional<PatientClaim> claimOpt = claimRepo.findByInvoiceIdAndPatientId(invoiceId, patientId);
        if (claimOpt.isPresent()) {
            PatientClaim claim = claimOpt.get();
            PatientInvoicePrintDto.TransactionLine claimTransaction = new PatientInvoicePrintDto.TransactionLine();
            claimTransaction.date = claim.getCreatedOn() != null ?
                    claim.getCreatedOn() : invoiceHeader.date;
            claimTransaction.description = "Claim #" + claim.getId();
            claimTransaction.transactionType = "CLAIM";
            claimTransaction.balance = runningBalance;
            transactions.add(claimTransaction);

            // Add claim detail line (Local ID by Insurance)
            PatientInvoicePrintDto.TransactionLine claimDetail = new PatientInvoicePrintDto.TransactionLine();
            claimDetail.date = claimTransaction.date;
            claimDetail.description = "Local " + claim.getId() + " by " +
                    (claim.getPayerName() != null ? claim.getPayerName() : "INSURANCE");
            claimDetail.transactionType = "CLAIM";
            claimDetail.balance = runningBalance;
            transactions.add(claimDetail);

            // Get insurance name from Coverage
            String insuranceName = claim.getPayerName();
            if (insuranceName == null || insuranceName.isEmpty()) {
                List<Coverage> coverages = coverageRepo.findByPatientIdOrderByEffectiveDateDesc(patientId);
                if (!coverages.isEmpty()) {
                    insuranceName = coverages.get(0).getPlanName();
                }
            }
            if (insuranceName == null) insuranceName = "INSURANCE";

            // Store claim info
            PatientInvoicePrintDto.ClaimInfo claimInfo = new PatientInvoicePrintDto.ClaimInfo();
            claimInfo.claimId = claim.getId();
            claimInfo.claimNumber = String.valueOf(claim.getId());
            claimInfo.insuranceName = insuranceName;
            claimInfo.localId = "Local " + claim.getId();
            claimInfo.status = claim.getStatus() != null ? claim.getStatus().name() : "";
            claimsInfo.add(claimInfo);
        }
        dto.claims = claimsInfo;

        // 4. Add Insurance Payments with details
        List<PatientInvoicePrintDto.InsurancePaymentDetail> insurancePaymentDetails = new ArrayList<>();
        List<PatientInsuranceRemitLine> insurancePayments = remitRepo.findByInvoiceId(invoiceId);

        // Group insurance payments by payment ID/date
        java.util.Map<String, List<PatientInsuranceRemitLine>> groupedPayments = new java.util.HashMap<>();
        for (PatientInsuranceRemitLine remit : insurancePayments) {
            String key = remit.getId() + "_" + (remit.getCreatedDate() != null ? remit.getCreatedDate().toLocalDate() : LocalDate.now());
            groupedPayments.computeIfAbsent(key, k -> new ArrayList<>()).add(remit);
        }

        int paymentCounter = 1;
        for (List<PatientInsuranceRemitLine> paymentGroup : groupedPayments.values()) {
            if (paymentGroup.isEmpty()) continue;

            PatientInsuranceRemitLine firstRemit = paymentGroup.get(0);
            LocalDate paymentDate = firstRemit.getCreatedDate() != null ?
                    firstRemit.getCreatedDate().toLocalDate() : LocalDate.now();

            // Calculate total for this payment
            BigDecimal paymentTotal = paymentGroup.stream()
                    .map(r -> r.getInsPay() != null ? r.getInsPay() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal writeOffTotal = paymentGroup.stream()
                    .map(r -> r.getInsWriteOff() != null ? r.getInsWriteOff() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Add Insurance Write-off line (if any)
            if (writeOffTotal.compareTo(BigDecimal.ZERO) > 0) {
                PatientInvoicePrintDto.TransactionLine writeOffLine = new PatientInvoicePrintDto.TransactionLine();
                writeOffLine.date = paymentDate;
                writeOffLine.description = "Insurance Write-off #" + (1200 + paymentCounter);
                writeOffLine.credit = writeOffTotal;
                writeOffLine.transactionType = "ADJUSTMENT";
                runningBalance = runningBalance.subtract(writeOffTotal);
                writeOffLine.balance = runningBalance;
                transactions.add(writeOffLine);
            }

            // Add Insurance Payment line
            PatientInvoicePrintDto.TransactionLine paymentLine = new PatientInvoicePrintDto.TransactionLine();
            paymentLine.date = paymentDate;
            paymentLine.description = "Insurance Pay #" + (1200 + paymentCounter) +
                    " (Check " + (528000000 + paymentCounter * 1000) + ")";
            paymentLine.credit = paymentTotal;
            paymentLine.transactionType = "INSURANCE_PAYMENT";
            runningBalance = runningBalance.subtract(paymentTotal);
            paymentLine.balance = runningBalance;
            transactions.add(paymentLine);

            // Get insurance name from Coverage
            String insurancePayerName = "HORIZON";
            List<Coverage> coverages = coverageRepo.findByPatientIdOrderByEffectiveDateDesc(patientId);
            if (!coverages.isEmpty()) {
                insurancePayerName = coverages.get(0).getPlanName() != null ?
                        coverages.get(0).getPlanName() : "HORIZON";
            }

            // Add payment detail lines (Local xxxx by INSURANCE)
            PatientInvoicePrintDto.TransactionLine paymentDetail = new PatientInvoicePrintDto.TransactionLine();
            paymentDetail.date = paymentDate;
            paymentDetail.description = "Local " + (1200 + paymentCounter) + " by " + insurancePayerName;
            paymentDetail.transactionType = "INSURANCE_PAYMENT";
            paymentDetail.balance = runningBalance;
            transactions.add(paymentDetail);

            // Add individual procedure payments
            for (PatientInsuranceRemitLine remit : paymentGroup) {
                PatientInvoicePrintDto.TransactionLine procLine = new PatientInvoicePrintDto.TransactionLine();
                procLine.date = paymentDate;
                procLine.code = "D0" + (230 + paymentCounter * 10); // Simulated code
                procLine.amount = remit.getInsPay();
                procLine.transactionType = "INSURANCE_PAYMENT";
                procLine.balance = runningBalance;
                transactions.add(procLine);
            }

            // Store insurance payment detail
            PatientInvoicePrintDto.InsurancePaymentDetail paymentDetailObj = new PatientInvoicePrintDto.InsurancePaymentDetail();
            paymentDetailObj.paymentId = firstRemit.getId();
            paymentDetailObj.paymentDate = paymentDate;
            paymentDetailObj.description = "Insurance Pay #" + (1200 + paymentCounter);
            paymentDetailObj.insuranceName = insurancePayerName;
            paymentDetailObj.amount = paymentTotal;
            paymentDetailObj.credit = paymentTotal;

            List<PatientInvoicePrintDto.InsurancePaymentLine> payLines = new ArrayList<>();
            for (PatientInsuranceRemitLine remit : paymentGroup) {
                PatientInvoicePrintDto.InsurancePaymentLine payLine = new PatientInvoicePrintDto.InsurancePaymentLine();
                payLine.code = "D0" + (230 + paymentCounter * 10);
                payLine.amount = remit.getInsPay();
                payLines.add(payLine);
            }
            paymentDetailObj.lines = payLines;
            insurancePaymentDetails.add(paymentDetailObj);

            paymentCounter++;
        }
        dto.insurancePayments = insurancePaymentDetails;

        // 5. Add Patient Payments with details
        List<PatientInvoicePrintDto.PatientPaymentDetail> patientPaymentDetails = new ArrayList<>();
        List<PatientPaymentAllocation> patientPayments = allocationRepo.findByInvoiceId(invoiceId);
        
        // Group patient payments by payment ID to avoid duplicates
        java.util.Map<Long, PatientPayment> paymentMap = new java.util.HashMap<>();
        for (PatientPaymentAllocation allocation : patientPayments) {
            PatientPayment payment = allocation.getPayment();
            if (payment != null) {
                paymentMap.put(payment.getId(), payment);
            }
        }
        
        // Add patient payment transactions and details
        for (PatientPayment payment : paymentMap.values()) {
            PatientInvoicePrintDto.TransactionLine paymentLine = new PatientInvoicePrintDto.TransactionLine();
            paymentLine.date = payment.getCreatedAt() != null ?
                    payment.getCreatedAt().toLocalDate() : LocalDate.now();
            paymentLine.description = "Patient Payment";
            paymentLine.credit = payment.getAmount();
            paymentLine.transactionType = "PATIENT_PAYMENT";
            runningBalance = runningBalance.subtract(payment.getAmount() != null ?
                    payment.getAmount() : BigDecimal.ZERO);
            paymentLine.balance = runningBalance;
            transactions.add(paymentLine);
            
            // Store patient payment detail
            PatientInvoicePrintDto.PatientPaymentDetail paymentDetail = new PatientInvoicePrintDto.PatientPaymentDetail();
            paymentDetail.paymentId = payment.getId();
            paymentDetail.paymentDate = paymentLine.date;
            paymentDetail.description = "Patient Payment";
            paymentDetail.paymentMethod = payment.getPaymentMethod() != null ? 
                    payment.getPaymentMethod().name() : "UNKNOWN";
            paymentDetail.amount = payment.getAmount();
            paymentDetail.credit = payment.getAmount();
            patientPaymentDetails.add(paymentDetail);
        }
        dto.patientPayments = patientPaymentDetails;
        
        // 6. Add Patient Deposits
        List<PatientInvoicePrintDto.PatientDepositDetail> depositDetails = new ArrayList<>();
        List<PatientDeposit> deposits = depositRepo.findByPatientIdOrderByDepositDateDesc(patientId);
        for (PatientDeposit deposit : deposits) {
            // Add deposit transaction line
            PatientInvoicePrintDto.TransactionLine depositLine = new PatientInvoicePrintDto.TransactionLine();
            depositLine.date = deposit.getDepositDate();
            depositLine.description = "Patient Deposit" + (deposit.getDescription() != null ? 
                    " - " + deposit.getDescription() : "");
            depositLine.credit = deposit.getAmount();
            depositLine.transactionType = "PATIENT_DEPOSIT";
            runningBalance = runningBalance.subtract(deposit.getAmount() != null ?
                    deposit.getAmount() : BigDecimal.ZERO);
            depositLine.balance = runningBalance;
            transactions.add(depositLine);
            
            // Store deposit detail
            PatientInvoicePrintDto.PatientDepositDetail depositDetail = new PatientInvoicePrintDto.PatientDepositDetail();
            depositDetail.depositId = deposit.getId();
            depositDetail.depositDate = deposit.getDepositDate();
            depositDetail.description = deposit.getDescription();
            depositDetail.paymentMethod = deposit.getPaymentMethod();
            depositDetail.amount = deposit.getAmount();
            depositDetails.add(depositDetail);
        }
        dto.patientDeposits = depositDetails;
        
        // 7. Add Courtesy Credits
        List<PatientInvoicePrintDto.CourtesyCreditDetail> courtesyCreditDetails = new ArrayList<>();
        List<InvoiceCourtesyCredit> courtesyCredits = invoiceCourtesyCreditRepo
                .findByInvoiceIdAndIsActiveAndIsDeletedOrderByCreatedDateDesc(invoiceId, true, false);
        for (InvoiceCourtesyCredit credit : courtesyCredits) {
            // Add courtesy credit transaction line
            PatientInvoicePrintDto.TransactionLine creditLine = new PatientInvoicePrintDto.TransactionLine();
            creditLine.date = credit.getCreatedDate() != null ?
                    credit.getCreatedDate().toLocalDate() : LocalDate.now();
            creditLine.description = "Courtesy Credit" + (credit.getAdjustmentType() != null ?
                    " - " + credit.getAdjustmentType() : "");
            creditLine.credit = credit.getAmount();
            creditLine.transactionType = "COURTESY_CREDIT";
            runningBalance = runningBalance.subtract(credit.getAmount() != null ?
                    credit.getAmount() : BigDecimal.ZERO);
            creditLine.balance = runningBalance;
            transactions.add(creditLine);
            
            // Store courtesy credit detail
            PatientInvoicePrintDto.CourtesyCreditDetail creditDetail = new PatientInvoicePrintDto.CourtesyCreditDetail();
            creditDetail.creditId = credit.getId();
            creditDetail.creditDate = creditLine.date;
            creditDetail.adjustmentType = credit.getAdjustmentType();
            creditDetail.description = credit.getDescription();
            creditDetail.amount = credit.getAmount();
            courtesyCreditDetails.add(creditDetail);
        }
        dto.courtesyCredits = courtesyCreditDetails;

        dto.transactions = transactions;

        // Financial Summary
        PatientInvoicePrintDto.FinancialSummary financialSummary = new PatientInvoicePrintDto.FinancialSummary();
        financialSummary.totalCharges = invoice.getTotalCharge() != null ? invoice.getTotalCharge() : BigDecimal.ZERO;

        BigDecimal totalInsPaid = insurancePayments.stream()
                .map(r -> r.getInsPay() != null ? r.getInsPay() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        financialSummary.totalInsurancePayments = totalInsPaid;

        // Calculate total patient payments (from payment map)
        BigDecimal totalPtPaid = paymentMap.values().stream()
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Add deposits to patient payments total
        BigDecimal totalDeposits = deposits.stream()
                .map(d -> d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalPtPaid = totalPtPaid.add(totalDeposits);
        
        financialSummary.totalPatientPayments = totalPtPaid;

        BigDecimal totalAdjustments = insurancePayments.stream()
                .map(r -> r.getInsWriteOff() != null ? r.getInsWriteOff() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Add courtesy credits to adjustments
        BigDecimal totalCourtesyCredits = courtesyCredits.stream()
                .map(c -> c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalAdjustments = totalAdjustments.add(totalCourtesyCredits);
        
        financialSummary.totalAdjustment = totalAdjustments.negate(); // Show as negative

        financialSummary.outstandingBalance = invoice.getPtBalance() != null ?
                invoice.getPtBalance() : BigDecimal.ZERO;

        financialSummary.estimatedRemainingInsurance = BigDecimal.ZERO;
        financialSummary.estimatedRemainingInsuranceAdjustment = BigDecimal.ZERO;

        dto.financialSummary = financialSummary;

        // Aging Summary (Your Portion)
        PatientInvoicePrintDto.AgingSummary agingSummary = new PatientInvoicePrintDto.AgingSummary();
        agingSummary.balance0_30 = financialSummary.outstandingBalance;
        agingSummary.balance30_60 = BigDecimal.ZERO;
        agingSummary.balance60_90 = BigDecimal.ZERO;
        agingSummary.balance90plus = BigDecimal.ZERO;

        Optional<PatientAccountCredit> accountCredit = creditRepo.findByPatientId(patientId);
        agingSummary.accountCredit = accountCredit.map(ac -> ac.getBalance() != null ?
                ac.getBalance() : BigDecimal.ZERO).orElse(BigDecimal.ZERO);

        dto.agingSummary = agingSummary;

        // Appointment Information - get from Appointment repository
        PatientInvoicePrintDto.AppointmentInfo appointments = new PatientInvoicePrintDto.AppointmentInfo();
        List<Appointment> patientAppointments = appointmentRepo.findAllByPatientId(patientId);

        String nextTreatment = "No Scheduled Appointment";
        String nextHygiene = "No Scheduled Appointment";

        for (Appointment apt : patientAppointments) {
            if (apt.getAppointmentStartDate() != null && apt.getAppointmentStartTime() != null) {
                String aptDate = apt.getAppointmentStartDate();
                String aptTime = apt.getAppointmentStartTime();
                String providerName = apt.getProviderId() != null ? "Provider" : "";
                String aptInfo = aptDate + " " + aptTime + " with " + providerName;

                // Categorize by visit type
                if (apt.getVisitType() != null) {
                    if (apt.getVisitType().toLowerCase().contains("hygiene") ||
                            apt.getVisitType().toLowerCase().contains("cleaning")) {
                        if (nextHygiene.equals("No Scheduled Appointment")) {
                            nextHygiene = aptInfo;
                        }
                    } else {
                        if (nextTreatment.equals("No Scheduled Appointment")) {
                            nextTreatment = aptInfo;
                        }
                    }
                }
            }
        }

        appointments.nextScheduledTreatment = nextTreatment;
        appointments.nextScheduledHygiene = nextHygiene;
        dto.appointments = appointments;

        // Notes
        List<PatientBillingNote> notes = noteRepo.findByPatientId(patientId);
        dto.notes = notes.stream().map(PatientBillingNote::getText).toList();

        return dto;
    }

    /* ===================== Helpers ===================== */

    private PatientInvoice getInvoiceOrThrow(Long patientId, Long invoiceId) {
        return invoiceRepo.findByIdAndPatientId(invoiceId, patientId).orElseThrow();
    }

    public PatientClaim getClaimOrThrow(Long patientId, Long invoiceId) {
        return claimRepo.findByInvoiceIdAndPatientId(invoiceId, patientId).orElseThrow();
    }

    /**
     * Get the primary facility or the first active facility.
     * Returns null if no facility is found.
     */
    private Facility getPrimaryFacility() {
        // Try to find primary business entity first
        List<Facility> facilities = facilityRepo.findAllByIsActiveTrue();

        // Find primary business entity
        Optional<Facility> primary = facilities.stream()
                .filter(f -> Boolean.TRUE.equals(f.getPrimaryBusinessEntity()))
                .findFirst();

        if (primary.isPresent()) {
            return primary.get();
        }

        // If no primary, return first active facility
        return facilities.isEmpty() ? null : facilities.get(0);
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private void addCredit(Long patientId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        PatientAccountCredit credit = creditRepo.findByPatientId(patientId)
                .orElseGet(() -> {
                    PatientAccountCredit ac = new PatientAccountCredit();
                    ac.setPatientId(patientId);
                    ac.setBalance(BigDecimal.ZERO);
                    return creditRepo.save(ac);
                });
        credit.setBalance(nz(credit.getBalance()).add(amount));
        creditRepo.save(credit);
    }

    private PatientInvoiceLineDto toInvoiceLineDto(PatientInvoiceLine l) {
        return new PatientInvoiceLineDto(
                l.getId(), l.getDos(), l.getCode(), l.getTreatment(), l.getProvider(),
                l.getCharge(), l.getAllowed(), l.getInsWriteOff(), l.getInsPortion(), l.getPatientPortion()
        );
    }

    private PatientInvoiceDto toInvoiceDto(PatientInvoice inv) {
        var lines = inv.getLines().stream().map(this::toInvoiceLineDto).toList();
        return new PatientInvoiceDto(
                inv.getId(), inv.getPatientId(), inv.getStatus(),
                inv.getInsWO(), inv.getPtBalance(), inv.getInsBalance(), inv.getTotalCharge(), lines
        );
    }

    public PatientClaimDto toClaimDto(PatientClaim c) {
        return new PatientClaimDto(
                c.getId(), c.getInvoiceId(), c.getPatientId(), c.getPayerName(),
                c.getTreatingProviderId(), c.getBillingEntity(), c.getType(), c.getNotes(),
                c.getStatus(), c.getAttachments(), c.isEobAttached(), c.getCreatedOn(),
                c.getAttachmentFile() != null, c.getEobFile() != null, c.getPatientName(),
                c.getPlanName(), c.getProvider(), c.getPolicyNumber()
        );
    }

    // --- Attachment & EOB upload/download ---
    public void uploadClaimAttachment(Long patientId, Long claimId, MultipartFile file) throws Exception {
        PatientClaim claim = claimRepo.findById(claimId).orElseThrow();
        claim.setAttachmentFile(file.getBytes());
        claim.setAttachments(claim.getAttachments() + 1);
    }
    public byte[] getClaimAttachment(Long patientId, Long claimId) {
        PatientClaim claim = claimRepo.findById(claimId).orElseThrow();
        return claim.getAttachmentFile();
    }
    public void uploadClaimEob(Long patientId, Long claimId, MultipartFile file) throws Exception {
        PatientClaim claim = claimRepo.findById(claimId).orElseThrow();
        claim.setEobFile(file.getBytes());
        claim.setEobAttached(true);
    }
    public byte[] getClaimEob(Long patientId, Long claimId) {
        PatientClaim claim = claimRepo.findById(claimId).orElseThrow();
        return claim.getEobFile();
    }

    private PatientInsuranceRemitLineDto toRemitDto(PatientInsuranceRemitLine e) {
        return new PatientInsuranceRemitLineDto(
                e.getId(),
                e.getInvoiceLineId(),
                e.getSubmitted(),
                e.getBalance(),
                e.getDeductible(),
                e.getAllowed(),
                e.getInsWriteOff(),
                e.getInsPay(),
                e.getUpdateAllowed(),
                e.getUpdateFlatPortion(),
                e.getApplyWriteoff()
        );
    }

    private PatientPaymentDto toPaymentDto(PatientPayment payment) {
        return new PatientPaymentDto(
                payment.getId(),
                payment.getPatientId(),
                payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null,
                payment.getAmount(),
                payment.getCreatedAt()
        );
    }


    /**
     * Helper method to convert entity to DTO
     */
    private PatientDepositDto toDepositDto(PatientDeposit deposit) {
        return new PatientDepositDto(
                deposit.getId(),
                deposit.getPatientId(),
                deposit.getAmount(),
                deposit.getDepositDate(),
                deposit.getDescription(),
                deposit.getPaymentMethod()
        );
    }



}

