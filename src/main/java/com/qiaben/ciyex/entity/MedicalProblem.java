package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "medical_problems")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalProblem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

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

    // audit fields provided by AuditableEntity
}
