package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxFqdnListResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxFqdnRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxFqdnResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxFqdnService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    private String url(String path) {
        return telnyxProperties.getApiBaseUrl() + path;
    }

    private RestClient.RequestHeadersSpec<?> auth(RestClient.RequestHeadersSpec<?> spec) {
        return spec.header("Authorization", "Bearer " + telnyxProperties.getApiKey());
    }

    public TelnyxFqdnListResponseDto list(Integer page, Integer size,
                                          String connectionId, String fqdn,
                                          Integer port, String dnsRecordType) {

        UriComponentsBuilder uri = UriComponentsBuilder
                .fromHttpUrl(url("/v2/fqdns"))
                .queryParam("page[number]", page)
                .queryParam("page[size]", size)
                .queryParamIfPresent("filter[connection_id]",   java.util.Optional.ofNullable(connectionId))
                .queryParamIfPresent("filter[fqdn]",            java.util.Optional.ofNullable(fqdn))
                .queryParamIfPresent("filter[port]",            java.util.Optional.ofNullable(port))
                .queryParamIfPresent("filter[dns_record_type]", java.util.Optional.ofNullable(dnsRecordType));

        return auth(restClient.get().uri(uri.build(true).toUri()))
                .retrieve()
                .body(TelnyxFqdnListResponseDto.class);
    }

    public TelnyxFqdnResponseDto create(TelnyxFqdnRequestDto req) {
        return auth(restClient.post().uri(url("/v2/fqdns")).body(req))
                .retrieve()
                .body(TelnyxFqdnResponseDto.class);
    }

    public TelnyxFqdnResponseDto getById(String id) {
        return auth(restClient.get().uri(url("/v2/fqdns/{id}"), id))
                .retrieve()
                .body(TelnyxFqdnResponseDto.class);
    }

    public TelnyxFqdnResponseDto update(String id, TelnyxFqdnRequestDto req) {
        return auth(restClient.patch().uri(url("/v2/fqdns/{id}"), id).body(req))
                .retrieve()
                .body(TelnyxFqdnResponseDto.class);
    }

    public TelnyxFqdnResponseDto delete(String id) {
        return auth(restClient.delete().uri(url("/v2/fqdns/{id}"), id))
                .retrieve()
                .body(TelnyxFqdnResponseDto.class);
    }
}
