package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class SendMessageDto {
    private String from;
    private String messagingProfileId;
    private String to;
    private String text;
    private String subject;
    private List<String> mediaUrls;
    private String webhookUrl;
    private String webhookFailoverUrl;
    private boolean useProfileWebhooks;
    private String type;
    private boolean autoDetect;
    private String sendAt;
}
