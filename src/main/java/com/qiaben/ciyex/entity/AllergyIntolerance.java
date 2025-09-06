// src/main/java/com/qiaben/ciyex/entity/AllergyIntolerance.java
package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "allergy_intolerances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllergyIntolerance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

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

    @Column(name = "created_date")
    private String createdDate;

    @Column(name = "last_modified_date")
    private String lastModifiedDate;
}
