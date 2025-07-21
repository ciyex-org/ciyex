package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TelnyxDeleteMessagingProfileDto {
    private String id;
    private String message;
}
