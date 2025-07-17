package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxIpConnectionCreateDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxIpConnectionUpdateDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxIpConnectionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxIpConnectionService {

    private final TelnyxProperties properties;

    public TelnyxIpConnectionResponseDto listIpConnections(
            Integer pageNumber,
            Integer pageSize,
            String connectionNameContains,
            Long outboundVoiceProfileId,
            String sort
    ) {
        RestClient restClient = RestClient.builder().baseUrl(properties.getApiBaseUrl()).build();

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromPath("/v2/ip_connections")
                .queryParam("page[number]", pageNumber != null ? pageNumber : 1)
                .queryParam("page[size]", pageSize != null ? pageSize : 250);

        if (connectionNameContains != null && !connectionNameContains.isBlank()) {
            uriBuilder.queryParam("filter[connection_name][contains]", connectionNameContains);
        }

        if (outboundVoiceProfileId != null) {
            uriBuilder.queryParam("filter[outbound.outbound_voice_profile_id]", outboundVoiceProfileId);
        }

        if (sort != null && !sort.isBlank()) {
            uriBuilder.queryParam("sort", sort);
        }

        return restClient.get()
                .uri(uriBuilder.toUriString())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(TelnyxIpConnectionResponseDto.class);
    }

    public TelnyxIpConnectionResponseDto.IpConnectionData getIpConnectionById(String id) {
        RestClient restClient = RestClient.builder().baseUrl(properties.getApiBaseUrl()).build();

        TelnyxIpConnectionResponseDto response = restClient.get()
                .uri("/v2/ip_connections/{id}", id)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(TelnyxIpConnectionResponseDto.class);

        return response != null ? response.getData().get(0) : null;
    }

    public TelnyxIpConnectionResponseDto.IpConnectionData createIpConnection(TelnyxIpConnectionCreateDto request) {
        RestClient restClient = RestClient.builder().baseUrl(properties.getApiBaseUrl()).build();

        TelnyxIpConnectionResponseDto response = restClient.post()
                .uri("/v2/ip_connections")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(request)
                .retrieve()
                .body(TelnyxIpConnectionResponseDto.class);

        return response != null ? response.getData().get(0) : null;
    }

    public TelnyxIpConnectionResponseDto.IpConnectionData updateIpConnection(String id, TelnyxIpConnectionUpdateDto request) {
        RestClient restClient = RestClient.builder().baseUrl(properties.getApiBaseUrl()).build();

        TelnyxIpConnectionResponseDto response = restClient.patch()
                .uri("/v2/ip_connections/{id}", id)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(request)
                .retrieve()
                .body(TelnyxIpConnectionResponseDto.class);

        return response != null ? response.getData().get(0) : null;
    }

    public TelnyxIpConnectionResponseDto.IpConnectionData deleteIpConnection(String id) {
        RestClient restClient = RestClient.builder().baseUrl(properties.getApiBaseUrl()).build();

        TelnyxIpConnectionResponseDto response = restClient.delete()
                .uri("/v2/ip_connections/{id}", id)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(TelnyxIpConnectionResponseDto.class);

        return response != null ? response.getData().get(0) : null;
    }
}
