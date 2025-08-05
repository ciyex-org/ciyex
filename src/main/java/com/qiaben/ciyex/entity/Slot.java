package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "org_id", nullable = false)
    private Long orgId; // Tenant identifier

    private Long providerId; // FK to providers table
    private Long locationId; // FK to locations table
    private String startTime; // ISO format, e.g., "2025-08-03T09:00:00"
    private String endTime; // ISO format, e.g., "2025-08-03T10:00:00"
    private String status; // e.g., "available", "booked"
    private String createdDate;
    private String lastModifiedDate;
}