package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Org reference
    @Column(nullable = false)
    private Long orgId; // Foreign key to organizations table

    // Visit details
    @Column(nullable = false)
    private String visitType;  // e.g., Consultation, Follow-up

    // Patient reference
    @Column(nullable = false)
    private Long patientId; // Foreign key to patients table

    // Provider reference
    @Column(nullable = false)
    private Long providerId; // Foreign key to providers table

    // Dates & times
    private String appointmentStartDate;
    private String appointmentEndDate;
    private String appointmentStartTime;
    private String appointmentEndTime;

    // Meta
    private String priority;   // Routine, Urgent, etc.

    @Column(nullable = true)
    private Long locationId;   // Foreign key to locations table

    private String status;     // Scheduled, Completed, Cancelled, etc.

    @Column(length = 2000)
    private String reason;     // Reason / Chief Complaint

    // Audit
    private String createdDate;
    private String lastModifiedDate;
}
