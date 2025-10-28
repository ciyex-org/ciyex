//package com.qiaben.ciyex.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "provider_note")
//@Getter @Setter
//@NoArgsConstructor @AllArgsConstructor @Builder
//public class ProviderNote {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    
//
//
//    @Column(name = "patient_id", nullable = false)
//    private Long patientId;
//
//    @Column(name = "encounter_id", nullable = false)
//    private Long encounterId;
//
//    @Column(name = "note_title")
//    private String noteTitle;
//
//    @Column(name = "note_type_code")
//    private String noteTypeCode;
//
//    @Column(name = "note_status")
//    private String noteStatus;
//
//    @Column(name = "note_datetime")
//    private LocalDateTime noteDateTime;   // <-- timestamp in DB
//
//    @Column(name = "author_practitioner_id")
//    private Long authorPractitionerId;    // <-- bigint in DB
//
//    // long text columns (NO @Lob to avoid LO/auto-commit issues)
//    @Column(columnDefinition = "text")
//    private String subjective;
//
//    @Column(columnDefinition = "text")
//    private String objective;
//
//    @Column(columnDefinition = "text")
//    private String assessment;
//
//    @Column(columnDefinition = "text")
//    private String plan;
//
//    @Column(columnDefinition = "text")
//    private String narrative;
//
//    @Column(name = "external_id")
//    private String externalId;
//
//    @CreationTimestamp
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    @Column(name = "updated_at", nullable = false)
//    private LocalDateTime updatedAt;
//}

package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "provider_note")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class ProviderNote extends AuditableEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "note_title", length = 255)
    private String noteTitle;

    @Column(name = "note_type_code", length = 255)
    private String noteTypeCode;

    @Column(name = "note_status", length = 255)
    private String noteStatus;

    @Column(name = "note_datetime")
    private LocalDateTime noteDateTime;

    @Column(name = "author_practitioner_id")
    private Long authorPractitionerId;

    @Column(name = "subjective", columnDefinition = "text")
    private String subjective;

    @Column(name = "objective", columnDefinition = "text")
    private String objective;

    @Column(name = "assessment", columnDefinition = "text")
    private String assessment;

    @Column(name = "plan", columnDefinition = "text")
    private String plan;

    @Column(name = "narrative", columnDefinition = "text")
    private String narrative;

    @Column(name = "external_id", length = 255)
    private String externalId;

    // eSign / Print
    @Builder.Default
    @Column(name = "e_signed")
    private Boolean eSigned = Boolean.FALSE;

    @Column(name = "signed_at")
    private OffsetDateTime signedAt;

    @Column(name = "signed_by", length = 128)
    private String signedBy;

    @Column(name = "printed_at")
    private OffsetDateTime printedAt;

    // audit fields provided by AuditableEntity

    public LocalDateTime getCreatedAt() { return getCreatedDate(); }
    public void setCreatedAt(LocalDateTime createdAt) { setCreatedDate(createdAt); }
    public LocalDateTime getUpdatedAt() { return getLastModifiedDate(); }
    public void setUpdatedAt(LocalDateTime updatedAt) { setLastModifiedDate(updatedAt); }
}
