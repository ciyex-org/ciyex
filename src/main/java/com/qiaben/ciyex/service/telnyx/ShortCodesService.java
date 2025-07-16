package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ShortCodesDto;
import com.qiaben.ciyex.dto.telnyx.SingleShortCodeDto;
import com.qiaben.ciyex.dto.telnyx.UpdateShortCodeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class ShortCodesService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient;

    // List short codes with optional filters
    public ShortCodesDto listShortCodes(Integer pageNumber, Integer pageSize, String messagingProfileId) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/short_codes";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (pageNumber != null) builder.queryParam("page[number]", pageNumber);
        if (pageSize != null) builder.queryParam("page[size]", pageSize);
        if (messagingProfileId != null) builder.queryParam("filter[messaging_profile_id]", messagingProfileId);

        return restClient
                .get()
                .uri(builder.toUriString())
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(ShortCodesDto.class);
    }

    // Retrieve single short code by ID
    public SingleShortCodeDto getShortCode(String id) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/short_codes/" + id;

        return restClient
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(SingleShortCodeDto.class);
    }


    public SingleShortCodeDto updateShortCode(String id, UpdateShortCodeRequest request) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/short_codes/" + id;

        return restClient
                .patch()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(SingleShortCodeDto.class);
    }
}
