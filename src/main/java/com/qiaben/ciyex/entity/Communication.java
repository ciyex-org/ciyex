package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "communications")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Communication extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "fhir_id")
    private String fhirId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CommunicationStatus status;

    @Column(name = "category")
    private String category;

    @Column(name = "sent_date")
    private String sentDate;

    // audit fields provided by AuditableEntity

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "sender")
    private String sender;

    @Column(name = "recipients")
    private String recipients; // comma-separated values

    @Column(name = "subject")
    private String subject;

    @Column(name = "in_response_to")
    private String inResponseTo;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "provider_id")
    private Long providerId;

    @Column(name = "read_at")
    private String readAt;

    @Column(name = "read_by")
    private String readBy; // "provider" or "patient"

    @Column(name = "attachment_ids")
    private String attachmentIds; // comma-separated message attachment IDs (deprecated - use message_attachments table instead)

    @Column(name = "message_type")
    private String messageType; // "patient_to_provider" or "provider_to_patient"

    @Column(name = "from_type")
    private String fromType; // "patient" or "provider"

    @Column(name = "from_id")
    private Long fromId; // ID of the sender (patient ID or provider ID)

    @Column(name = "from_name")
    private String fromName; // Name of the sender
}