package com.qiaben.ciyex.dto;

import lombok.Data;
import java.util.List;

@Data
public class InvoiceDto {
    private Long id;
    private String externalId;      // FHIR Invoice id (optional)
    private Long orgId;
    private Long patientId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(Long encounterId) {
        this.encounterId = encounterId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(String issueDate) {
        this.issueDate = issueDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getPayer() {
        return payer;
    }

    public void setPayer(String payer) {
        this.payer = payer;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getTotalGross() {
        return totalGross;
    }

    public void setTotalGross(String totalGross) {
        this.totalGross = totalGross;
    }

    public String getTotalNet() {
        return totalNet;
    }

    public void setTotalNet(String totalNet) {
        this.totalNet = totalNet;
    }

    public List<LineDto> getLines() {
        return lines;
    }

    public void setLines(List<LineDto> lines) {
        this.lines = lines;
    }

    public List<PaymentDto> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentDto> payments) {
        this.payments = payments;
    }

    public Audit getAudit() {
        return audit;
    }

    public void setAudit(Audit audit) {
        this.audit = audit;
    }

    private Long encounterId;

    private String invoiceNumber;
    private String status;          // draft|issued|balanced|cancelled|entered-in-error
    private String currency;        // USD
    private String issueDate;       // yyyy-MM-dd
    private String dueDate;         // yyyy-MM-dd
    private String payer;           // person/org to bill
    private String notes;

    private String totalGross;      // string for transport; BigDecimal in entity
    private String totalNet;

    private List<LineDto> lines;    // charge lines
    private List<PaymentDto> payments; // payments applied (optional)

    private Audit audit;
    @Data public static class Audit { private String createdDate; private String lastModifiedDate; }

    @Data public static class LineDto {
        private Long id;
        private String description;
        private Integer quantity;
        private String unitPrice;
        private String amount;     // quantity * price - adj
        private String code;       // optional code
    }
    @Data public static class PaymentDto {
        private Long id;
        private String date;       // yyyy-MM-dd
        private String amount;
        private String method;     // cash|card|eft|adjustment
        private String reference;  // txn id / check #
        private String note;
    }
}
