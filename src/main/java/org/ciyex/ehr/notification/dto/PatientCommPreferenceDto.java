package org.ciyex.ehr.notification.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PatientCommPreferenceDto {
    private Long id;
    private Long patientId;
    private String email;
    private String phone;
    private Boolean emailOptIn;
    private Boolean smsOptIn;
    private String preferredChannel;
    private String language;
    private String quietHoursStart;
    private String quietHoursEnd;
    private String createdAt;
    private String updatedAt;
}
