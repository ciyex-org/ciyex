//package com.qiaben.ciyex.service;
//import com.qiaben.ciyex.dto.*;
//import com.qiaben.ciyex.entity.*;
//import com.qiaben.ciyex.repository.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;   // <-- add this
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class PatientBillingService {
//
//    private final PatientInvoiceRepository invoiceRepo;
//    private final PatientInvoiceLineRepository lineRepo;
//    private final PatientClaimRepository claimRepo;
//    private final PatientInsuranceRemitLineRepository remitRepo;
//    private final PatientAccountCreditRepository creditRepo;
//
//
//
//    /* ====== Request DTOs for JSON bodies (simple and explicit) ====== */
//    public record CreateInvoiceRequest(String code, String description, String provider, String dos, BigDecimal rate) {}
//    public record UpdateLineAmountRequest(BigDecimal newCharge) {}
//    public record PercentageAdjustmentRequest(int percent) {}
//    public record ApplyCreditRequest(BigDecimal amount) {}
//    /** Minimal compose payload for Claim header panel */
//    public record PatientClaimCoreUpdate(
//            String treatingProviderId,
//            String billingEntity,
//            String type,
//            String notes,
//            String attachmentIndicator,
//            String attachmentType,
//            String attachmentTransmissionCode,
//            String claimSubmissionReasonCode
//    ) {}
//
//    /* ===================== Invoices ===================== */
//
//    public List<PatientInvoiceDto> listInvoices(Long orgId, Long patientId) {
//        return invoiceRepo.findByOrgIdAndPatientIdOrderByIdDesc(orgId, patientId)
//                .stream().map(this::toInvoiceDto).toList();
//    }
//
//    public PatientInvoiceDto createInvoiceFromProcedure(Long orgId, Long patientId, CreateInvoiceRequest b) {
//        if (b == null) throw new IllegalArgumentException("Body required");
//        PatientInvoice invoice = new PatientInvoice();
//        invoice.setOrgId(orgId);
//        invoice.setPatientId(patientId);
//        invoice.setStatus(PatientInvoice.Status.OPEN);
//
//        PatientInvoiceLine line = new PatientInvoiceLine();
//        line.setInvoice(invoice);
//        line.setCode(b.code());
//        line.setTreatment(b.description());
//        line.setProvider(b.provider());
//        line.setDos(LocalDate.parse(b.dos()));
//        line.setCharge(nz(b.rate()));
//        line.setAllowed(nz(b.rate()));
//        // seed split to match UI expectations
//        line.setInsPortion(nz(b.rate()).multiply(new BigDecimal("0.82")));
//        line.setPatientPortion(nz(b.rate()).multiply(new BigDecimal("0.18")));
//        invoice.getLines().add(line);
//
//        invoice.recalcTotals();
//        invoiceRepo.save(invoice);
//
//        // seed claim in DRAFT
//        PatientClaim claim = new PatientClaim();
//        claim.setOrgId(orgId);
//        claim.setPatientId(patientId);
//        claim.setInvoiceId(invoice.getId());
//        claim.setStatus(PatientClaim.Status.DRAFT);
//        claim.setCreatedOn(LocalDate.parse(b.dos()));
//        claimRepo.save(claim);
//
//        return toInvoiceDto(invoice);
//    }
//
//    public PatientInvoiceDto updateInvoiceLineAmount(Long orgId, Long patientId, Long invoiceId, Long lineId, UpdateLineAmountRequest b) {
//        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
//        PatientInvoiceLine line = lineRepo.findById(lineId).orElseThrow();
//        if (!line.getInvoice().getId().equals(invoiceId)) throw new IllegalArgumentException("Line not in invoice");
//
//        BigDecimal amt = nz(b == null ? null : b.newCharge());
//        line.setCharge(amt);
//        line.setAllowed(amt);
//        line.setInsPortion(amt.multiply(new BigDecimal("0.82")));
//        line.setPatientPortion(amt.multiply(new BigDecimal("0.18")));
//        invoice.recalcTotals();
//
//        return toInvoiceDto(invoice);
//    }
//
//    public PatientInvoiceDto applyInvoicePercentageAdjustment(Long orgId, Long patientId, Long invoiceId, PercentageAdjustmentRequest b) {
//        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
//        int percent = (b == null) ? 0 : b.percent();
//        // add rounding mode to satisfy IDE/linter
//        BigDecimal p = BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
//
//        for (PatientInvoiceLine l : invoice.getLines()) {
//            BigDecimal delta = l.getCharge().multiply(p);
//            BigDecimal newCharge = l.getCharge().subtract(delta).max(BigDecimal.ZERO);
//            l.setCharge(newCharge);
//            l.setAllowed(newCharge);
//            l.setInsPortion(l.getInsPortion().subtract(delta).max(BigDecimal.ZERO));
//            l.setPatientPortion(l.getPatientPortion().subtract(delta).max(BigDecimal.ZERO));
//        }
//        invoice.recalcTotals();
//        return toInvoiceDto(invoice);
//    }
//
//    /* ===================== Claims ===================== */
//
//    public PatientClaimDto promoteClaim(Long orgId, Long patientId, Long invoiceId) {
//        PatientClaim c = getClaimOrThrow(orgId, patientId, invoiceId);
//        if (c.getStatus() == PatientClaim.Status.DRAFT) c.setStatus(PatientClaim.Status.IN_PROCESS);
//        return toClaimDto(c);
//    }
//
//    public PatientClaimDto sendClaimToBatch(Long orgId, Long patientId, Long invoiceId) {
//        PatientClaim c = getClaimOrThrow(orgId, patientId, invoiceId);
//        c.setStatus(PatientClaim.Status.READY_FOR_SUBMISSION);
//        return toClaimDto(c);
//    }
//
//    public PatientClaimDto submitClaim(Long orgId, Long patientId, Long invoiceId) {
//        PatientClaim c = getClaimOrThrow(orgId, patientId, invoiceId);
//        c.setStatus(PatientClaim.Status.SUBMITTED);
//        return toClaimDto(c);
//    }
//
//    public PatientClaimDto closeClaim(Long orgId, Long patientId, Long invoiceId) {
//        PatientClaim c = getClaimOrThrow(orgId, patientId, invoiceId);
//        c.setStatus(PatientClaim.Status.CLOSED);
//        return toClaimDto(c);
//    }
//
//    public PatientClaimDto voidAndRecreateClaim(Long orgId, Long patientId, Long invoiceId) {
//        PatientClaim existing = getClaimOrThrow(orgId, patientId, invoiceId);
//        existing.setStatus(PatientClaim.Status.VOID);
//
//        PatientClaim fresh = new PatientClaim();
//        fresh.setOrgId(orgId);
//        fresh.setPatientId(patientId);
//        fresh.setInvoiceId(invoiceId);
//        fresh.setStatus(PatientClaim.Status.DRAFT);
//        fresh.setCreatedOn(LocalDate.now());
//        claimRepo.save(fresh);
//
//        return toClaimDto(fresh);
//    }
//
//    public PatientClaimDto updateClaim(Long orgId, Long patientId, Long invoiceId, PatientClaimCoreUpdate p) {
//        PatientClaim c = getClaimOrThrow(orgId, patientId, invoiceId);
//        if (p != null) {
//            c.setTreatingProviderId(p.treatingProviderId());
//            c.setBillingEntity(p.billingEntity());
//            c.setType(p.type());
//            c.setNotes(p.notes());
//            c.setAttachmentIndicator(p.attachmentIndicator());
//            c.setAttachmentType(p.attachmentType());
//            c.setAttachmentTransmissionCode(p.attachmentTransmissionCode());
//            c.setClaimSubmissionReasonCode(p.claimSubmissionReasonCode());
//        }
//        return toClaimDto(c);
//    }
//
//    /* ================ Insurance Payment ================ */
//
//    public PatientInvoiceDto applyInsurancePayment(Long orgId, Long patientId, Long invoiceId, PatientInsurancePaymentRequestDto req) {
//        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
//
//        if (req != null && req.lines() != null) {
//            for (PatientInsuranceRemitLineDto r : req.lines()) {
//                PatientInsuranceRemitLine e = new PatientInsuranceRemitLine();
//                e.setOrgId(orgId);
//                e.setPatientId(patientId);
//                e.setInvoiceId(invoiceId);
//                e.setInvoiceLineId(r.invoiceLineId());
//                e.setSubmitted(nz(r.submitted()));
//                e.setBalance(nz(r.balance()));
//                e.setDeductible(nz(r.deductible()));
//                e.setAllowed(nz(r.allowed()));
//                e.setInsWriteOff(nz(r.insWriteOff()));
//                e.setInsPay(nz(r.insPay()));
//                e.setUpdateAllowed(r.updateAllowed());
//                e.setUpdateFlatPortion(r.updateFlatPortion());
//                e.setApplyWriteoff(r.applyWriteoff());
//                remitRepo.save(e);
//            }
//        }
//
//        BigDecimal submitted = (req == null || req.lines() == null) ? BigDecimal.ZERO
//                : req.lines().stream().map(x -> nz(x.submitted())).reduce(BigDecimal.ZERO, BigDecimal::add);
//        BigDecimal paid = (req == null || req.lines() == null) ? BigDecimal.ZERO
//                : req.lines().stream().map(x -> nz(x.insPay())).reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        invoice.setInsBalance(BigDecimal.ZERO);
//        invoice.setPtBalance(BigDecimal.ZERO);
//        invoice.setStatus(paid.compareTo(submitted) >= 0 ? PatientInvoice.Status.PAID : PatientInvoice.Status.PARTIALLY_PAID);
//
//        claimRepo.findByInvoiceIdAndOrgIdAndPatientId(invoiceId, orgId, patientId)
//                .ifPresent(c -> c.setStatus(invoice.getStatus() == PatientInvoice.Status.PAID
//                        ? PatientClaim.Status.ACCEPTED
//                        : PatientClaim.Status.IN_PROCESS));
//
//        return toInvoiceDto(invoice);
//    }
//
//    /* ================ Patient Payment & Credit ================ */
//
//    public PatientInvoiceDto applyPatientPayment(Long orgId, Long patientId, Long invoiceId, PatientPatientPaymentRequestDto req) {
//        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
//
//        BigDecimal outstanding = nz(invoice.getInsBalance()).add(nz(invoice.getPtBalance()));
//        BigDecimal entered = (req == null || req.allocations() == null) ? BigDecimal.ZERO
//                : req.allocations().stream().map(a -> nz(a.amount())).reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
//            addCredit(orgId, patientId, entered);   // <-- now implemented
//            return toInvoiceDto(invoice);
//        }
//
//        BigDecimal cover = entered.min(outstanding);
//        BigDecimal remaining = outstanding.subtract(cover);
//
//        invoice.setStatus(remaining.compareTo(BigDecimal.ZERO) <= 0 ? PatientInvoice.Status.PAID : PatientInvoice.Status.PARTIALLY_PAID);
//
//        BigDecimal over = entered.subtract(cover);
//        if (over.compareTo(BigDecimal.ZERO) > 0) addCredit(orgId, patientId, over);
//
//        return toInvoiceDto(invoice);
//    }
//
//    public PatientAccountCreditDto getAccountCredit(Long orgId, Long patientId) {
//        PatientAccountCredit c = creditRepo.findByOrgIdAndPatientId(orgId, patientId)
//                .orElseGet(() -> {
//                    PatientAccountCredit ac = new PatientAccountCredit();
//                    ac.setOrgId(orgId);
//                    ac.setPatientId(patientId);
//                    ac.setBalance(BigDecimal.ZERO);
//                    return creditRepo.save(ac);
//                });
//        return new PatientAccountCreditDto(patientId, c.getBalance());
//    }
//
//    public PatientAccountCreditDto applyAccountCredit(Long orgId, Long patientId, ApplyCreditRequest b) {
//        BigDecimal amount = (b == null) ? BigDecimal.ZERO : nz(b.amount());
//        PatientAccountCredit c = creditRepo.findByOrgIdAndPatientId(orgId, patientId).orElseThrow();
//        if (amount.compareTo(BigDecimal.ZERO) <= 0) return new PatientAccountCreditDto(patientId, c.getBalance());
//        if (c.getBalance().compareTo(amount) < 0) throw new IllegalArgumentException("Insufficient credit");
//        c.setBalance(c.getBalance().subtract(amount));
//        return new PatientAccountCreditDto(patientId, c.getBalance());
//    }
//
//    public void deleteInvoice(Long orgId, Long patientId, Long invoiceId) {
//        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
//        invoiceRepo.delete(invoice);
//    }
//
//    /* ===================== Helpers & mapping ===================== */
//
//    private PatientInvoice getInvoiceOrThrow(Long orgId, Long patientId, Long invoiceId) {
//        return invoiceRepo.findByIdAndOrgIdAndPatientId(invoiceId, orgId, patientId).orElseThrow();
//    }
//
//    private PatientClaim getClaimOrThrow(Long orgId, Long patientId, Long invoiceId) {
//        return claimRepo.findByInvoiceIdAndOrgIdAndPatientId(invoiceId, orgId, patientId).orElseThrow();
//    }
//
//    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
//
//    /** create-or-increment patient account credit */
//    private void addCredit(Long orgId, Long patientId, BigDecimal amount) {
//        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
//        PatientAccountCredit credit = creditRepo.findByOrgIdAndPatientId(orgId, patientId)
//                .orElseGet(() -> {
//                    PatientAccountCredit ac = new PatientAccountCredit();
//                    ac.setOrgId(orgId);
//                    ac.setPatientId(patientId);
//                    ac.setBalance(BigDecimal.ZERO);
//                    return creditRepo.save(ac);
//                });
//        credit.setBalance(nz(credit.getBalance()).add(amount));
//        // no need to explicitly save if entity is managed in Tx, but safe:
//        creditRepo.save(credit);
//    }
//
//    private PatientInvoiceDto toInvoiceDto(PatientInvoice inv) {
//        var lines = inv.getLines().stream().map(l ->
//                new PatientInvoiceLineDto(
//                        l.getId(), l.getDos(), l.getCode(), l.getTreatment(), l.getProvider(),
//                        l.getCharge(), l.getAllowed(), l.getInsWriteOff(), l.getInsPortion(), l.getPatientPortion()
//                )).toList();
//        return new PatientInvoiceDto(
//                inv.getId(), inv.getPatientId(), inv.getStatus(),
//                inv.getInsWO(), inv.getPtBalance(), inv.getInsBalance(), inv.getTotalCharge(), lines
//        );
//    }
//
//    private PatientClaimDto toClaimDto(PatientClaim c) {
//        return new PatientClaimDto(
//                c.getId(), c.getInvoiceId(), c.getPatientId(), c.getPayerName(),
//                c.getTreatingProviderId(), c.getBillingEntity(), c.getType(), c.getNotes(),
//                c.getStatus(), c.getAttachments(), c.isEobAttached(), c.getCreatedOn()
//        );
//    }
//
//
//
//
//
//}


package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.*;
import com.qiaben.ciyex.entity.*;
import com.qiaben.ciyex.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.math.RoundingMode;   // <-- keep this
import java.time.LocalDate;
import java.util.List;
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

    /* ====== Request DTOs for JSON bodies (simple and explicit) ====== */
    public record CreateInvoiceRequest(String code, String description, String provider, String dos, BigDecimal rate) {}
    public record UpdateLineAmountRequest(BigDecimal newCharge) {}
    public record PercentageAdjustmentRequest(int percent) {}
    public record ApplyCreditRequest(BigDecimal amount) {}
    /** Minimal compose payload for Claim header panel */
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


    /* ===================== Invoices ===================== */

    public List<PatientInvoiceDto> listInvoices(Long orgId, Long patientId) {
        return invoiceRepo.findByOrgIdAndPatientIdOrderByIdDesc(orgId, patientId)
                .stream().map(inv -> {
                    // Prefer tenant-safe lookup; fall back if older repo method exists
                    PatientClaim claim = claimRepo.findByInvoiceIdAndOrgIdAndPatientId(inv.getId(), orgId, patientId)
                            .orElseGet(() -> {
                                try { return claimRepo.findByInvoiceId(inv.getId()); }
                                catch (Exception ignored) { return null; }
                            });
                    PatientClaimDto claimDto = (claim != null) ? toClaimDto(claim) : null;
                    return new PatientInvoiceDto(
                            inv.getId(),
                            inv.getPatientId(),
                            inv.getStatus(),
                            inv.getInsWO(),
                            inv.getPtBalance(),
                            inv.getInsBalance(),
                            inv.getTotalCharge(),
                            inv.getLines().stream().map(this::toInvoiceLineDto).toList()
                            //PatientClaimDto
                    );
                }).toList();
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
//        line.setAllowed(nz(b.rate()));
//        // seed split to match UI expectations
//        line.setInsPortion(nz(b.rate()).multiply(new BigDecimal("0.82")));
//        line.setPatientPortion(nz(b.rate()).multiply(new BigDecimal("0.18")));

        line.setAllowed(nz(b.rate()));
// seed: all to insurance, patient starts at $0
        line.setInsPortion(nz(b.rate()));
        line.setPatientPortion(BigDecimal.ZERO);


        invoice.getLines().add(line);

        invoice.recalcTotals();
        invoiceRepo.save(invoice);

        // seed claim in DRAFT
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
//        line.setInsPortion(amt.multiply(new BigDecimal("0.82")));
//        line.setPatientPortion(amt.multiply(new BigDecimal("0.18")));

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
            l.setInsPortion(l.getInsPortion().subtract(delta).max(BigDecimal.ZERO));
            l.setPatientPortion(l.getPatientPortion().subtract(delta).max(BigDecimal.ZERO));
        }
        invoice.recalcTotals();
        return toInvoiceDto(invoice);
    }

    /* ===================== Claims ===================== */

    /** NEW: list all claims across all invoices for a patient */
    public List<PatientClaimDto> listAllClaimsForPatient(Long orgId, Long patientId) {
        // Expect repo method: findAllByOrgIdAndPatientIdOrderByIdDesc(...)
        List<PatientClaim> claims;
        try {
            claims = claimRepo.findAllByOrgIdAndPatientIdOrderByIdDesc(orgId, patientId);
        } catch (NoSuchMethodError | RuntimeException e) {
            // fallback without ordering if old repo
            claims = claimRepo.findAllByOrgIdAndPatientId(orgId, patientId);
        }
        return claims.stream().map(this::toClaimDto).toList();
    }

    /** NEW: get the active/latest claim for a given invoice */
    public PatientClaimDto getActiveClaimForInvoice(Long orgId, Long patientId, Long invoiceId) {
        PatientClaim c = getClaimOrThrow(orgId, patientId, invoiceId);
        return toClaimDto(c);
    }

    /** NEW (optional): list all (historical) claims for a specific invoice */
    public List<PatientClaimDto> listClaimsForInvoice(Long orgId, Long patientId, Long invoiceId) {
        // Expect repo method: findAllByInvoiceIdAndOrgIdAndPatientIdOrderByIdDesc(...)
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

//    public PatientInvoiceDto applyInsurancePayment(Long orgId, Long patientId, Long invoiceId, PatientInsurancePaymentRequestDto req) {
//        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
//
//        if (req != null && req.lines() != null) {
//            for (PatientInsuranceRemitLineDto r : req.lines()) {
//                PatientInsuranceRemitLine e = new PatientInsuranceRemitLine();
//                e.setOrgId(orgId);
//                e.setPatientId(patientId);
//                e.setInvoiceId(invoiceId);
//                e.setInvoiceLineId(r.invoiceLineId());
//                e.setSubmitted(nz(r.submitted()));
//                e.setBalance(nz(r.balance()));
//                e.setDeductible(nz(r.deductible()));
//                e.setAllowed(nz(r.allowed()));
//                e.setInsWriteOff(nz(r.insWriteOff()));
//                e.setInsPay(nz(r.insPay()));
//                e.setUpdateAllowed(r.updateAllowed());
//                e.setUpdateFlatPortion(r.updateFlatPortion());
//                e.setApplyWriteoff(r.applyWriteoff());
//                remitRepo.save(e);
//            }
//        }
//
//        BigDecimal submitted = (req == null || req.lines() == null) ? BigDecimal.ZERO
//                : req.lines().stream().map(x -> nz(x.submitted())).reduce(BigDecimal.ZERO, BigDecimal::add);
//        BigDecimal paid = (req == null || req.lines() == null) ? BigDecimal.ZERO
//                : req.lines().stream().map(x -> nz(x.insPay())).reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        invoice.setInsBalance(BigDecimal.ZERO);
//        invoice.setPtBalance(BigDecimal.ZERO);
//        invoice.setStatus(paid.compareTo(submitted) >= 0 ? PatientInvoice.Status.PAID : PatientInvoice.Status.PARTIALLY_PAID);
//
//        claimRepo.findByInvoiceIdAndOrgIdAndPatientId(invoiceId, orgId, patientId)
//                .ifPresent(c -> c.setStatus(invoice.getStatus() == PatientInvoice.Status.PAID
//                        ? PatientClaim.Status.ACCEPTED
//                        : PatientClaim.Status.IN_PROCESS));
//
//        return toInvoiceDto(invoice);
//    }





    public PatientInvoiceDto applyInsurancePayment(Long orgId, Long patientId, Long invoiceId, PatientInsurancePaymentRequestDto req) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);

        if (req != null && req.lines() != null) {
            for (PatientInsuranceRemitLineDto r : req.lines()) {
                // 1) persist remit line as before
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

                // 2) update the invoice line based on formulas
                PatientInvoiceLine line = lineRepo.findById(r.invoiceLineId()).orElseThrow();
                if (!line.getInvoice().getId().equals(invoiceId)) throw new IllegalArgumentException("Line not in invoice");

                BigDecimal submitted = nz(r.submitted());
                BigDecimal allowed   = nz(r.allowed());
                BigDecimal insPay    = nz(r.insPay());

                BigDecimal insWO  = submitted.subtract(allowed);
                if (insWO.compareTo(BigDecimal.ZERO) < 0) insWO = BigDecimal.ZERO;

                BigDecimal ptResp = allowed.subtract(insPay);
                if (ptResp.compareTo(BigDecimal.ZERO) < 0) ptResp = BigDecimal.ZERO;

                // Keep charge as submitted; set allowed/write-off; shift balance to patient
                line.setCharge(submitted);
                line.setAllowed(allowed);
                line.setInsWriteOff(insWO);
                line.setInsPortion(BigDecimal.ZERO);   // no remaining insurance balance after EOB math
                line.setPatientPortion(ptResp);        // patient responsibility from EOB
                lineRepo.save(line);
            }
        }

        // Re-total invoice from lines
        invoice.recalcTotals();

        // Mark claim status by whether anything is left for the patient
        invoice.setStatus(
                invoice.getPtBalance().compareTo(BigDecimal.ZERO) == 0
                        ? PatientInvoice.Status.PAID
                        : PatientInvoice.Status.PARTIALLY_PAID
        );

        // Keep auto-advance for claim
        claimRepo.findByInvoiceIdAndOrgIdAndPatientId(invoiceId, orgId, patientId)
                .ifPresent(c -> c.setStatus(
                        invoice.getStatus() == PatientInvoice.Status.PAID
                                ? PatientClaim.Status.ACCEPTED
                                : PatientClaim.Status.IN_PROCESS
                ));

        return toInvoiceDto(invoice);
    }


    public List<PatientInsuranceRemitLineDto> listInsurancePayments(
            Long orgId, Long patientId, Long invoiceId, Long claimId, Long insuranceId) {

        if (invoiceId == null && claimId != null) {
            try {
                PatientClaim c;
                try {
                    var claims = claimRepo.findAllByIdAndOrgIdAndPatientId(claimId, orgId, patientId);
                    c = (claims != null && !claims.isEmpty()) ? claims.get(0) : null;
                } catch (NoSuchMethodError e) {
                    c = claimRepo.findById(claimId).orElse(null);
                }
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
        } catch (NoSuchMethodError | RuntimeException e) {
            try {
                rows = (invoiceId != null)
                        ? remitRepo.findAllByOrgIdAndPatientIdAndInvoiceId(orgId, patientId, invoiceId)
                        : remitRepo.findAllByOrgIdAndPatientId(orgId, patientId);
            } catch (RuntimeException ex) {
                log.warn("Fallback remit query (no org filter) due to: {}", ex.getMessage());
                rows = remitRepo.findAllByPatientId(patientId);
            }
        }

        if (insuranceId != null) {
            log.info("insuranceId filter provided ({}), but entity lacks payer id; ignoring.", insuranceId);
        }

        return rows.stream().map(this::toRemitDto).toList();
    }






    /* ================ Patient Payment & Credit ================ */

//    public PatientInvoiceDto applyPatientPayment(Long orgId, Long patientId, Long invoiceId, PatientPatientPaymentRequestDto req) {
//        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
//
//        BigDecimal outstanding = nz(invoice.getInsBalance()).add(nz(invoice.getPtBalance()));
//        BigDecimal entered = (req == null || req.allocations() == null) ? BigDecimal.ZERO
//                : req.allocations().stream().map(a -> nz(a.amount())).reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
//            addCredit(orgId, patientId, entered);
//            return toInvoiceDto(invoice);
//        }
//
//        BigDecimal cover = entered.min(outstanding);
//        BigDecimal remaining = outstanding.subtract(cover);
//
//        invoice.setStatus(remaining.compareTo(BigDecimal.ZERO) <= 0 ? PatientInvoice.Status.PAID : PatientInvoice.Status.PARTIALLY_PAID);
//
//        BigDecimal over = entered.subtract(cover);
//        if (over.compareTo(BigDecimal.ZERO) > 0) addCredit(orgId, patientId, over);
//
//        return toInvoiceDto(invoice);
//    }
@Transactional
public PatientInvoiceDto applyPatientPayment(
        Long orgId, Long patientId, Long invoiceId, PatientPatientPaymentRequestDto req) {

    // 1️⃣ Fetch and validate invoice
    PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);

    BigDecimal outstanding = nz(invoice.getInsBalance()).add(nz(invoice.getPtBalance()));
    BigDecimal entered = (req == null || req.allocations() == null)
            ? BigDecimal.ZERO
            : req.allocations().stream()
            .map(a -> nz(a.amount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 2️⃣ Handle zero or overpayment as account credit
    if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
        addCredit(orgId, patientId, entered);
        return toInvoiceDto(invoice);
    }

    // 3️⃣ Compute coverage and remaining balance
    BigDecimal cover = entered.min(outstanding);
    BigDecimal remaining = outstanding.subtract(cover);

    invoice.setStatus(remaining.compareTo(BigDecimal.ZERO) <= 0
            ? PatientInvoice.Status.PAID
            : PatientInvoice.Status.PARTIALLY_PAID);

    // 4️⃣ Handle any overpayment
    BigDecimal over = entered.subtract(cover);
    if (over.compareTo(BigDecimal.ZERO) > 0) {
        addCredit(orgId, patientId, over);
    }

    // 5️⃣ Save payment first — ensures ID exists before allocation
    PatientPayment payment = new PatientPayment(
            patientId,
            PaymentMethod.valueOf(req.paymentMethod()),
            entered
    );
    PatientPayment savedPayment = paymentRepo.saveAndFlush(payment); // flush ensures persistence before allocation

    // 6️⃣ Save allocations linked to payment
    if (req.allocations() != null && !req.allocations().isEmpty()) {
        for (var allocReq : req.allocations()) {
            PatientInvoiceLine line = invoiceLineRepo.findById(allocReq.invoiceLineId())
                    .orElseThrow(() -> new RuntimeException("Invoice line not found: " + allocReq.invoiceLineId()));

            PatientPaymentAllocation alloc = new PatientPaymentAllocation(savedPayment, line, allocReq.amount());
            alloc.setPayment(savedPayment); // make sure relation is set
            allocationRepo.save(alloc);
        }
    }

    // 7️⃣ Persist invoice status updates
    invoiceRepo.save(invoice);

    // 8️⃣ Return latest DTO
    return toInvoiceDto(invoice);
}

    // -------------------- NEW METHODS -------------------- //

//    /** 🔹 Get all payments for patient */
//    public List<PatientPatientPaymentAllocationDto> getAllPatientPayments(Long orgId, Long patientId) {
//        var allocations = allocationRepo.findByPayment_PatientId(patientId);
//        return allocations.stream()
//                .map(a -> new PatientPatientPaymentAllocationDto(
//                        a.getId(),
//                        a.getInvoiceLine() != null ? a.getInvoiceLine().getId() : null,
//                        a.getAmount(),
//                        a.getPayment() != null ? a.getPayment().getPaymentMethod().name() : null,
//                        a.getCreatedAt()
//                ))
//                .collect(Collectors.toList());
//
//    }
//
//    /** 🔹 Get payments for a specific invoice */
//    public List<PatientPatientPaymentAllocationDto> getPatientPaymentsByInvoice(Long orgId, Long patientId, Long invoiceId) {
//        getInvoiceOrThrow(orgId, patientId, invoiceId);
//        var allocations = allocationRepo.findByInvoiceLine_Invoice_Id(invoiceId);
//        return allocations.stream()
//                .map(a -> new PatientPatientPaymentAllocationDto(
//                        a.getId(),
//                        a.getInvoiceLine() != null ? a.getInvoiceLine().getId() : null,
//                        a.getAmount(),
//                        a.getPayment() != null ? a.getPayment().getPaymentMethod().name() : null,
//                        a.getCreatedAt()
//                ))
//                .collect(Collectors.toList());
//
//    }



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

    public void deleteInvoice(Long orgId, Long patientId, Long invoiceId) {
        PatientInvoice invoice = getInvoiceOrThrow(orgId, patientId, invoiceId);
        invoiceRepo.delete(invoice);
    }













    /* ===================== Helpers & mapping ===================== */

    private PatientInvoice getInvoiceOrThrow(Long orgId, Long patientId, Long invoiceId) {
        return invoiceRepo.findByIdAndOrgIdAndPatientId(invoiceId, orgId, patientId).orElseThrow();
    }

    private PatientClaim getClaimOrThrow(Long orgId, Long patientId, Long invoiceId) {
        return claimRepo.findByInvoiceIdAndOrgIdAndPatientId(invoiceId, orgId, patientId).orElseThrow();
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    /** create-or-increment patient account credit */
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
        // Using the constructor without claim for most single-invoice responses
        return new PatientInvoiceDto(
                inv.getId(), inv.getPatientId(), inv.getStatus(),
                inv.getInsWO(), inv.getPtBalance(), inv.getInsBalance(), inv.getTotalCharge(), lines
        );
    }

    private PatientClaimDto toClaimDto(PatientClaim c) {
        return new PatientClaimDto(
                c.getId(), c.getInvoiceId(), c.getPatientId(), c.getPayerName(),
                c.getTreatingProviderId(), c.getBillingEntity(), c.getType(), c.getNotes(),
                c.getStatus(), c.getAttachments(), c.isEobAttached(), c.getCreatedOn()
        );
    }
    private PatientInsuranceRemitLineDto toRemitDto(PatientInsuranceRemitLine e) {
        return new PatientInsuranceRemitLineDto(
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

    public void uploadClaimAttachment(Long orgId, Long patientId, Long invoiceId, Long claimId, MultipartFile file) {
        // Validate claim exists
        PatientClaim claim = claimRepo.findById(claimId).orElseThrow(() -> new IllegalArgumentException("Claim not found"));
        // Null-safe increment for attachments
        Integer currentAttachments = claim.getAttachments();
        claim.setAttachments((currentAttachments != null ? currentAttachments : 0) + 1);
        claimRepo.save(claim);
    }

    public void uploadEobDocument(Long orgId, Long patientId, Long invoiceId, Long claimId, MultipartFile file) {
        // Validate claim exists
        PatientClaim claim = claimRepo.findById(claimId).orElseThrow(() -> new IllegalArgumentException("Claim not found"));
        // TODO: Store file (local, cloud, or DB)
        // For now, just set eob_attached to true
        claim.setEobAttached(true);
        claimRepo.save(claim);
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
