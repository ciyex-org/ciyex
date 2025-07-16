package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.GetOsrCampaignAttributesResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class GetOsrCampaignAttributesService {

    private final TelnyxProperties properties;
    private final RestClient restClient = RestClient.builder().build();

    public GetOsrCampaignAttributesResponseDTO getAttributes(String campaignId) {
        String url = properties.getApiBaseUrl() + "/v2/10dlc/campaign/" + campaignId + "/osr/attributes";

        return restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GetOsrCampaignAttributesResponseDTO.class);
    }
}
