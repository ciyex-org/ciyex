package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qiaben.ciyex.entity.CommunicationStatus;
import lombok.Data;

import java.util.List;

@Data
public class CommunicationDto {
    private Long id;
    private String externalId;
    private String fhirId;

    private CommunicationStatus status;
    private String category;

    // ✅ Keep both sentDate and createdDate so frontend can use whichever is relevant
    private String sentDate;         // When the message was sent
    private String createdDate;      // When the record was created in system
    private String lastModifiedDate; // For updates/edits

    private String payload;
    private String subject;
    private String inResponseTo;

    private Long patientId;
    private Long providerId;

    // 🔒 Internal linking fields (hidden from API/UI)
    @JsonIgnore
    private String sender;

    @JsonIgnore
    private List<String> recipients;

    // ✅ Exposed enriched fields for API/UI
    private Long fromId;           // provider id
    private String fromName;       // provider full name
    private List<Long> toIds;      // patient ids
    private List<String> toNames;  // patient full names

    // ✅ Message type differentiation
    private String messageType;    // "provider_to_patient" or "patient_to_provider"
    private String fromType;       // "provider" or "patient" - matches frontend expectation

    // ✅ Read tracking fields
    private String readAt;         // When the message was read
    private String readBy;         // Who read it ("provider" or "patient")

    // ✅ Attachment support
    private String attachmentIds;  // Comma-separated message attachment IDs (deprecated - use message_attachments table instead)
    private List<MessageAttachmentDto> attachments; // Full attachment details from message_attachments table
}
