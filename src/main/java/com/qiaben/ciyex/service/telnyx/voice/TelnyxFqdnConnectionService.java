package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxFqdnConnectionDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxFqdnConnectionListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxFqdnConnectionService {

    private final TelnyxProperties properties;

    public TelnyxFqdnConnectionListResponseDto list(String connectionId, String ipAddress, Integer port, int page, int size) {
        String url = properties.getApiBaseUrl() + "/v2/ips"
                + "?page[number]=" + page
                + "&page[size]=" + size;

        if (connectionId != null) url += "&filter[connection_id]=" + connectionId;
        if (ipAddress != null) url += "&filter[ip_address]=" + ipAddress;
        if (port != null) url += "&filter[port]=" + port;

        return RestClient.create()
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(TelnyxFqdnConnectionListResponseDto.class);
    }

    public TelnyxFqdnConnectionDto create(TelnyxFqdnConnectionDto dto) {
        return RestClient.create()
                .post()
                .uri(properties.getApiBaseUrl() + "/v2/ips")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(dto)
                .retrieve()
                .body(TelnyxFqdnConnectionDto.class);
    }

    public TelnyxFqdnConnectionDto update(String id, TelnyxFqdnConnectionDto dto) {
        return RestClient.create()
                .patch()
                .uri(properties.getApiBaseUrl() + "/v2/ips/" + id)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(dto)
                .retrieve()
                .body(TelnyxFqdnConnectionDto.class);
    }

    public TelnyxFqdnConnectionDto getById(String id) {
        return RestClient.create()
                .get()
                .uri(properties.getApiBaseUrl() + "/v2/ips/" + id)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(TelnyxFqdnConnectionDto.class);
    }
}

