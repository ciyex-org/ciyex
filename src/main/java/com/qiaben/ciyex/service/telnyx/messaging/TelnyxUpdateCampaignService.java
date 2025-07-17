package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxUpdateCampaignRequestDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxUpdateCampaignResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxUpdateCampaignService {

    private final TelnyxProperties properties;
    private final RestClient restClient = RestClient.builder().build();

    public TelnyxUpdateCampaignResponseDTO updateCampaign(String campaignId, TelnyxUpdateCampaignRequestDTO request) {
        String url = properties.getApiBaseUrl() + "/v2/10dlc/campaign/" + campaignId;

        return restClient.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelnyxUpdateCampaignResponseDTO.class);
    }
}
