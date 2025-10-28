package com.qiaben.ciyex.dto;

import java.math.BigDecimal;

public class ClaimStatusUpdateDto {
    private String status;
    private String remitDate;
    private BigDecimal paymentAmount;



    // Getters and setters

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemitDate() { return remitDate; }
    public void setRemitDate(String remitDate) { this.remitDate = remitDate; }

    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
}