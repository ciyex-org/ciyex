package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MessagingOptOutDTO {
    private String from;
    private String to;
    private String messagingProfileId;
    private String keyword;
    private OffsetDateTime createdAt;
}
