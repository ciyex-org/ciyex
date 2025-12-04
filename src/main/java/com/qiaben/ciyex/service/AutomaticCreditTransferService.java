package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.CreditTransferDto;
import com.qiaben.ciyex.entity.*;
import com.qiaben.ciyex.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AutomaticCreditTransferService {

    private final PatientAccountCreditRepository creditRepo;
    private final PatientInvoiceRepository invoiceRepo;

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

        // Find or create patient account credit
        PatientAccountCredit credit = creditRepo.findByPatientId(patientId)
                .orElseGet(() -> {
                    PatientAccountCredit newCredit = new PatientAccountCredit();
                    newCredit.setPatientId(patientId);
                    newCredit.setBalance(BigDecimal.ZERO);
                    return creditRepo.save(newCredit);
                });

        // Add overpayment to credit balance
        BigDecimal newBalance = credit.getBalance().add(overpaymentAmount);
        credit.setBalance(newBalance);
        creditRepo.save(credit);

        log.info("Automatic credit transfer completed: patientId={}, newCreditBalance={}", 
                patientId, newBalance);
        
        return CreditTransferDto.automatic(patientId, invoiceId, overpaymentAmount, newBalance);
    }

    /**
     * Check if invoice is fully paid and process any overpayment
     */
    public CreditTransferDto checkAndProcessOverpayment(Long patientId, Long invoiceId) {
        PatientInvoice invoice = invoiceRepo.findByIdAndPatientId(invoiceId, patientId)
                .orElse(null);
        
        if (invoice == null) {
            return null;
        }

        BigDecimal totalCharge = invoice.getTotalCharge() != null ? invoice.getTotalCharge() : BigDecimal.ZERO;
        BigDecimal ptBalance = invoice.getPtBalance() != null ? invoice.getPtBalance() : BigDecimal.ZERO;
        BigDecimal insBalance = invoice.getInsBalance() != null ? invoice.getInsBalance() : BigDecimal.ZERO;
        
        // Calculate total outstanding
        BigDecimal totalOutstanding = ptBalance.add(insBalance);
        
        // If there's a negative balance (overpayment), transfer to credit
        if (totalOutstanding.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal overpayment = totalOutstanding.abs();
            CreditTransferDto result = processAutomaticCreditTransfer(patientId, invoiceId, overpayment);
            
            // Reset balances to zero
            invoice.setPtBalance(BigDecimal.ZERO);
            invoice.setInsBalance(BigDecimal.ZERO);
            invoice.setStatus(PatientInvoice.Status.PAID);
            invoiceRepo.save(invoice);
            
            return result;
        }
        
        return null;
    }
}