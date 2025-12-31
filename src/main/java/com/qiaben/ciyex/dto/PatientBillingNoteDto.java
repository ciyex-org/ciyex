package com.qiaben.ciyex.dto;

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
}