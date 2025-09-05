package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Recall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orgId;
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

    private String createdDate;
    private String lastModifiedDate;

    private String externalId;
}
