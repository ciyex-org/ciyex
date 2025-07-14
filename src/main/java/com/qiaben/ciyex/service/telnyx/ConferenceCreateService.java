package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ConferenceCreateRequestDto;
import com.qiaben.ciyex.dto.telnyx.ConferenceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ConferenceCreateService {

    private final RestClient restClient;
    private final TelnyxProperties telnyxProperties;

    /**
     * Calls Telnyx POST /v2/conferences.
     */
    public ConferenceResponseDto createConference(ConferenceCreateRequestDto req) {

        return restClient
                .post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/conferences")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(ConferenceResponseDto.class);
    }
}

