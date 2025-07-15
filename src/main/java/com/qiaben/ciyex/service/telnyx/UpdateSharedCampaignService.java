package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.SharedCampaignDTO;
import com.qiaben.ciyex.dto.telnyx.UpdateSharedCampaignRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class UpdateSharedCampaignService {

    private final TelnyxProperties telnyxProperties;

    public SharedCampaignDTO updateCampaign(String campaignId, UpdateSharedCampaignRequestDTO requestDTO) {
        String url = telnyxProperties.getApiBaseUrl() + "/10dlc/partner_campaigns/" + campaignId;

        RestClient restClient = RestClient.builder().build();

        return restClient.patch()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestDTO)
                .retrieve()
                .body(SharedCampaignDTO.class);
    }
}
