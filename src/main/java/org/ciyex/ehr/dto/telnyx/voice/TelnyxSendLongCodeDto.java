package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxSendLongCodeDto {
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
