package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TelnyxMessagingOptOutDTO {
    private String from;
    private String to;
    private String messagingProfileId;
    private String keyword;
    private OffsetDateTime createdAt;
}
