// src/main/java/com/qiaben/ciyex/dto/telnyx/RcsTestNumberInviteDTO.java
package com.qiaben.ciyex.dto.telnyx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RcsTestNumberInviteDTO {
    private String recordType;   // "rcs.test_number_invite"
    private String agentId;
    private String phoneNumber;
    private String status;       // e.g. "invited"
}
