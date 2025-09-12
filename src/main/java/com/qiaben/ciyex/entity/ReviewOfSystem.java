//package com.qiaben.ciyex.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Entity
//@Table(
//        name = "review_of_systems",
//        indexes = @Index(name = "idx_ros_scope", columnList = "org_id, patient_id, encounter_id")
//)
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class ReviewOfSystem {
//
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "external_id")
//    private String externalId;
//
//    @Column(name = "org_id", nullable = false)
//    private Long orgId;
//
//    @Column(name = "patient_id", nullable = false)
//    private Long patientId;
//
//    @Column(name = "encounter_id", nullable = false)
//    private Long encounterId;
//
//    @Column(name = "system_name", length = 64, nullable = false)
//    private String systemName;     // e.g., "Cardiovascular"
//
//    @Column(name = "is_negative")
//    private Boolean isNegative;    // true = all negative
//
//    @Column(name = "notes", columnDefinition = "TEXT")
//    private String notes;          // free text
//
//    // Store the checked items as a simple list in a join table (no JSON)
//    @ElementCollection
//    @CollectionTable(
//            name = "review_of_system_details",
//            joinColumns = @JoinColumn(name = "ros_id", nullable = false)
//    )
//    @Column(name = "detail", length = 128, nullable = false)
//    @Builder.Default
//    private List<String> systemDetails = new ArrayList<>();
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review_of_systems")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewOfSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "system_name", nullable = false, length = 64)
    private String systemName;

    @Column(name = "is_negative")
    private Boolean isNegative;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    // details table: review_of_system_details (ros_id, detail)
    @ElementCollection
    @CollectionTable(
            name = "review_of_system_details",
            joinColumns = @JoinColumn(name = "ros_id")
    )
    @Column(name = "detail", length = 128)
    private List<String> systemDetails = new ArrayList<>();

    // --- eSign / Print ---
    @Column(name = "e_signed")
    private Boolean eSigned = Boolean.FALSE;

    @Column(name = "signed_at")
    private OffsetDateTime signedAt;

    @Column(name = "signed_by", length = 128)
    private String signedBy;

    @Column(name = "printed_at")
    private OffsetDateTime printedAt;

    // audit
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
