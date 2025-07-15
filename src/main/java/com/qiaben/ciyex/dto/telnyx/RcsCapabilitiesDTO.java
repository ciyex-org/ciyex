// src/main/java/com/qiaben/ciyex/dto/telnyx/RcsCapabilitiesDTO.java
package com.qiaben.ciyex.dto.telnyx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RcsCapabilitiesDTO {
    private String recordType;   // "rcs.capabilities"
    private String phoneNumber;
    private String agentId;
    private String agentName;
    private List<String> features;   // e.g. ["FILE_TRANSFER", "LOCATION"]
}
