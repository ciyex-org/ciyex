package org.ciyex.ehr.kiosk.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "kiosk_checkin")
@Builder @NoArgsConstructor @AllArgsConstructor
public class KioskCheckin {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String patientName;
    private Long appointmentId;
    private LocalDateTime checkInTime;
    private Boolean demographicsUpdated;
    private Boolean insuranceUpdated;
    private Boolean consentSigned;
    private Boolean copayCollected;
    private BigDecimal copayAmount;
    private String verificationMethod; // dob, phone, both
    private String orgAlias;
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); if (checkInTime == null) checkInTime = LocalDateTime.now(); }
}
