package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;

@Data
public class TelnyxUpdateCampaignRequestDTO {
    private String resellerId;
    private String sample1;
    private String sample2;
    private String sample3;
    private String sample4;
    private String sample5;
    private String messageFlow;
    private String helpMessage;
    private Boolean autoRenewal = true;
    private String webhookURL;
    private String webhookFailoverURL;
}
