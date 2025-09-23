package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class PatientEducationAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "education_id")
    private PatientEducation education;

    @Column(nullable = false)
    private Long patientId;

    private String patientName; //  new column

    private String notes;

    private boolean delivered = true;

    private LocalDateTime assignedDate = LocalDateTime.now();
}
