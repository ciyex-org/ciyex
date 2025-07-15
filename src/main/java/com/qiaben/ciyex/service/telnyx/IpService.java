package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.IpDto;
import com.qiaben.ciyex.dto.telnyx.IpListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class IpService {

    private final TelnyxProperties props;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();
    }

    public IpListResponseDto list(int page, int size, String connectionId, String ipAddress, Integer port) {
        String url = UriComponentsBuilder
                .fromHttpUrl(props.getApiBaseUrl() + "/v2/ips")
                .queryParam("page[number]", page)
                .queryParam("page[size]", size)
                .queryParam("filter[connection_id]", connectionId)
                .queryParam("filter[ip_address]", ipAddress)
                .queryParam("filter[port]", port)
                .toUriString();

        return RestClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build()
                .get()
                .retrieve()
                .body(IpListResponseDto.class);
    }

    public IpDto create(IpDto dto) {
        return client().post()
                .uri("/v2/ips")
                .body(dto)
                .retrieve()
                .body(IpDto.class);
    }

    public IpDto update(String id, IpDto dto) {
        return client().put()
                .uri("/v2/ips/{id}", id)
                .body(dto)
                .retrieve()
                .body(IpDto.class);
    }

    public IpDto getById(String id) {
        return client().get()
                .uri("/v2/ips/{id}", id)
                .retrieve()
                .body(IpDto.class);
    }
}
