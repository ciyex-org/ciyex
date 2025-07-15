package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class CampaignSharingStatusDTO {
    private String downstreamCnpId;
    private String sharedDate;
    private String sharingStatus;
    private String statusDate;
    private String upstreamCnpId;
}
