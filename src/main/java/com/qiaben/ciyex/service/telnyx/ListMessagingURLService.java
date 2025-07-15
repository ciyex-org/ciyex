package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ListMessagingURLDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ListMessagingURLService {

    private final TelnyxProperties telnyxProperties;
    private final RestTemplate restTemplate;

    @Autowired
    public ListMessagingURLService(TelnyxProperties telnyxProperties, RestTemplate restTemplate) {
        this.telnyxProperties = telnyxProperties;
        this.restTemplate = restTemplate;
    }

    public ListMessagingURLDto listMessagingUrlDomains(Integer page, Integer size) {
        String url = UriComponentsBuilder
                .fromHttpUrl(telnyxProperties.getApiBaseUrl() + "/v2/messaging_url_domains")
                .queryParam("page[number]", page != null ? page : 1)
                .queryParam("page[size]", size != null ? size : 20)
                .toUriString();

        return restTemplate.getForObject(url, ListMessagingURLDto.class);
    }
}
