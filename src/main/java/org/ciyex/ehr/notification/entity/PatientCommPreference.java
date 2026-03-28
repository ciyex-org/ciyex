package org.ciyex.ehr.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data @Entity @Table(name = "patient_comm_preference",
        uniqueConstraints = @UniqueConstraint(columnNames = {"org_alias", "patient_id"}))
@Builder @NoArgsConstructor @AllArgsConstructor
public class PatientCommPreference {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String email;
    private String phone;
    private Boolean emailOptIn;
    private Boolean smsOptIn;
    private String preferredChannel;
    private String language;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
