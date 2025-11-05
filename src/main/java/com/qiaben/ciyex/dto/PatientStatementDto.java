package com.qiaben.ciyex.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PatientStatementDto {
    public static class StatementLine {
        public String date;
        public String description;
        public String provider;
        public BigDecimal amount;
        public BigDecimal credit;
        public BigDecimal balance;
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

    public static class AppointmentSummary {
        public String nextTreatment;
        public String nextHygiene;
    }

    public Long patientId;
    public String patientName;
    public String statementDate;
    public String address;
    public String phone;
    public String email;
    public List<StatementLine> lines;
    public Summary summary;
    public YourPortion yourPortion;
    public AppointmentSummary appointments;
    public List<String> notes;
}
