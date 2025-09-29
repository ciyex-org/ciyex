package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "medical_problems")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    // Requested fields
    private String title;

    private String outcome;

    @Column(name = "verification_status")
    private String verificationStatus;

    private String occurrence;

    @Column(length = 2000)
    private String note;

    // Audit
    @Column(name = "created_date")
    private String createdDate;

    @Column(name = "last_modified_date")
    private String lastModifiedDate;
}
