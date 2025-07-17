package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSharedCampaignDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxUpdateSharedCampaignRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxUpdateSharedCampaignService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxSharedCampaignDTO updateCampaign(String campaignId, TelnyxUpdateSharedCampaignRequestDTO requestDTO) {
        String url = telnyxProperties.getApiBaseUrl() + "/10dlc/partner_campaigns/" + campaignId;

        RestClient restClient = RestClient.builder().build();

        return restClient.patch()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestDTO)
                .retrieve()
                .body(TelnyxSharedCampaignDTO.class);
    }
}
