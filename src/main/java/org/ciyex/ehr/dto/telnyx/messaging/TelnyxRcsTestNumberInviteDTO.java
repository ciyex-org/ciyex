// src/main/java/com/ciyex/ciyex/dto/telnyx/RcsTestNumberInviteDTO.java
package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelnyxRcsTestNumberInviteDTO {
    private String recordType;   // "rcs.test_number_invite"
    private String agentId;
    private String phoneNumber;
    private String status;       // e.g. "invited"
}
