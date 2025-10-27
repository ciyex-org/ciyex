package com.qiaben.ciyex.entity;


import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "patient_billing_notes",
    indexes = {
        @Index(name = "idx_pbn_patient", columnList = "patientId"),
        @Index(name = "idx_pbn_invoice", columnList = "invoiceId"),
        @Index(name = "idx_pbn_type_target", columnList = "targetType,targetId")
    })
public class PatientBillingNote extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;          // required
    private Long invoiceId;          // nullable, but helpful for joins/filters

    @Enumerated(EnumType.STRING)
    private NoteTargetType targetType;

    private Long targetId;           // id of invoice/claim/remit/payment depending on type

    @Column(length = 4000)
    private String text;

    // audit fields provided by AuditableEntity (createdDate, lastModifiedDate, createdBy, etc.)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public NoteTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(NoteTargetType targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    // created/updated audit handled by AuditableEntity


}
