package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxMMSMessageDto {
    private String from;
    private List<String> to; // Up to 8 phone numbers in +E.164 format
    private String text;
    private String subject;
    private List<String> mediaUrls;
    private String webhookUrl;
    private String webhookFailoverUrl;
    private Boolean useProfileWebhooks;
}
