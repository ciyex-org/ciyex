package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "communications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Communication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CommunicationStatus status;

    @Column(name = "category")
    private String category;

    @Column(name = "sent_date")
    private String sentDate;

    @Column(name = "created_date")
    private String createdDate;

    @Column(name = "last_modified_date")
    private String lastModifiedDate;

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
}