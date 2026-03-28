package org.ciyex.ehr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for printable invoice display - Complete statement with transaction history
 */
public class PatientInvoicePrintDto {


    public static class PracticeInfo {
        public String practiceName;
        public String address;
        public String phone;
        public String email;
        public String website;
    }
    
    /**
     * Transaction line in the statement table
     */
    public static class TransactionLine {
        public LocalDate date;
        public String description; // "Invoice #11710: $145.00", "Claim #1711", "Insurance Pay #1205", etc.
        public String provider;
        public BigDecimal amount;
        public BigDecimal credit;
        public BigDecimal balance;
        public String transactionType; // INVOICE, CLAIM, INSURANCE_PAYMENT, PATIENT_PAYMENT, ADJUSTMENT, INVOICE_LINE
        
        // For invoice lines
        public String code; // D0230, D0220, D0140
        public String procedureDescription; // "Intraoral Periapical Xray"
    }
    
    /**
     * Claim information
     */
    public static class ClaimInfo {
        public Long claimId;
        public String claimNumber;
        public String insuranceName; // "HORIZON"
        public String localId; // "Local 1202"
        public String status;
    }
    
    /**
     * Insurance payment detail
     */
    public static class InsurancePaymentDetail {
        public Long paymentId;
        public LocalDate paymentDate;
        public String description; // "Insurance Pay #1205 (Check 528153989)"
        public String insuranceName;
        public BigDecimal amount;
        public BigDecimal credit;
        public List<InsurancePaymentLine> lines;
    }
    
    public static class InsurancePaymentLine {
        public String code; // D0230, D0220, D0140
        public BigDecimal amount;
    }
    
    /**
     * Patient payment detail
     */
    public static class PatientPaymentDetail {
        public Long paymentId;
        public LocalDate paymentDate;
        public String description; // "Patient Payment"
        public String paymentMethod; // "CASH", "CHECK", "CARD"
        public BigDecimal amount;
        public BigDecimal credit;
    }
    
    /**
     * Patient deposit detail
     */
    public static class PatientDepositDetail {
        public Long depositId;
        public LocalDate depositDate;
        public String description;
        public String paymentMethod;
        public BigDecimal amount;
    }
    
    /**
     * Courtesy credit detail
     */
    public static class CourtesyCreditDetail {
        public Long creditId;
        public LocalDate creditDate;
        public String adjustmentType;
        public String description;
        public BigDecimal amount;
    }
    
    /**
     * Financial summary section
     */
    public static class FinancialSummary {
        public BigDecimal totalCharges;
        public BigDecimal totalPatientPayments;
        public BigDecimal totalInsurancePayments;
        public BigDecimal totalAdjustment;
        public BigDecimal outstandingBalance;
        public BigDecimal estimatedRemainingInsurance;
        public BigDecimal estimatedRemainingInsuranceAdjustment;
    }
    
    /**
     * Aging buckets for Your Portion
     */
    public static class AgingSummary {
        public BigDecimal balance0_30;
        public BigDecimal balance30_60;
        public BigDecimal balance60_90;
        public BigDecimal balance90plus;
        public BigDecimal accountCredit;
    }
    
    /**
     * Appointment information
     */
    public static class AppointmentInfo {
        public String nextScheduledTreatment; // "No Scheduled Appointment" or "11/21/2025 10:00 AM with Divya Arora"
        public String nextScheduledHygiene;
    }

    // Practice information
    public PracticeInfo practice;
    
    // Patient information
    public Long patientId;
    public String patientName;
    public String patientPhone;
    public String patientEmail;
    public String patientAddress;
    
    // Invoice information
    public Long invoiceId;
    public LocalDateTime invoiceDate;
    public String invoiceNumber;
    public String status;
    
    // Transaction history table (main statement table)
    public List<TransactionLine> transactions;
    
    // Claims associated with this invoice
    public List<ClaimInfo> claims;
    
    // Insurance payments details
    public List<InsurancePaymentDetail> insurancePayments;
    
    // Patient payments details
    public List<PatientPaymentDetail> patientPayments;
    
    // Patient deposits details
    public List<PatientDepositDetail> patientDeposits;
    
    // Courtesy credits details
    public List<CourtesyCreditDetail> courtesyCredits;
    
    // Financial summary
    public FinancialSummary financialSummary;
    
    // Aging summary (Your Portion)
    public AgingSummary agingSummary;
    
    // Appointment information
    public AppointmentInfo appointments;
    
    // Statement notes
    public List<String> notes;
}
