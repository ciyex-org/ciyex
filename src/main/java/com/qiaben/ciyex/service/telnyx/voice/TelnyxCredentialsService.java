package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCredentialsDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCredentialsListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxCredentialsService {

    private final TelnyxProperties props;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();
    }

    public TelnyxCredentialsListResponseDto list(int page, int size, String tag, String name, String status, String resourceId, String sipUsername) {
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
                .body(TelnyxCredentialsListResponseDto.class);
    }

    public TelnyxCredentialsDto create(TelnyxCredentialsDto dto) {
        return client().post()
                .uri("/v2/credentials")
                .body(dto)
                .retrieve()
                .body(TelnyxCredentialsDto.class);
    }

    public TelnyxCredentialsDto update(String id, TelnyxCredentialsDto dto) {
        return client().put()
                .uri("/v2/credentials/{id}", id)
                .body(dto)
                .retrieve()
                .body(TelnyxCredentialsDto.class);
    }

    public TelnyxCredentialsDto getById(String id) {
        return client().get()
                .uri("/v2/credentials/{id}", id)
                .retrieve()
                .body(TelnyxCredentialsDto.class);
    }
}
