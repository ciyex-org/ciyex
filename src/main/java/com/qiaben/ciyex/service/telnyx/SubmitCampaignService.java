package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.SubmitCampaignRequestDTO;
import com.qiaben.ciyex.dto.telnyx.SubmitCampaignResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class SubmitCampaignService {

    private final TelnyxProperties properties;
    private final RestClient restClient = RestClient.builder().build();

    public SubmitCampaignResponseDTO submitCampaign(SubmitCampaignRequestDTO request) {
        String url = properties.getApiBaseUrl() + "/v2/10dlc/campaignBuilder";

        return restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(SubmitCampaignResponseDTO.class);
    }
}
