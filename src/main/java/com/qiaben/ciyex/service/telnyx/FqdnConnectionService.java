package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.FqdnConnectionDto;
import com.qiaben.ciyex.dto.telnyx.FqdnConnectionListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class FqdnConnectionService {

    private final TelnyxProperties properties;

    public FqdnConnectionListResponseDto list(String connectionId, String ipAddress, Integer port, int page, int size) {
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
                .body(FqdnConnectionListResponseDto.class);
    }

    public FqdnConnectionDto create(FqdnConnectionDto dto) {
        return RestClient.create()
                .post()
                .uri(properties.getApiBaseUrl() + "/v2/ips")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(dto)
                .retrieve()
                .body(FqdnConnectionDto.class);
    }

    public FqdnConnectionDto update(String id, FqdnConnectionDto dto) {
        return RestClient.create()
                .patch()
                .uri(properties.getApiBaseUrl() + "/v2/ips/" + id)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(dto)
                .retrieve()
                .body(FqdnConnectionDto.class);
    }

    public FqdnConnectionDto getById(String id) {
        return RestClient.create()
                .get()
                .uri(properties.getApiBaseUrl() + "/v2/ips/" + id)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .retrieve()
                .body(FqdnConnectionDto.class);
    }
}

