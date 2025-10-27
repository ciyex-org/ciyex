package com.qiaben.ciyex.entity;


import jakarta.persistence.*;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Entity
@Table(name = "patient_claims", indexes = {
        @Index(name = "idx_claim_inv", columnList = "invoiceId")
})
@EqualsAndHashCode(callSuper = true)
public class PatientClaim extends AuditableEntity {
    public enum Status {
        DRAFT, READY_FOR_SUBMISSION, SUBMITTED, IN_PROCESS, ACCEPTED, REJECTED, CLOSED, VOID
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public String getTreatingProviderId() {
        return treatingProviderId;
    }

    public void setTreatingProviderId(String treatingProviderId) {
        this.treatingProviderId = treatingProviderId;
    }

    public String getBillingEntity() {
        return billingEntity;
    }

    public void setBillingEntity(String billingEntity) {
        this.billingEntity = billingEntity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getAttachments() {
        return attachments;
    }

    public void setAttachments(int attachments) {
        this.attachments = attachments;
    }

    public boolean isEobAttached() {
        return eobAttached;
    }

    public void setEobAttached(boolean eobAttached) {
        this.eobAttached = eobAttached;
    }

    public LocalDate getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDate createdOn) {
        this.createdOn = createdOn;
    }

    public String getAccidentDate() {
        return accidentDate;
    }

    public void setAccidentDate(String accidentDate) {
        this.accidentDate = accidentDate;
    }

    public String getDiagnosticCode() {
        return diagnosticCode;
    }

    public void setDiagnosticCode(String diagnosticCode) {
        this.diagnosticCode = diagnosticCode;
    }

    public String getPredeterminationNumber() {
        return predeterminationNumber;
    }

    public void setPredeterminationNumber(String predeterminationNumber) {
        this.predeterminationNumber = predeterminationNumber;
    }

    public String getServiceLocationName() {
        return serviceLocationName;
    }

    public void setServiceLocationName(String serviceLocationName) {
        this.serviceLocationName = serviceLocationName;
    }

    public String getLabFacilityPrimaryId() {
        return labFacilityPrimaryId;
    }

    public void setLabFacilityPrimaryId(String labFacilityPrimaryId) {
        this.labFacilityPrimaryId = labFacilityPrimaryId;
    }

    public String getDocumentControlNumber() {
        return documentControlNumber;
    }

    public void setDocumentControlNumber(String documentControlNumber) {
        this.documentControlNumber = documentControlNumber;
    }

    public String getDelayReasonCode() {
        return delayReasonCode;
    }

    public void setDelayReasonCode(String delayReasonCode) {
        this.delayReasonCode = delayReasonCode;
    }

    public String getPriorPlacementDate() {
        return priorPlacementDate;
    }

    public void setPriorPlacementDate(String priorPlacementDate) {
        this.priorPlacementDate = priorPlacementDate;
    }

    public String getReferralDate() {
        return referralDate;
    }

    public void setReferralDate(String referralDate) {
        this.referralDate = referralDate;
    }

    public String getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(String admissionDate) {
        this.admissionDate = admissionDate;
    }

    public String getDischargeDate() {
        return dischargeDate;
    }

    public void setDischargeDate(String dischargeDate) {
        this.dischargeDate = dischargeDate;
    }

    public String getServiceAuthorizationExceptionCode() {
        return serviceAuthorizationExceptionCode;
    }

    public void setServiceAuthorizationExceptionCode(String serviceAuthorizationExceptionCode) {
        this.serviceAuthorizationExceptionCode = serviceAuthorizationExceptionCode;
    }

    public String getRemittanceDate() {
        return remittanceDate;
    }

    public void setRemittanceDate(String remittanceDate) {
        this.remittanceDate = remittanceDate;
    }

    public String getInsurancePaymentAmount() {
        return insurancePaymentAmount;
    }

    public void setInsurancePaymentAmount(String insurancePaymentAmount) {
        this.insurancePaymentAmount = insurancePaymentAmount;
    }

    public String getAttachmentIndicator() {
        return attachmentIndicator;
    }

    public void setAttachmentIndicator(String attachmentIndicator) {
        this.attachmentIndicator = attachmentIndicator;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getAttachmentTransmissionCode() {
        return attachmentTransmissionCode;
    }

    public void setAttachmentTransmissionCode(String attachmentTransmissionCode) {
        this.attachmentTransmissionCode = attachmentTransmissionCode;
    }

    public String getClaimSubmissionReasonCode() {
        return claimSubmissionReasonCode;
    }

    public void setClaimSubmissionReasonCode(String claimSubmissionReasonCode) {
        this.claimSubmissionReasonCode = claimSubmissionReasonCode;
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long orgId;
    @Column(nullable = false) private Long patientId;
    @Column(nullable = false) private Long invoiceId;

    private String payerName;
    private String treatingProviderId;
    private String billingEntity;
    private String type; // Electronic | Paper
    @Lob private String notes;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status = Status.DRAFT;

    private int attachments = 0;
    private boolean eobAttached = false;
    private LocalDate createdOn;

    // --- editable claim form fields (placeholders matching UI) ---
    private String accidentDate;
    private String diagnosticCode;
    private String predeterminationNumber;
    private String serviceLocationName;
    private String labFacilityPrimaryId;
    private String documentControlNumber;
    private String delayReasonCode;
    private String priorPlacementDate;
    private String referralDate;
    private String admissionDate;
    private String dischargeDate;
    private String serviceAuthorizationExceptionCode;
    private String remittanceDate;
    private String insurancePaymentAmount;
    private String attachmentIndicator;
    private String attachmentType;
    private String attachmentTransmissionCode;
    private String claimSubmissionReasonCode;

    // --- Attachment and EOB file storage (for demo: store as byte[]; in prod, use external storage and path/URL) ---
    @Lob
    private byte[] attachmentFile;
    @Lob
    private byte[] eobFile;

    public byte[] getAttachmentFile() { return attachmentFile; }
    public void setAttachmentFile(byte[] attachmentFile) { this.attachmentFile = attachmentFile; }
    public byte[] getEobFile() { return eobFile; }
    public void setEobFile(byte[] eobFile) { this.eobFile = eobFile; }
    // getters/setters
}

