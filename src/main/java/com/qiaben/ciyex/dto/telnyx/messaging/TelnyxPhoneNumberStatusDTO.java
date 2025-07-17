package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TelnyxPhoneNumberStatusDTO {
    private String taskId;
    private String phoneNumber;
    private String status; // phone-number assignment status
}
