package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class FqdnService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    private String url(String path) {
        return telnyxProperties.getApiBaseUrl() + path;
    }

    private RestClient.RequestHeadersSpec<?> auth(RestClient.RequestHeadersSpec<?> spec) {
        return spec.header("Authorization", "Bearer " + telnyxProperties.getApiKey());
    }

    public FqdnListResponseDto list(Integer page, Integer size,
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
                .body(FqdnListResponseDto.class);
    }

    public FqdnResponseDto create(FqdnRequestDto req) {
        return auth(restClient.post().uri(url("/v2/fqdns")).body(req))
                .retrieve()
                .body(FqdnResponseDto.class);
    }

    public FqdnResponseDto getById(String id) {
        return auth(restClient.get().uri(url("/v2/fqdns/{id}"), id))
                .retrieve()
                .body(FqdnResponseDto.class);
    }

    public FqdnResponseDto update(String id, FqdnRequestDto req) {
        return auth(restClient.patch().uri(url("/v2/fqdns/{id}"), id).body(req))
                .retrieve()
                .body(FqdnResponseDto.class);
    }

    public FqdnResponseDto delete(String id) {
        return auth(restClient.delete().uri(url("/v2/fqdns/{id}"), id))
                .retrieve()
                .body(FqdnResponseDto.class);
    }
}
