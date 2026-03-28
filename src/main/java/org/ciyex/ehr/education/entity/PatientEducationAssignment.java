package org.ciyex.ehr.education.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "patient_education_assignment")
@Builder @NoArgsConstructor @AllArgsConstructor
public class PatientEducationAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String patientName;
    private Long materialId;
    private String assignedBy;
    private LocalDate assignedDate;
    private LocalDate dueDate;
    private String status;
    private LocalDateTime viewedAt;
    private LocalDateTime completedAt;
    private Long encounterId;
    private String notes;
    @Column(columnDefinition = "TEXT")
    private String patientFeedback;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
