package com.qiaben.ciyex.service.telnyx.messaging;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxListMessagingURLDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxListMessagingURLService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    public TelnyxListMessagingURLDto listMessagingUrlDomains(Integer page, Integer size) {
        String url = UriComponentsBuilder
                .fromHttpUrl(telnyxProperties.getApiBaseUrl() + "/v2/messaging_url_domains")
                .queryParam("page[number]", page != null ? page : 1)
                .queryParam("page[size]", size != null ? size : 20)
                .toUriString();

        return restClient
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxListMessagingURLDto.class);
    }
}
