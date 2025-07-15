package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ListShortcodesMessagingProfileDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ListShortcodesMessagingProfileService {

    private final TelnyxProperties telnyxProperties;
    private final RestTemplate restTemplate;

    @Autowired
    public ListShortcodesMessagingProfileService(TelnyxProperties telnyxProperties, RestTemplate restTemplate) {
        this.telnyxProperties = telnyxProperties;
        this.restTemplate = restTemplate;
    }

    public ListShortcodesMessagingProfileDto getShortcodes(String id, int pageNumber, int pageSize) {
        String url = String.format("%s/v2/messaging_profiles/%s/short_codes", telnyxProperties.getApiBaseUrl(), id);

        // Building URL with query parameters (page[number], page[size])
        String finalUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("page[number]", pageNumber)
                .queryParam("page[size]", pageSize)
                .toUriString();

        return restTemplate.getForObject(finalUrl, ListShortcodesMessagingProfileDto.class);
    }
}
