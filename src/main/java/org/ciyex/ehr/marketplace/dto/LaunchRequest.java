package org.ciyex.ehr.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaunchRequest {
    private UUID patientId;
    private UUID encounterId;
    private String launchType;
}
