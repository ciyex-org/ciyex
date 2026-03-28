package org.ciyex.ehr.immunization.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "immunization_record")
@Builder @NoArgsConstructor @AllArgsConstructor
public class ImmunizationRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String patientName;
    private String vaccineName;
    private String cvxCode;
    private String lotNumber;
    private String manufacturer;
    private LocalDate administrationDate;
    private LocalDate expirationDate;
    private String site;            // left arm, right arm, left thigh, etc.
    private String route;           // intramuscular, subcutaneous, oral, intranasal
    private Integer doseNumber;
    private String doseSeries;      // e.g., "1 of 3", "booster"
    private String administeredBy;
    private String orderingProvider;
    private String status;          // completed, entered_in_error, not_done
    @Column(columnDefinition = "TEXT")
    private String refusalReason;
    @Column(columnDefinition = "TEXT")
    private String reaction;
    private LocalDate visDate;      // Vaccine Information Statement date given
    @Column(columnDefinition = "TEXT")
    private String notes;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
