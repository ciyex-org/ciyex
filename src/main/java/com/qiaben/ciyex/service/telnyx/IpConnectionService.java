package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.IpConnectionCreateDto;
import com.qiaben.ciyex.dto.telnyx.IpConnectionUpdateDto;
import com.qiaben.ciyex.dto.telnyx.IpConnectionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class IpConnectionService {

    private final TelnyxProperties properties;

    public IpConnectionResponseDto listIpConnections(
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
                .body(IpConnectionResponseDto.class);
    }

    public IpConnectionResponseDto.IpConnectionData getIpConnectionById(String id) {
        RestClient restClient = RestClient.builder().baseUrl(properties.getApiBaseUrl()).build();

        IpConnectionResponseDto response = restClient.get()
                .uri("/v2/ip_connections/{id}", id)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(IpConnectionResponseDto.class);

        return response != null ? response.getData().get(0) : null;
    }

    public IpConnectionResponseDto.IpConnectionData createIpConnection(IpConnectionCreateDto request) {
        RestClient restClient = RestClient.builder().baseUrl(properties.getApiBaseUrl()).build();

        IpConnectionResponseDto response = restClient.post()
                .uri("/v2/ip_connections")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(request)
                .retrieve()
                .body(IpConnectionResponseDto.class);

        return response != null ? response.getData().get(0) : null;
    }

    public IpConnectionResponseDto.IpConnectionData updateIpConnection(String id, IpConnectionUpdateDto request) {
        RestClient restClient = RestClient.builder().baseUrl(properties.getApiBaseUrl()).build();

        IpConnectionResponseDto response = restClient.patch()
                .uri("/v2/ip_connections/{id}", id)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(request)
                .retrieve()
                .body(IpConnectionResponseDto.class);

        return response != null ? response.getData().get(0) : null;
    }

    public IpConnectionResponseDto.IpConnectionData deleteIpConnection(String id) {
        RestClient restClient = RestClient.builder().baseUrl(properties.getApiBaseUrl()).build();

        IpConnectionResponseDto response = restClient.delete()
                .uri("/v2/ip_connections/{id}", id)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(IpConnectionResponseDto.class);

        return response != null ? response.getData().get(0) : null;
    }
}
