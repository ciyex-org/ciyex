package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class UpdateSharedCampaignRequestDTO {
    private String webhookURL;
    private String webhookFailoverURL;
}
