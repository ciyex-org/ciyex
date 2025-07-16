package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class ScheduleMessageDto {
    private String from;
    private String messagingProfileId;
    private String to;
    private String text;
    private String subject;
    private List<String> mediaUrls;
    private String webhookUrl;
    private String webhookFailoverUrl;
    private Boolean useProfileWebhooks;
    private String type; // SMS or MMS
    private Boolean autoDetect;
    private String sendAt; // ISO 8601
}
