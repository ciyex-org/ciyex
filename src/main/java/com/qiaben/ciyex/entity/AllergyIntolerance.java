package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "allergy_intolerances")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllergyIntolerance extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fhir_id")
    private String fhirId;

    @Column(name = "external_id")
    private String externalId;



    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "allergy_name")
    private String allergyName;

    @Column(name = "reaction")
    private String reaction;

    @Column(name = "severity")
    private String severity;

    @Column(name = "status")
    private String status;

    // NEW
    @Column(name = "start_date")
    private String startDate;

    @Column(name = "end_date")
    private String endDate;

    // NEW
    @Column(name = "comments")
    private String comments;

    // audit fields provided by AuditableEntity
}
