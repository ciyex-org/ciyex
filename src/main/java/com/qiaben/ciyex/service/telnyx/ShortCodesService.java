package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ShortCodesDto;
import com.qiaben.ciyex.dto.telnyx.SingleShortCodeDto;
import com.qiaben.ciyex.dto.telnyx.UpdateShortCodeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ShortCodesService {

    private final TelnyxProperties telnyxProperties;
    private final RestTemplate restTemplate;

    @Autowired
    public ShortCodesService(TelnyxProperties telnyxProperties, RestTemplate restTemplate) {
        this.telnyxProperties = telnyxProperties;
        this.restTemplate = restTemplate;
    }

    // List short codes with optional filters
    public ShortCodesDto listShortCodes(Integer pageNumber, Integer pageSize, String messagingProfileId) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/short_codes";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (pageNumber != null) builder.queryParam("page[number]", pageNumber);
        if (pageSize != null) builder.queryParam("page[size]", pageSize);
        if (messagingProfileId != null) builder.queryParam("filter[messaging_profile_id]", messagingProfileId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + telnyxProperties.getApiKey());
        headers.set("Accept", "application/json");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ShortCodesDto> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                ShortCodesDto.class
        );
        return response.getBody();
    }

    // Retrieve single short code by id
    public SingleShortCodeDto getShortCode(String id) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/short_codes/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + telnyxProperties.getApiKey());
        headers.set("Accept", "application/json");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<SingleShortCodeDto> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, SingleShortCodeDto.class
        );
        return response.getBody();
    }

    // Update short code
    public SingleShortCodeDto updateShortCode(String id, UpdateShortCodeRequest request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/short_codes/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + telnyxProperties.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");

        HttpEntity<UpdateShortCodeRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<SingleShortCodeDto> response = restTemplate.exchange(
                url, HttpMethod.PATCH, entity, SingleShortCodeDto.class
        );
        return response.getBody();
    }
}
