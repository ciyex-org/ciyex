package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "schedules")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "external_id")
    private String externalId;

    // One-time schedule fields
    @Column(name = "start_time")
    private String start; // ISO-8601 datetime

    @Column(name = "end_time")
    private String end; // ISO-8601 datetime

    @Column(name = "timezone")
    private String timezone; // IANA timezone

    // Metadata fields
    @Column(name = "service_category")
    private String serviceCategory;

    @Column(name = "service_type")
    private String serviceType;

    @Column(name = "specialty")
    private String specialty;

    @Column(name = "status")
    private String status; // active/inactive

    @Column(name = "comment", length = 1000)
    private String comment;

    // Recurrence fields
    @Column(name = "recurrence_frequency")
    private String recurrenceFrequency; // DAILY, WEEKLY, MONTHLY

    @Column(name = "recurrence_interval")
    private Integer recurrenceInterval;

    @Column(name = "recurrence_by_weekday")
    private String recurrenceByWeekday; // Comma-separated: MO,TU,WE

    @Column(name = "recurrence_start_date")
    private String recurrenceStartDate; // yyyy-MM-dd

    @Column(name = "recurrence_end_date")
    private String recurrenceEndDate; // yyyy-MM-dd

    @Column(name = "recurrence_start_time")
    private String recurrenceStartTime; // HH:mm

    @Column(name = "recurrence_end_time")
    private String recurrenceEndTime; // HH:mm

    @Column(name = "recurrence_max_occurrences")
    private Integer recurrenceMaxOccurrences;

    @Column(name = "recurrence_location_id")
    private String recurrenceLocationId;

    // Actor references stored as comma-separated list
    @Column(name = "actor_references", length = 1000)
    private String actorReferences; // e.g., "Practitioner/123,Location/456"

    // audit fields provided by AuditableEntity
}