package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxListSharedCampaignsResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxListSharedCampaignsService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxListSharedCampaignsResponseDTO listSharedCampaigns(int page, int recordsPerPage, String sort) {
        String url = UriComponentsBuilder.fromHttpUrl(telnyxProperties.getApiBaseUrl() + "/10dlc/partner_campaigns")
                .queryParam("page", page)
                .queryParam("recordsPerPage", recordsPerPage)
                .queryParam("sort", sort)
                .toUriString();

        RestClient restClient = RestClient.builder().build();

        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxListSharedCampaignsResponseDTO.class);
    }
}
