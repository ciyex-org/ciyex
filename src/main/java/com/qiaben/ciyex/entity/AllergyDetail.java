// src/main/java/com/qiaben/ciyex/entity/AllergyDetail.java
package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "allergy_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllergyDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning side */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allergy_intolerance_id", nullable = false)
    @ToString.Exclude
    private AllergyIntolerance allergyIntolerance;

    @Column(name = "allergy_name")
    private String allergyName;

    @Column(name = "reaction")
    private String reaction;

    @Column(name = "severity")
    private String severity;

    @Column(name = "status")
    private String status;
}
