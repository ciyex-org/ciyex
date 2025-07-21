package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSharedCampaignDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxGetSharedCampaignService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxSharedCampaignDTO getSharedCampaignById(String campaignId) {
        String url = telnyxProperties.getApiBaseUrl() + "/10dlc/partner_campaigns/" + campaignId;

        RestClient restClient = RestClient.builder().build();

        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxSharedCampaignDTO.class);
    }
}
