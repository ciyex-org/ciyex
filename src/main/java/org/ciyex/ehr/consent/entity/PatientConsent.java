package org.ciyex.ehr.consent.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "patient_consent")
@Builder @NoArgsConstructor @AllArgsConstructor
public class PatientConsent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String patientName;
    private String consentType;     // hipaa_privacy, treatment, release_of_info, telehealth, research, financial
    private String status;          // pending, signed, expired, revoked
    private LocalDate signedDate;
    private LocalDate expiryDate;
    private String signedBy;        // patient or guardian name
    private String witnessName;
    private String documentUrl;
    private String version;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
