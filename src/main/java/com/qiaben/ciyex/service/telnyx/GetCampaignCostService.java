package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.GetCampaignCostResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class GetCampaignCostService {

    private final TelnyxProperties properties;
    private final RestClient restClient = RestClient.builder().build();

    public GetCampaignCostResponseDTO getCost(String usecase) {
        String url = UriComponentsBuilder
                .fromHttpUrl(properties.getApiBaseUrl() + "/v2/10dlc/campaign/usecase/cost")
                .queryParam("usecase", usecase)
                .toUriString();

        return restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GetCampaignCostResponseDTO.class);
    }
}
