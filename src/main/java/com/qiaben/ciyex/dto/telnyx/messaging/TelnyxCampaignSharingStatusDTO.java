package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;

@Data
public class TelnyxCampaignSharingStatusDTO {
    private String downstreamCnpId;
    private String sharedDate;
    private String sharingStatus;
    private String statusDate;
    private String upstreamCnpId;
}
