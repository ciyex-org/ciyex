package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class ShortCodeMessageDto {
    private String from;              // Required
    private String to;                // Required
    private String text;              // Required for SMS
    private String subject;           // Optional (MMS)
    private List<String> mediaUrls;   // Optional (MMS)
    private String webhookUrl;
    private String webhookFailoverUrl;
    private Boolean useProfileWebhooks = true;
    private String type;              // "SMS" or "MMS"
    private Boolean autoDetect;
}
