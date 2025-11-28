package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Org reference


    // Visit details
    @Column(name = "visit_type", nullable = false)
    private String visitType;  // e.g., Consultation, Follow-up

    // Patient reference
    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    // Provider reference
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    // Dates & times
    @Column(name = "appointment_start_date")
    private String appointmentStartDate;

    @Column(name = "appointment_end_date")
    private String appointmentEndDate;

    @Column(name = "appointment_start_time")
    private String appointmentStartTime;

    @Column(name = "appointment_end_time")
    private String appointmentEndTime;

    // Meta
    private String priority;   // Routine, Urgent, etc.

    @Column(name = "location_id")
    private Long locationId;

    private String status;     // Scheduled, Completed, Cancelled, etc.

    @Column(length = 2000)
    private String reason;

    @Column(name = "fhir_id")
    private String fhirId;

    @Column(name = "external_id")
    private String externalId;

    // Telehealth
    // @Column(name = "meeting_url")
    // private String meetingUrl;

    // audit fields are provided by AuditableEntity
}
