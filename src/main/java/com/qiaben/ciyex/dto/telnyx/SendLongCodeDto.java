package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class SendLongCodeDto {
    private String from;
    private String to;
    private String text;
    private String subject;
    private List<String> mediaUrls;
    private String webhookUrl;
    private String webhookFailoverUrl;
    private Boolean useProfileWebhooks;
    private String type;
    private Boolean autoDetect;
}
