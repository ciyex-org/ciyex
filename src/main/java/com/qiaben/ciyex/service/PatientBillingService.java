package com.qiaben.ciyex.service;


import com.qiaben.ciyex.dto.*;
import com.qiaben.ciyex.entity.*;
import com.qiaben.ciyex.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

    private final PatientInvoiceRepository invoiceRepo;
    private final PatientInvoiceLineRepository lineRepo;
    private final PatientClaimRepository claimRepo;
    private final PatientInsuranceRemitLineRepository remitRepo;
    private final PatientAccountCreditRepository creditRepo;

    private final PatientPaymentAllocationRepository allocationRepo;
    private final PatientPaymentRepository paymentRepo;
    private final PatientInvoiceLineRepository invoiceLineRepo;
    private final PatientBillingNoteRepository noteRepo;

    /* ====== Request DTOs ====== */
    public record CreateInvoiceRequest(String code, String description, String provider, String dos, BigDecimal rate) {}
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
        public PatientInvoiceDto backdateInvoice(Long orgId, Long patientId, Long invoiceId, BackdateRequest req) {
            PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
            if (req != null && req.date() != null) {
                invoice.setBackdate(LocalDate.parse(req.date()));
                invoiceRepo.save(invoice);
            }
            return toInvoiceDto(invoice);
        }

    public PatientAccountCreditDto accountAdjustment(Long orgId, Long patientId, AccountAdjustmentRequest req) {
        if (req == null || req.adjustmentType() == null) {
            throw new IllegalArgumentException("Adjustment type is required");
        }

        PatientAccountCredit credit = creditRepo.findByOrgIdAndPatientId(orgId, patientId)
                .orElseGet(() -> {
                    PatientAccountCredit c = new PatientAccountCredit();
                    c.setOrgId(orgId);
                    c.setPatientId(patientId);
                    c.setBalance(BigDecimal.ZERO);
                    return c;
                });

        BigDecimal adjustmentAmount = BigDecimal.ZERO;

        switch (req.adjustmentType()) {
            case "Flat-rate" -> adjustmentAmount = nz(req.flatRate());
            case "Total Outstanding" -> {
                // Calculate total outstanding from all patient invoices
                List<PatientInvoice> invoices = invoiceRepo.findByOrgIdAndPatientIdOrderByIdDesc(orgId, patientId);
                BigDecimal totalOutstanding = invoices.stream()
                        .map(inv -> nz(inv.getPtBalance()).add(nz(inv.getInsBalance())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                adjustmentAmount = totalOutstanding;
            }
            case "Patient Outstanding" -> {
                // Calculate only patient portion outstanding
                List<PatientInvoice> invoices = invoiceRepo.findByOrgIdAndPatientIdOrderByIdDesc(orgId, patientId);
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



    public PatientInvoiceDto adjustInvoice(Long orgId, Long patientId, Long invoiceId, InvoiceAdjustmentRequest req) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);

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
            addCredit(orgId, patientId, req.adjustmentAmount());
        }

        // Recalculate invoice totals
        invoice.recalcTotals();
        invoiceRepo.save(invoice);

        log.info("Invoice adjusted: invoiceId={}, type={}, discount={}%, amount={}",
                invoiceId, req.adjustmentType(), req.percentageDiscount(), req.adjustmentAmount());

        return toInvoiceDto(invoice);
    }

    public List<PatientInvoiceDto> listInvoices(Long orgId, Long patientId) {
        return invoiceRepo.findByOrgIdAndPatientIdOrderByIdDesc(orgId, patientId)
                .stream().map(this::toInvoiceDto).toList();
    }

    public PatientInvoiceDto createInvoiceFromProcedure(Long orgId, Long patientId, CreateInvoiceRequest b) {
        if (b == null) throw new IllegalArgumentException("Body required");

        PatientInvoice invoice = new PatientInvoice();
        invoice.setOrgId(orgId);
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
        claim.setOrgId(orgId);
        claim.setPatientId(patientId);
        claim.setInvoiceId(invoice.getId());
        claim.setStatus(PatientClaim.Status.DRAFT);
        claim.setCreatedOn(LocalDate.parse(b.dos()));
        claimRepo.save(claim);

        return toInvoiceDto(invoice);
    }

    public PatientInvoiceDto updateInvoiceLineAmount(Long orgId, Long patientId, Long invoiceId, Long lineId, UpdateLineAmountRequest b) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
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

    public PatientInvoiceDto applyInvoicePercentageAdjustment(Long orgId, Long patientId, Long invoiceId, PercentageAdjustmentRequest b) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
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
    public void deleteInvoice(Long orgId, Long patientId, Long invoiceId) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
        invoiceRepo.delete(invoice);
    }

    /* ===================== Claims ===================== */

    public List<PatientClaimDto> listAllClaimsForPatient(Long orgId, Long patientId) {
        List<PatientClaim> claims;
        try {
            claims = claimRepo.findAllByOrgIdAndPatientIdOrderByIdDesc(orgId, patientId);
        } catch (NoSuchMethodError | RuntimeException e) {
            claims = claimRepo.findAllByOrgIdAndPatientId(orgId, patientId);
        }
        return claims.stream().map(this::toClaimDto).toList();
    }

    public PatientClaimDto getActiveClaimForInvoice(Long orgId, Long patientId, Long invoiceId) {
        return toClaimDto(getClaimOrThrow(orgId, patientId, invoiceId));
    }

    public List<PatientClaimDto> listClaimsForInvoice(Long orgId, Long patientId, Long invoiceId) {
        List<PatientClaim> claims;
        try {
            claims = claimRepo.findAllByInvoiceIdAndOrgIdAndPatientIdOrderByIdDesc(invoiceId, orgId, patientId);
        } catch (NoSuchMethodError | RuntimeException e) {
            claims = claimRepo.findAllByInvoiceIdAndOrgIdAndPatientId(invoiceId, orgId, patientId);
        }
        return claims.stream().map(this::toClaimDto).toList();
    }

    public PatientClaimDto promoteClaim(Long orgId, Long patientId, Long invoiceId) {
        PatientClaim c = getClaimOrThrow(orgId, patientId, invoiceId);
        if (c.getStatus() == PatientClaim.Status.DRAFT) c.setStatus(PatientClaim.Status.IN_PROCESS);
        return toClaimDto(c);
    }

    public PatientClaimDto sendClaimToBatch(Long orgId, Long patientId, Long invoiceId) {
        PatientClaim c = getClaimOrThrow(orgId, patientId, invoiceId);
        c.setStatus(PatientClaim.Status.READY_FOR_SUBMISSION);
        return toClaimDto(c);
    }

    public PatientClaimDto submitClaim(Long orgId, Long patientId, Long invoiceId) {
        PatientClaim c = getClaimOrThrow(orgId, patientId, invoiceId);
        c.setStatus(PatientClaim.Status.SUBMITTED);
        return toClaimDto(c);
    }

    public PatientClaimDto closeClaim(Long orgId, Long patientId, Long invoiceId) {
        PatientClaim c = getClaimOrThrow(orgId, patientId, invoiceId);
        c.setStatus(PatientClaim.Status.CLOSED);
        return toClaimDto(c);
    }

    public PatientClaimDto voidAndRecreateClaim(Long orgId, Long patientId, Long invoiceId) {
        PatientClaim existing = getClaimOrThrow(orgId, patientId, invoiceId);
        existing.setStatus(PatientClaim.Status.VOID);

        PatientClaim fresh = new PatientClaim();
        fresh.setOrgId(orgId);
        fresh.setPatientId(patientId);
        fresh.setInvoiceId(invoiceId);
        fresh.setStatus(PatientClaim.Status.DRAFT);
        fresh.setCreatedOn(LocalDate.now());
        claimRepo.save(fresh);

        return toClaimDto(fresh);
    }

    public PatientClaimDto updateClaim(Long orgId, Long patientId, Long invoiceId, PatientClaimCoreUpdate p) {
        PatientClaim c = getClaimOrThrow(orgId, patientId, invoiceId);
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
    





    /* ================ Insurance Payment ================ */

    public PatientInvoiceDto applyInsurancePayment(Long orgId, Long patientId, Long invoiceId, PatientInsurancePaymentRequestDto req) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);

        if (req != null && req.lines() != null) {
            for (PatientInsuranceRemitLineDto r : req.lines()) {
                // 1) persist remit row
                PatientInsuranceRemitLine e = new PatientInsuranceRemitLine();
                e.setOrgId(orgId);
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
        claimRepo.findByInvoiceIdAndOrgIdAndPatientId(invoiceId, orgId, patientId)
                .ifPresent(c -> c.setStatus(
                        invoice.getPtBalance().compareTo(BigDecimal.ZERO) == 0
                                ? PatientClaim.Status.ACCEPTED
                                : PatientClaim.Status.IN_PROCESS));

        return toInvoiceDto(invoice);
    }

    public List<PatientInsuranceRemitLineDto> listInsurancePayments(
            Long orgId, Long patientId, Long invoiceId, Long claimId, Long insuranceId) {

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
                    ? remitRepo.findAllByOrgIdAndPatientIdAndInvoiceIdOrderByIdDesc(orgId, patientId, invoiceId)
                    : remitRepo.findAllByOrgIdAndPatientIdOrderByIdDesc(orgId, patientId);
        } catch (RuntimeException e) {
            rows = remitRepo.findAllByPatientId(patientId);
        }

        return rows.stream().map(this::toRemitDto).toList();
    }

    /** EDIT */
    public PatientInvoiceDto editInsuranceRemitLine(Long orgId, Long patientId, Long invoiceId, Long remitId, PatientInsuranceRemitLineDto dto) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
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
    public PatientInvoiceDto voidInsurancePayment(Long orgId, Long patientId, Long invoiceId, Long remitId, VoidReason reason) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
        remitRepo.deleteById(remitId);
        invoice.recalcTotals(); // recompute totals from remaining lines + their portions
        return toInvoiceDto(invoice);
    }

    /** REFUND insurance → increase insurance balance (reduce paid) */
    public PatientInvoiceDto refundInsurancePayment(Long orgId, Long patientId, Long invoiceId, Long remitId, RefundRequest req) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
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
        refundRow.setOrgId(orgId);
        refundRow.setPatientId(patientId);
        refundRow.setInvoiceId(invoiceId);
        refundRow.setInvoiceLineId(line.getId());
        refundRow.setInsPay(amount.negate());
        remitRepo.save(refundRow);

        invoice.recalcTotals();
        return toInvoiceDto(invoice);
    }

    /** TRANSFER insurance balance → patient account credit */
    public PatientInvoiceDto transferInsuranceCreditToPatient(Long orgId, Long patientId, Long invoiceId, Long remitId, TransferCreditRequest req) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
        BigDecimal amount = Optional.ofNullable(req).map(TransferCreditRequest::amount)
                .orElseThrow(() -> new IllegalArgumentException("Transfer amount required"));
        if (amount.signum() <= 0) throw new IllegalArgumentException("Transfer amount must be > 0");

        if (invoice.getLines() == null || invoice.getLines().isEmpty()) {
            throw new IllegalStateException("No invoice lines to transfer from");
        }
        PatientInvoiceLine line = invoice.getLines().get(0);

        // reduce insurer portion (can't go below zero)
        BigDecimal newInsPortion = nz(line.getInsPortion()).subtract(amount);
        if (newInsPortion.signum() < 0) throw new IllegalArgumentException("Transfer exceeds insurance balance");
        line.setInsPortion(newInsPortion);
        lineRepo.save(line);

        // credit patient account
        addCredit(orgId, patientId, amount);

        // audit: create remit row with negative insPay to mirror the transfer out of invoice
        PatientInsuranceRemitLine adj = new PatientInsuranceRemitLine();
        adj.setOrgId(orgId);
        adj.setPatientId(patientId);
        adj.setInvoiceId(invoiceId);
        adj.setInvoiceLineId(line.getId());
        adj.setInsPay(amount.negate());
        remitRepo.save(adj);

        invoice.recalcTotals();
        return toInvoiceDto(invoice);
    }

    /* ================ Patient Payment & Credit ================ */

    public PatientInvoiceDto applyPatientPayment(Long orgId, Long patientId, Long invoiceId, PatientPatientPaymentRequestDto req) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);

        BigDecimal outstanding = nz(invoice.getInsBalance()).add(nz(invoice.getPtBalance()));
        BigDecimal entered = (req == null || req.allocations() == null)
                ? BigDecimal.ZERO
                : req.allocations().stream()
                .map(a -> nz(a.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // If nothing is outstanding, entire payment becomes account credit
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            addCredit(orgId, patientId, entered);
            invoice.setPtBalance(BigDecimal.ZERO);
            invoice.setInsBalance(BigDecimal.ZERO);
            invoice.setStatus(PatientInvoice.Status.PAID);
            invoiceRepo.save(invoice);
            return toInvoiceDto(invoice);
        }

        BigDecimal cover = entered.min(outstanding);
        BigDecimal remaining = outstanding.subtract(cover);

        // Use account credit if available
        PatientAccountCredit credit = creditRepo.findByOrgIdAndPatientId(orgId, patientId).orElse(null);
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
        if (over.compareTo(BigDecimal.ZERO) > 0) addCredit(orgId, patientId, over);

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

    public List<PatientPatientPaymentAllocationDto> getAllPatientPayments(Long orgId, Long patientId) {
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

    public List<PatientPatientPaymentAllocationDto> getPatientPaymentsByInvoice(Long orgId, Long patientId, Long invoiceId) {
        getInvoiceOrThrow(orgId, patientId, invoiceId);
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

    public PatientInvoiceDto editPatientPayment(Long orgId, Long patientId, Long invoiceId, Long paymentId, PatientPaymentDto dto) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
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
    public PatientInvoiceDto voidPatientPayment(Long orgId, Long patientId, Long invoiceId, Long paymentId, VoidReason reason) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
        List<PatientPaymentAllocation> allocs = allocationRepo.findByPaymentId(paymentId);
        allocs.forEach(allocationRepo::delete);
        paymentRepo.deleteById(paymentId);
        invoice.recalcTotals();
        return toInvoiceDto(invoice);
    }

    /** REFUND patient → add to patient account credit */
    public PatientInvoiceDto refundPatientPayment(Long orgId, Long patientId, Long invoiceId, Long paymentId, RefundRequest req) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
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

        addCredit(orgId, patientId, amount);
        invoice.recalcTotals();
        return toInvoiceDto(invoice);
    }

    public PatientAccountCreditDto[] transferPatientCreditToPatient(Long orgId, Long fromPatientId, Long toPatientId, BigDecimal amount) {
        if (fromPatientId.equals(toPatientId)) throw new IllegalArgumentException("Source and destination patients must differ");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive");

        PatientAccountCredit fromCredit = creditRepo.findByOrgIdAndPatientId(orgId, fromPatientId)
                .orElseThrow(() -> new IllegalArgumentException("Source patient has no credit account"));
        if (fromCredit.getBalance().compareTo(amount) < 0) throw new IllegalArgumentException("Insufficient credit in source account");

        PatientAccountCredit toCredit = creditRepo.findByOrgIdAndPatientId(orgId, toPatientId)
                .orElseGet(() -> {
                    PatientAccountCredit ac = new PatientAccountCredit();
                    ac.setOrgId(orgId);
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

    public PatientAccountCreditDto getAccountCredit(Long orgId, Long patientId) {
        PatientAccountCredit c = creditRepo.findByOrgIdAndPatientId(orgId, patientId)
                .orElseGet(() -> {
                    PatientAccountCredit ac = new PatientAccountCredit();
                    ac.setOrgId(orgId);
                    ac.setPatientId(patientId);
                    ac.setBalance(BigDecimal.ZERO);
                    return creditRepo.save(ac);
                });
        return new PatientAccountCreditDto(patientId, c.getBalance());
    }

    public PatientAccountCreditDto applyAccountCredit(Long orgId, Long patientId, ApplyCreditRequest b) {
        BigDecimal amount = (b == null) ? BigDecimal.ZERO : nz(b.amount());
        PatientAccountCredit c = creditRepo.findByOrgIdAndPatientId(orgId, patientId).orElseThrow();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return new PatientAccountCreditDto(patientId, c.getBalance());
        if (c.getBalance().compareTo(amount) < 0) throw new IllegalArgumentException("Insufficient credit");
        c.setBalance(c.getBalance().subtract(amount));
        return new PatientAccountCreditDto(patientId, c.getBalance());
    }

    public PatientAccountCreditDto addPatientDeposit(Long orgId, Long patientId, PatientDepositRequest request) {
        if (request == null || request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        var creditOpt = creditRepo.findByOrgIdAndPatientId(orgId, patientId);
        var credit = creditOpt.orElseGet(() -> {
            var c = new PatientAccountCredit();
            c.setOrgId(orgId);
            c.setPatientId(patientId);
            c.setBalance(java.math.BigDecimal.ZERO);
            return c;
        });
        credit.setBalance(credit.getBalance().add(request.amount()));
        creditRepo.save(credit);
        return new PatientAccountCreditDto(patientId, credit.getBalance());
    }



    /* ===================== Notes ===================== */

    /**
     * List notes for a specific invoice (targetType=INVOICE, targetId=invoiceId)
     */
    public List<PatientBillingNoteDto> listInvoiceNotes(Long orgId, Long patientId, Long invoiceId) {
        // Verify invoice exists and belongs to patient
        getInvoiceOrThrow(orgId, patientId, invoiceId);

        return noteRepo.findByPatientIdAndTargetTypeAndTargetIdOrderByCreatedAtAsc(patientId, NoteTargetType.INVOICE, invoiceId)
                .stream()
                .map(PatientBillingNoteDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Create a note for a specific invoice
     */
    public PatientBillingNoteDto createInvoiceNote(Long orgId, Long patientId, Long invoiceId, PatientBillingNoteDto dto) {
        // Verify invoice exists and belongs to patient
        getInvoiceOrThrow(orgId, patientId, invoiceId);

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
    public PatientBillingNoteDto updateInvoiceNote(Long orgId, Long patientId, Long invoiceId, Long noteId, PatientBillingNoteDto dto) {
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
    public void deleteInvoiceNote(Long orgId, Long patientId, Long invoiceId, Long noteId) {
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
    public PatientAccountCreditDto addInsuranceDeposit(Long orgId, Long patientId, InsuranceDepositRequest request) {
        // Find or create PatientAccountCredit
        var credit = creditRepo.findByOrgIdAndPatientId(orgId, patientId)
                .orElseGet(() -> {
                    var c = new PatientAccountCredit();
                    c.setOrgId(orgId);
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
    public PatientAccountCreditDto addCourtesyCredit(Long orgId, Long patientId, CourtesyCreditRequest request) {
        // Find or create PatientAccountCredit
        var credit = creditRepo.findByOrgIdAndPatientId(orgId, patientId)
                .orElseGet(() -> {
                    var c = new PatientAccountCredit();
                    c.setOrgId(orgId);
                    c.setPatientId(patientId);
                    c.setBalance(java.math.BigDecimal.ZERO);
                    return c;
                });
        java.math.BigDecimal amount = request.amount() != null ? request.amount() : java.math.BigDecimal.ZERO;
        credit.setBalance(credit.getBalance().add(amount));
        creditRepo.save(credit);
        // TODO: Optionally, persist courtesy credit as a separate entity for audit/history
        return new PatientAccountCreditDto(patientId, credit.getBalance());
    }


    /* ===================== Helpers ===================== */

    private PatientInvoice getInvoiceOrThrow(Long orgId, Long patientId, Long invoiceId) {
        return invoiceRepo.findByIdAndOrgIdAndPatientId(invoiceId, orgId, patientId).orElseThrow();
    }

    private PatientClaim getClaimOrThrow(Long orgId, Long patientId, Long invoiceId) {
        return claimRepo.findByInvoiceIdAndOrgIdAndPatientId(invoiceId, orgId, patientId).orElseThrow();
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private void addCredit(Long orgId, Long patientId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        PatientAccountCredit credit = creditRepo.findByOrgIdAndPatientId(orgId, patientId)
                .orElseGet(() -> {
                    PatientAccountCredit ac = new PatientAccountCredit();
                    ac.setOrgId(orgId);
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

    private PatientClaimDto toClaimDto(PatientClaim c) {
        return new PatientClaimDto(
                c.getId(), c.getInvoiceId(), c.getPatientId(), c.getPayerName(),
                c.getTreatingProviderId(), c.getBillingEntity(), c.getType(), c.getNotes(),
                c.getStatus(), c.getAttachments(), c.isEobAttached(), c.getCreatedOn(),
                c.getAttachmentFile() != null, c.getEobFile() != null
        );
    }

    // --- Attachment & EOB upload/download ---
    public void uploadClaimAttachment(Long orgId, Long patientId, Long claimId, MultipartFile file) throws Exception {
        PatientClaim claim = claimRepo.findById(claimId).orElseThrow();
        claim.setAttachmentFile(file.getBytes());
        claim.setAttachments(claim.getAttachments() + 1);
    }
    public byte[] getClaimAttachment(Long orgId, Long patientId, Long claimId) {
        PatientClaim claim = claimRepo.findById(claimId).orElseThrow();
        return claim.getAttachmentFile();
    }
    public void uploadClaimEob(Long orgId, Long patientId, Long claimId, MultipartFile file) throws Exception {
        PatientClaim claim = claimRepo.findById(claimId).orElseThrow();
        claim.setEobFile(file.getBytes());
        claim.setEobAttached(true);
    }
    public byte[] getClaimEob(Long orgId, Long patientId, Long claimId) {
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


}
