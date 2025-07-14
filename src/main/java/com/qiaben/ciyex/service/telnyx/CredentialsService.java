package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.CredentialsDto;
import com.qiaben.ciyex.dto.telnyx.CredentialsListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class CredentialsService {

    private final TelnyxProperties props;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();
    }

    public CredentialsListResponseDto list(int page, int size, String tag, String name, String status, String resourceId, String sipUsername) {
        String url = UriComponentsBuilder
                .fromHttpUrl(props.getApiBaseUrl() + "/v2/credentials")
                .queryParam("page[number]", page)
                .queryParam("page[size]", size)
                .queryParam("filter[tag]", tag)
                .queryParam("filter[name]", name)
                .queryParam("filter[status]", status)
                .queryParam("filter[resource_id]", resourceId)
                .queryParam("filter[sip_username]", sipUsername)
                .toUriString();

        return RestClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build()
                .get()
                .retrieve()
                .body(CredentialsListResponseDto.class);
    }

    public CredentialsDto create(CredentialsDto dto) {
        return client().post()
                .uri("/v2/credentials")
                .body(dto)
                .retrieve()
                .body(CredentialsDto.class);
    }

    public CredentialsDto update(String id, CredentialsDto dto) {
        return client().put()
                .uri("/v2/credentials/{id}", id)
                .body(dto)
                .retrieve()
                .body(CredentialsDto.class);
    }

    public CredentialsDto getById(String id) {
        return client().get()
                .uri("/v2/credentials/{id}", id)
                .retrieve()
                .body(CredentialsDto.class);
    }
}
