package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity
public class PatientEducationAssignment extends AuditableEntity {

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

    @Builder.Default
    private boolean delivered = true;

    private LocalDateTime assignedDate;

    // Delegating accessor for backward compatibility
    public LocalDateTime getAssignedDate() {
        return assignedDate != null ? assignedDate : getCreatedDate();
    }
}
