package org.ciyex.ehr.dto.portal;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequest {
    private String visitType;
    private Long providerId;
    private Long locationId;
    private String date;
    private String time;
    private String reason;
    private String priority;
}