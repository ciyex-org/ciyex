package org.ciyex.ehr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PatientStatementDto {
    
    public static class PracticeInfo {
        public String practiceName;
        public String address;
        public String phone;
        public String email;
        public String website;
    }
    
    public static class StatementLine {
        public String date;
        public String description;
        public String provider;
        public BigDecimal amount;
        public BigDecimal credit;
        public BigDecimal balance;
        public String transactionType; // INVOICE, CLAIM, INSURANCE_PAYMENT, PATIENT_PAYMENT, ADJUSTMENT
    }

    public static class Summary {
        public BigDecimal totalCharges;
        public BigDecimal totalPatientPayments;
        public BigDecimal totalInsurancePayments;
        public BigDecimal totalAdjustment;
        public BigDecimal outstandingBalance;
    }

    public static class YourPortion {
        public BigDecimal balance0_30;
        public BigDecimal balance30_60;
        public BigDecimal balance60_90;
        public BigDecimal balance90plus;
        public BigDecimal accountCredit;
    }
    
    public static class InsuranceSummary {
        public BigDecimal totalInsurancePayments;
        public BigDecimal totalInsuranceAdjustments;
        public BigDecimal estimatedRemainingInsurance;
    }

    public static class AppointmentSummary {
        public String nextScheduledAppointment;
        public String nextScheduledHygieneAppointment;
    }

    public PracticeInfo practice;
    public Long patientId;
    public String patientName;
    public String patientAddress;
    public String patientPhone;
    public String patientEmail;
    public String statementDate;
    public List<StatementLine> lines;
    public Summary summary;
    public YourPortion yourPortion;
    public InsuranceSummary insurance;
    public AppointmentSummary appointments;
    public List<String> notes;
}
