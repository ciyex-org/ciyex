// src/main/java/com/ciyex/ciyex/dto/telnyx/RcsCapabilitiesDTO.java
package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelnyxRcsCapabilitiesDTO {
    private String recordType;   // "rcs.capabilities"
    private String phoneNumber;
    private String agentId;
    private String agentName;
    private List<String> features;   // e.g. ["FILE_TRANSFER", "LOCATION"]
}
