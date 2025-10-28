package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.entity.NoteTargetType;
import com.qiaben.ciyex.entity.PatientBillingNote;

import java.time.OffsetDateTime;

public class PatientBillingNoteDto {
    public Long id;
    public Long patientId;
    public Long invoiceId;
    public NoteTargetType type;
    public Long targetId;
    public String text;
    public String createdBy;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;

    public static PatientBillingNoteDto from(PatientBillingNote e) {
        PatientBillingNoteDto d = new PatientBillingNoteDto();
        d.id = e.getId();
        d.patientId = e.getPatientId();
        d.invoiceId = e.getInvoiceId();
        d.type = e.getTargetType();
        d.targetId = e.getTargetId();
        d.text = e.getText();
        d.createdBy = e.getCreatedBy();
        return d;
    }
}