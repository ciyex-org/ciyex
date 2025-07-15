package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.CampaignSharingStatusDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class GetSharingStatusService {

    private final TelnyxProperties telnyxProperties;

    public CampaignSharingStatusDTO getSharingStatus(String campaignId) {
        String url = telnyxProperties.getApiBaseUrl() + "/10dlc/partnerCampaign/" + campaignId + "/sharing";

        RestClient restClient = RestClient.builder().build();

        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(CampaignSharingStatusDTO.class);
    }
}
