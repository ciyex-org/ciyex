package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxAcceptSharedCampaignResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxAcceptSharedCampaignService {

    private final TelnyxProperties properties;

    private final RestClient restClient = RestClient.builder().build();

    public TelnyxAcceptSharedCampaignResponseDTO accept(String campaignId) {
        String url = properties.getApiBaseUrl() + "/v2/10dlc/campaign/acceptSharing/" + campaignId;

        return restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(TelnyxAcceptSharedCampaignResponseDTO.class);
    }
}
