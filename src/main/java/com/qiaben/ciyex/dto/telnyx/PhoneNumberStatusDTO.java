package com.qiaben.ciyex.dto.telnyx;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhoneNumberStatusDTO {
    private String taskId;
    private String phoneNumber;
    private String status; // phone-number assignment status
}
