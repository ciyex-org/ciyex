package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConnectionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxConnectionService {

    private final TelnyxProperties properties;

    public TelnyxConnectionResponseDto listConnections(
            Integer pageNumber,
            Integer pageSize,
            String connectionNameContains,
            Long outboundVoiceProfileId,
            String sort
    ) {
        RestClient restClient = RestClient.builder().baseUrl(properties.getApiBaseUrl()).build();

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromPath("/v2/connections")
                .queryParam("page[number]", pageNumber != null ? pageNumber : 1)
                .queryParam("page[size]", pageSize != null ? pageSize : 250);

        if (connectionNameContains != null && !connectionNameContains.isBlank()) {
            uriBuilder.queryParam("filter[connection_name][contains]", connectionNameContains);
        }

        if (outboundVoiceProfileId != null) {
            uriBuilder.queryParam("filter[outbound_voice_profile_id]", outboundVoiceProfileId);
        }

        if (sort != null && !sort.isBlank()) {
            uriBuilder.queryParam("sort", sort);
        }

        return restClient.get()
                .uri(uriBuilder.toUriString())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(TelnyxConnectionResponseDto.class);
    }

    public TelnyxConnectionResponseDto.ConnectionData getConnectionById(String id) {
        RestClient restClient = RestClient.builder().baseUrl(properties.getApiBaseUrl()).build();

        TelnyxConnectionResponseDto response = restClient.get()
                .uri("/v2/connections/{id}", id)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(TelnyxConnectionResponseDto.class);

        return response != null ? response.getData().get(0) : null;
    }
}
