package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EqualsAndHashCode(callSuper = true)
public class Recall extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;
    private Long providerId;

    private String patientName;
    private String dob;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phone;
    private String email;

    private String lastVisit;
    private String recallDate;
    private String recallReason;

    private boolean smsConsent;
    private boolean emailConsent;

    // audit fields provided by AuditableEntity

    private String externalId;
}
