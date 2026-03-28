package org.ciyex.ehr.kiosk.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KioskCheckinDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long appointmentId;
    private String checkInTime;
    private Boolean demographicsUpdated;
    private Boolean insuranceUpdated;
    private Boolean consentSigned;
    private Boolean copayCollected;
    private BigDecimal copayAmount;
    private String verificationMethod;
    private String createdAt;
}
