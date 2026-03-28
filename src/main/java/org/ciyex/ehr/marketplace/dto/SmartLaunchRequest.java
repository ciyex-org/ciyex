package org.ciyex.ehr.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartLaunchRequest {
    private String patientId;
    private String encounterId;
    private String smartLaunchUrl;
    private String intent;
}
