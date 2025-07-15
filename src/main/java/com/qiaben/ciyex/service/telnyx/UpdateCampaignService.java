package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.UpdateCampaignRequestDTO;
import com.qiaben.ciyex.dto.telnyx.UpdateCampaignResponseDTO;
import com.qiaben.ciyex.dto.telnyx.UpdateCampaignResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class UpdateCampaignService {

    private final TelnyxProperties properties;
    private final RestClient restClient = RestClient.builder().build();

    public UpdateCampaignResponseDTO updateCampaign(String campaignId, UpdateCampaignRequestDTO request) {
        String url = properties.getApiBaseUrl() + "/v2/10dlc/campaign/" + campaignId;

        return restClient.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(UpdateCampaignResponseDTO.class);
    }
}
