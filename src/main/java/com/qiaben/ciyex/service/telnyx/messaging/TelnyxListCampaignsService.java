package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxListCampaignsResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxListCampaignsService {

    private final TelnyxProperties properties;
    private final RestClient restClient = RestClient.builder().build();

    public TelnyxListCampaignsResponseDTO listCampaigns(String brandId,
                                                        Integer page,
                                                        Integer recordsPerPage,
                                                        String sort) {

        String url = UriComponentsBuilder
                .fromHttpUrl(properties.getApiBaseUrl() + "/v2/10dlc/campaign")
                .queryParam("brandId", brandId)
                .queryParam("page", page != null ? page : 1)
                .queryParam("recordsPerPage", recordsPerPage != null ? recordsPerPage : 10)
                .queryParam("sort", sort != null ? sort : "-createdAt")
                .toUriString();

        return restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(TelnyxListCampaignsResponseDTO.class);
    }
}
