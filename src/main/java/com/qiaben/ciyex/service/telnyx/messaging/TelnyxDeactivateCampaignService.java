package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxDeactivateCampaignResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxDeactivateCampaignService {

    private final TelnyxProperties properties;
    private final RestClient restClient = RestClient.builder().build();

    public TelnyxDeactivateCampaignResponseDTO deactivateCampaign(String campaignId) {
        String url = properties.getApiBaseUrl() + "/v2/10dlc/campaign/" + campaignId;

        return restClient.delete()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(TelnyxDeactivateCampaignResponseDTO.class);
    }
}
