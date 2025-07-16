package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.AcceptSharedCampaignResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class AcceptSharedCampaignService {

    private final TelnyxProperties properties;

    private final RestClient restClient = RestClient.builder().build();

    public AcceptSharedCampaignResponseDTO accept(String campaignId) {
        String url = properties.getApiBaseUrl() + "/v2/10dlc/campaign/acceptSharing/" + campaignId;

        return restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(AcceptSharedCampaignResponseDTO.class);
    }
}
