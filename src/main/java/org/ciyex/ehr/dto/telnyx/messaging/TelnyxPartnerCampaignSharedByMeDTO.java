package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;

@Data
public class TelnyxPartnerCampaignSharedByMeDTO {
    private String brandId;
    private String campaignId;
    private String createDate;
    private String status;
    private String usecase;
}
